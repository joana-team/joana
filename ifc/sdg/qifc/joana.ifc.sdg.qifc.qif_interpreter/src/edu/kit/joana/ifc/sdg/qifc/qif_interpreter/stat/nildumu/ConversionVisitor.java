package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.ssa.*;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Array;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ConversionException;
import nildumu.Parser;
import swp.lexer.Location;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.kit.joana.ifc.sdg.qifc.qif_interpreter.exec.ExecutionVisitor.OUTPUT_FUNCTION;

public class ConversionVisitor extends SSAInstruction.Visitor {

	public final Method m;
	public final Converter conv;
	public List<Parser.StatementNode> stmts;
	public final Map<Integer, Parser.ParameterNode> valToParam;
	public final Map<Integer, Parser.ExpressionNode> blockToExpr;
	private Map<Integer, Parser.VariableDeclarationNode> varDecls;
	private final Map<Integer, Integer> arrayVarIdxCounter;
	public BasicBlock currentBlock;

	public ConversionVisitor(Converter conv, Method m, Map<Integer, Parser.ParameterNode> parameterToNode) {
		this.m = m;
		this.stmts = new ArrayList<>();
		this.valToParam = parameterToNode;
		this.blockToExpr = new HashMap<>();
		this.arrayVarIdxCounter = new HashMap<>();
		this.conv = conv;
	}

	@Override public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
		varDecls.put(instruction.getDef(), Converter.varDecl(Converter.varName(instruction.getDef(), m),
				m.getValue(instruction.getDef()).getType().nildumuType()));
		Parser.BracketedAccessOperatorNode arrayAccess = new Parser.BracketedAccessOperatorNode(
				access(instruction.getArrayRef(), instruction), access(instruction.getIndex(), instruction));
		stmts.add(Converter.assignment(Converter.varName(instruction.getDef(), m), arrayAccess));
	}

	@Override public void visitArrayStore(SSAArrayStoreInstruction instruction) {
		Parser.ArrayAssignmentNode arrayAssignmentNode = new Parser.ArrayAssignmentNode(location(instruction),
				Converter.varName(instruction.getArrayRef(), m), access(instruction.getIndex(), instruction),
				access(instruction.getValue(), instruction));
		stmts.add(arrayAssignmentNode);

		int arrayVarIdx = arrayVarIdxCounter.getOrDefault(instruction.getArrayRef(), 1);
		Converter.arrayVarIndices.put(instruction, arrayVarIdx);
		arrayVarIdxCounter.put(instruction.getArrayRef(), arrayVarIdx + 1);
	}

	@Override public void visitBinaryOp(SSABinaryOpInstruction instruction) {
		IBinaryOpInstruction.Operator op = (IBinaryOpInstruction.Operator) instruction.getOperator();

		Parser.LexerTerminal terminal = null;
		try {
			terminal = Converter.LexerTerminal.of(op);
		} catch (ConversionException e) {
			e.printStackTrace();
		}

		Parser.BinaryOperatorNode binOp = new Parser.BinaryOperatorNode(access(instruction.getUse(0), instruction),
				access(instruction.getUse(1), instruction), terminal);

		this.varDecls.put(instruction.getDef(), Converter.varDecl(Converter.varName(instruction.getDef(), m),
				m.getValue(instruction.getDef()).getType().nildumuType()));
		this.stmts.add(Converter.assignment(Converter.varName(instruction.getDef(), m), binOp));
	}

	// TODO how is unary minus handled?
	@Override public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
		IUnaryOpInstruction.Operator op = (IUnaryOpInstruction.Operator) instruction.getOpcode();
		Parser.UnaryOperatorNode unOp = null;
		try {
			unOp = new Parser.UnaryOperatorNode(access(instruction.getUse(0), instruction),
					Converter.LexerTerminal.of(op));
		} catch (ConversionException e) {
			e.printStackTrace();
		}
		this.varDecls.put(instruction.getDef(), Converter.varDecl(Converter.varName(instruction.getDef(), m),
				m.getValue(instruction.getDef()).getType().nildumuType()));
		this.stmts.add(Converter.assignment(Converter.varName(instruction.getDef(), m), unOp));
	}

	@Override public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
		try {
			Parser.BinaryOperatorNode expr = new Parser.BinaryOperatorNode(access(instruction.getUse(0), instruction),
					access(instruction.getUse(1), instruction),
					Converter.LexerTerminal.of((IConditionalBranchInstruction.Operator) instruction.getOperator()));
			this.blockToExpr.put(currentBlock.idx(), expr);
		} catch (ConversionException e) {
			e.printStackTrace();
		}
	}

	@Override public void visitReturn(SSAReturnInstruction instruction) {
		if (currentBlock.getCFG().getMethod().getProg().getEntryMethod().equals(currentBlock.getCFG().getMethod()))
			return;

		if (instruction.returnsVoid()) {
			this.stmts.add(new Parser.ReturnStatementNode(location(instruction)));
		} else {
			this.stmts.add(new Parser.ReturnStatementNode(location(instruction),
					access(instruction.getResult(), instruction)));
		}
	}

	@Override public void visitInvoke(SSAInvokeInstruction instruction) {
		if (instruction.getCallSite().getDeclaredTarget().getSignature().equals(OUTPUT_FUNCTION)) {
			int leaked = instruction.getUse(0);
			Parser.OutputVariableDeclarationNode node;
			node = new Parser.OutputVariableDeclarationNode(location(instruction),
					"o_" + Converter.varName(instruction.getUse(0), m),
					edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER.nildumuType(),
					access(leaked, instruction), "l");
			this.stmts.add(node);
		} else {
			assert (instruction.hasDef());
			Parser.ArgumentsNode args = Converter.arguments(IntStream.range(1, instruction.getNumberOfParameters())
					.mapToObj(i -> Converter.varName(instruction.getUse(i), m)).collect(Collectors.toList()));
			Parser.MethodInvocationNode invocation = new Parser.MethodInvocationNode(location(instruction),
					Converter.methodName(instruction.getDeclaredTarget()), args);
			this.varDecls.put(instruction.getDef(), Converter.varDecl(Converter.varName(instruction.getDef(), m),
					m.getValue(instruction.getDef()).getType().nildumuType()));
			this.stmts.add(Converter.assignment(Converter.varName(instruction.getDef(), m), invocation));
		}
	}

	@Override public void visitNew(SSANewInstruction instruction) {
		assert (instruction.getConcreteType().isArrayType());
		varDecls.put(instruction.getDef(), Converter.varDecl(Converter.varName(instruction.getDef(), m),
				m.getValue(instruction.getDef()).getType().nildumuType()));

		// initialize array w/ zeros
		int arrayLength = ((Array<? extends Value>) m.getValue(instruction.getDef())).length();
		Parser.ArrayLiteralNode initVal = new Parser.ArrayLiteralNode(location(instruction),
				Collections.nCopies(arrayLength, Parser.literal(0)));
		stmts.add(Converter.assignment(Converter.varName(instruction.getDef(), m), initVal));
		arrayVarIdxCounter.put(instruction.getDef(), 1);
	}

	@Override public void visitArrayLength(SSAArrayLengthInstruction instruction) {

	}

	@Override public void visitPhi(SSAPhiInstruction instruction) {
		this.varDecls.put(instruction.getDef(), Converter.varDecl(Converter.varName(instruction.getDef(), m),
				m.getValue(instruction.getDef()).getType().nildumuType()));

		if (!currentBlock.isLoopHeader() && currentBlock.preds().stream()
				.map(pred -> (pred.isDummy() ? pred.preds().get(0) : pred)).anyMatch(BasicBlock::isLoopHeader)) {
			// phi that results from a break in loop
			// since loops are converted into functions, we only need to assign the new value the value of the loop-function return variable

			// one of the uses comes from a loop header, the other one from somewhere inside the loop
			BasicBlock use0DefBlock = BasicBlock.getBBlockForInstruction(m.getDef(instruction.getUse(0)), m.getCFG());
			BasicBlock loopHead;
			if (use0DefBlock.isLoopHeader() && m.getCFG().isDominatedBy(currentBlock, use0DefBlock)) {
				loopHead = use0DefBlock;
			} else {
				loopHead = BasicBlock.getBBlockForInstruction(m.getDef(instruction.getUse(1)), m.getCFG());
			}

			int substitutedVar = loopHead.ownsValue(instruction.getUse(0)) ?
					instruction.getUse(0) :
					instruction.getUse(1);
			stmts.add(
					new Parser.VariableAssignmentNode(location(instruction), Converter.varName(instruction.getDef(), m),
							access(substitutedVar, instruction)));
		} else {
			// phi from if-stmt
			BasicBlock condBlock = m.getCFG().getImmDom(currentBlock);
			BasicBlock firstPred = currentBlock.preds().get(0);
			int firstArg = (m.getCFG().isDominatedBy(firstPred, m.getCFG().getBlock(condBlock.getTrueTarget()))) ?
					0 :
					1;
			int sndArg = 1 - firstArg;

			Parser.PhiNode phi = new Parser.PhiNode(location(instruction),
					Arrays.asList(Converter.varName(instruction.getUse(firstArg), m),
							Converter.varName(instruction.getUse(sndArg), m)));
			stmts.add(
					new Parser.VariableAssignmentNode(location(instruction), Converter.varName(instruction.getDef(), m),
							phi));
		}
	}

	public Pair<List<Parser.StatementNode>, Map<Integer, Parser.VariableDeclarationNode>> visitBlock(BasicBlock b) {
		this.currentBlock = b;
		this.stmts = new ArrayList<>();
		this.varDecls = new HashMap<>();
		b.instructions().forEach(i -> i.visit(this));
		return Pair.make(stmts, varDecls);
	}

	public Parser.ExpressionNode blockToExpr(int idx) {
		return this.blockToExpr.get(idx);
	}

	public Parser.PrimaryExpressionNode access(int valNum, SSAInstruction instruction) {
		if (this.valToParam.containsKey(valNum)) {
			return accessParam(valNum);
		} else if (m.isConstant(valNum)) {
			return Parser.literal((Integer) m.getValue(valNum).getVal());
		}
		return new Parser.VariableAccessNode(location(instruction), Converter.varName(valNum, m));
	}

	private Parser.ParameterAccessNode accessParam(int valNum) {
		return new Parser.ParameterAccessNode(Converter.DUMMY_LOCATION, valToParam.get(valNum).name);
	}

	private Location location(SSAInstruction i) {
		return new Location(currentBlock.idx(), i.iindex);
	}
}