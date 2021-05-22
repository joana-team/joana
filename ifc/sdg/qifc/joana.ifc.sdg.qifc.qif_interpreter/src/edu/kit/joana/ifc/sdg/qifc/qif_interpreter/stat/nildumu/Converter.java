package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.ssa.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ConversionException;
import nildumu.Operator;
import nildumu.Parser;
import nildumu.typing.Type;
import nildumu.typing.Types;
import swp.lexer.Location;

import java.util.*;
import java.util.stream.Collectors;

/**
 * converts a qif program into a nildumu ast
 */

public class Converter {

	public static final Location DUMMY_LOCATION = new Location(-1, -1);
	private NildumuOptions options;

	public static String varName(int valNum) {
		return String.valueOf(valNum);
	}

	public Parser.MethodNode convertMethod(Method m) throws ConversionException {
		Map<Integer, Parser.ParameterNode> params = parseParameters(m);
		Map<Integer, Parser.ExpressionNode> consts = parseConstantValues(m);
		Parser.BlockNode body = convertMethodBody(m, params, consts);
		Type returnType = NildumuType.of(m.getReturnType());
		Parser.GlobalVariablesNode globals = new Parser.GlobalVariablesNode(DUMMY_LOCATION, new HashMap<>());
		return new Parser.MethodNode(DUMMY_LOCATION, m.getCFG().getName(), returnType,
				new Parser.ParametersNode(DUMMY_LOCATION,
						params.values().stream().map(params::get).map(n -> (Parser.ParameterNode) n)
								.collect(Collectors.toList())), body, globals);
	}

	private Map<Integer, Parser.ExpressionNode> parseConstantValues(Method m) {
		Map<Integer, Parser.ExpressionNode> consts = new HashMap<>();
		m.getProgramValues().entrySet().stream().filter(p -> p.getValue().isConstant())
				.forEach(p -> consts.put(p.getKey(), Parser.literal((int) m.getValue(p.getKey()).getVal())));
		return consts;
	}

	private Parser.BlockNode convertMethodBody(Method m, Map<Integer, Parser.ParameterNode> params,
			Map<Integer, Parser.ExpressionNode> consts) {
		ConversionVisitor cVis = new ConversionVisitor(m, params, consts);
		return cVis.convert();
	}

	private List<Parser.StatementNode> convertStatements(List<BBlock> toConvert, List<Parser.StatementNode> converted,
			ConversionVisitor cVis) {
		if (toConvert.isEmpty()) {
			return converted;
		}
		BBlock b = toConvert.remove(0);
		converted.addAll(cVis.visitBlock(b));

		if (b.isCondHeader()) {
			BBlock trueTarget = b.getCFG().getBlock(b.getTrueTarget());
			BBlock falseTarget = b.succs().stream().filter(bb -> bb.idx() != b.getTrueTarget()).findAny().get();
			List<BBlock> trueBranch = computeConditionalBranch(b, trueTarget, trueTarget,
					new ArrayList<>(Arrays.asList(trueTarget)));
			List<BBlock> falseBranch = computeConditionalBranch(b, falseTarget, falseTarget,
					new ArrayList<>(Arrays.asList(falseTarget)));
			List<Parser.StatementNode> trueStmts = convertStatements(trueBranch, new ArrayList<>(), cVis);
			List<Parser.StatementNode> falseStmts = convertStatements(falseBranch, new ArrayList<>(), cVis);
			toConvert.removeAll(trueBranch);
			toConvert.removeAll(falseBranch);

			Parser.IfStatementNode if_ = new Parser.IfStatementNode(DUMMY_LOCATION, cVis.blockToExpr(b.idx()),
					new Parser.BlockNode(DUMMY_LOCATION, trueStmts), new Parser.BlockNode(DUMMY_LOCATION, falseStmts));
			converted.add(if_);
		}

		if (b.isLoopHeader()) {
			// TODO get loop blocks in topo order
			// TODO remove loop header from list
			LoopBody loop = b.getCFG().getMethod().getLoops().stream().filter(l -> l.getHead().idx() == b.idx())
					.findFirst().get();
			List<Parser.StatementNode> inLoop = convertStatements(new ArrayList<>(loop.getBlocks()), new ArrayList<>(),
					cVis);
			toConvert.removeAll(loop.getBlocks());
		}

		return converted;
	}

	/**
	 * returns a list of blocks that belong to a branch in an if-statement
	 * <p>
	 * invariant: all nodes that belong to the branch are dominated by the first block in the branch. We recursively (DFS) add successors, until we find one that is not dominated by {@code firstBlock}
	 *
	 * @param head       block containing the if-condition
	 * @param firstBlock successor of head that belongs to the branch we want to compute
	 * @param branch     list of blocks that we have already identified as being part of the branch. {@code current} is already part of the list!
	 * @return list of all basic blocks that belong to the branch
	 */
	public List<BBlock> computeConditionalBranch(BBlock head, BBlock firstBlock, BBlock current, List<BBlock> branch) {
		for (BBlock succ : current.succs()) {
			if ((succ.isDummy() || head.getCFG().isDominatedBy(succ, firstBlock)) && !branch.contains(succ)) {
				branch.add(succ);
				branch = computeConditionalBranch(head, firstBlock, succ, branch);
			}
		}
		return branch;
	}

	public Map<Integer, Parser.ParameterNode> parseParameters(Method m) throws ConversionException {
		Map<Integer, Parser.ParameterNode> params = new HashMap<>();

		// start @ 1, bc 0 is 'this'-reference
		for (int i = 1; i < m.getParamNum(); i++) {
			Parser.ParameterNode p = new Parser.ParameterNode(DUMMY_LOCATION, NildumuType.of(m.getParamType(i)),
					varName(m.getIr().getParameter(i)));
			params.put(m.getIr().getParameter(i), p);
		}
		return params;
	}

	public static class NildumuType {

		public static Type of(edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type type) throws ConversionException {
			if (type == edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type.INTEGER)
				return new Types().INT;
			throw new ConversionException(type);
		}
	}

	public static class LexerTerminal {

		public static Parser.LexerTerminal of(IBinaryOpInstruction.Operator op) throws ConversionException {
			switch (op) {
			case ADD:
				return Parser.LexerTerminal.PLUS;
			case SUB:
				return Parser.LexerTerminal.MINUS;
			case MUL:
				return Parser.LexerTerminal.MULTIPLY;
			case DIV:
				return Parser.LexerTerminal.DIVIDE;
			case REM:
				return Parser.LexerTerminal.MODULO;
			case AND:
				return Parser.LexerTerminal.BAND;
			case OR:
				return Parser.LexerTerminal.BOR;
			case XOR:
				return Parser.LexerTerminal.XOR;
			default:
				throw new ConversionException(op);
			}
		}

		public static Parser.LexerTerminal of(IUnaryOpInstruction.Operator op) throws ConversionException {
			switch (op) {
			case NEG:
				return Parser.LexerTerminal.INVERT;
			default:
				throw new ConversionException(op);
			}
		}
	}

	public static class NildumuOperator {

		public static Operator of(IBinaryOpInstruction.Operator op) throws ConversionException {
			switch (op) {
			case ADD:
				return Operator.ADD;
			case MUL:
				return Operator.MULTIPLY;
			case DIV:
				return Operator.DIVIDE;
			case REM:
				return Operator.MODULO;
			case AND:
				return Operator.AND;
			case OR:
				return Operator.OR;
			case XOR:
				return Operator.XOR;
			default:
				throw new ConversionException(op);
			}
		}
	}

	public static class ConversionVisitor implements SSAInstruction.IVisitor {

		private final Method m;
		private final List<Parser.StatementNode> stmts;
		private final Map<Integer, Parser.ParameterNode> valToParam;
		private final Map<Integer, Parser.ExpressionNode> valToExpr;
		private final Map<Integer, Parser.ExpressionNode> blockToExpr;
		private BBlock currentBlock;

		public ConversionVisitor(Method m, Map<Integer, Parser.ParameterNode> parameterToNode,
				Map<Integer, Parser.ExpressionNode> consts) {
			this.m = m;
			this.stmts = new ArrayList<>();
			this.valToParam = parameterToNode;
			this.valToExpr = consts;
			this.blockToExpr = new HashMap<>();
		}

		public Parser.BlockNode convert() {
			for (BBlock b : m.getCFG().getBlocks()) {
				if (b.isDummy()) {
					continue;
				}
				this.currentBlock = b;
				this.currentBlock.instructions().forEach(i -> i.visit(this));
			}
			return new Parser.BlockNode(DUMMY_LOCATION, stmts);
		}

		@Override public void visitGoto(SSAGotoInstruction instruction) {
			// do nothing
			return;
		}

		@Override public void visitArrayLoad(SSAArrayLoadInstruction instruction) {

		}

		@Override public void visitArrayStore(SSAArrayStoreInstruction instruction) {

		}

		@Override public void visitBinaryOp(SSABinaryOpInstruction instruction) {
			IBinaryOpInstruction.Operator op = (IBinaryOpInstruction.Operator) instruction.getOperator();

			Parser.LexerTerminal terminal = null;
			try {
				terminal = LexerTerminal.of(op);
			} catch (ConversionException e) {
				e.printStackTrace();
			}

			Parser.BinaryOperatorNode binOp = new Parser.BinaryOperatorNode(expressionNode(instruction.getUse(0)),
					expressionNode(instruction.getUse(1)), terminal);
			this.valToExpr.put(instruction.getDef(), binOp);

			// TODO should we use the same Types() obj every time?
			// TODO compute result type from operation, instead of statically using INT
			this.stmts.add(new Parser.VariableDeclarationNode(DUMMY_LOCATION, varName(instruction.getDef()),
					new Types().INT, expressionNode(instruction.getDef())));
		}

		// TODO how is unary minus handled?
		@Override public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
			IUnaryOpInstruction.Operator op = (IUnaryOpInstruction.Operator) instruction.getOpcode();
			Parser.UnaryOperatorNode unOp = null;
			try {
				unOp = new Parser.UnaryOperatorNode(expressionNode(instruction.getUse(0)), LexerTerminal.of(op));
			} catch (ConversionException e) {
				e.printStackTrace();
			}
			this.valToExpr.put(instruction.getDef(), unOp);
			this.stmts.add(new Parser.VariableDeclarationNode(DUMMY_LOCATION, varName(instruction.getDef()),
					new Types().INT, expressionNode(instruction.getDef())));
		}

		@Override public void visitConversion(SSAConversionInstruction instruction) {

		}

		@Override public void visitComparison(SSAComparisonInstruction instruction) {

		}

		@Override public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {

		}

		@Override public void visitSwitch(SSASwitchInstruction instruction) {

		}

		@Override public void visitReturn(SSAReturnInstruction instruction) {

		}

		@Override public void visitGet(SSAGetInstruction instruction) {

		}

		@Override public void visitPut(SSAPutInstruction instruction) {

		}

		@Override public void visitInvoke(SSAInvokeInstruction instruction) {

		}

		@Override public void visitNew(SSANewInstruction instruction) {

		}

		@Override public void visitArrayLength(SSAArrayLengthInstruction instruction) {

		}

		@Override public void visitThrow(SSAThrowInstruction instruction) {

		}

		@Override public void visitMonitor(SSAMonitorInstruction instruction) {

		}

		@Override public void visitCheckCast(SSACheckCastInstruction instruction) {

		}

		@Override public void visitInstanceof(SSAInstanceofInstruction instruction) {

		}

		@Override public void visitPhi(SSAPhiInstruction instruction) {

		}

		@Override public void visitPi(SSAPiInstruction instruction) {

		}

		@Override public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {

		}

		@Override public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {

		}

		private Parser.ExpressionNode expressionNode(int valNum) {
			assert (this.valToParam.containsKey(valNum) || this.valToExpr.containsKey(valNum));
			if (this.valToParam.containsKey(valNum)) {
				return new Parser.ParameterAccessNode(DUMMY_LOCATION, valToParam.get(valNum).name);
			} else {
				return this.valToExpr.get(valNum);
			}
		}

		public List<Parser.StatementNode> visitBlock(BBlock b) {
			return new ArrayList<>();
		}

		public Parser.ExpressionNode blockToExpr(int idx) {
			return this.blockToExpr.get(idx);
		}
	}
}
