package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import nildumu.Parser;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LoopConversionVisitor extends ConversionVisitor {

	LoopBody l;
	NildumuProgram.LoopMethod res;
	final Set<Integer> alreadyDefined;

	public LoopConversionVisitor(Converter conv, Method m, Map<Integer, Parser.ParameterNode> parameterToNode,
			LoopBody l, NildumuProgram.LoopMethod res) {
		super(conv, m, parameterToNode);
		this.l = l;
		this.res = res;
		this.alreadyDefined = new HashSet<>();
	}

	@Override public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
		super.visitConditionalBranch(instruction);

		if (l.getBreaks().contains(currentBlock)) {
			List<BasicBlock> bridge = l.breakToPostLoop(currentBlock);
			List<Parser.StatementNode> beforeBreak = conv
					.convertStatements(bridge, new ConversionVisitor(conv, m, this.valToParam));

			BasicBlock postLoopSuccessor = l.getPostLoopSuccessor(currentBlock);

			int[] returnVars = new int[res.returnVars.length];
			for (int i = 0; i < returnVars.length; i++) {
				Optional<SSAInstruction> possibleBreakReturn = instructionForDef(res.returnVars[i], l.getHead());
				Optional<SSAInstruction> possibleBreakPhi = phiInstructionForUse(res.returnVars[i], postLoopSuccessor);
				int finalI = i;
				returnVars[i] = possibleBreakPhi
						.map(ssaInstruction -> (ssaInstruction.getUse(0) == res.returnVars[finalI]) ?
								ssaInstruction.getUse(1) :
								ssaInstruction.getUse(0)).orElseGet(() -> res.returnVars[finalI]);
			}

			Parser.ReturnStatementNode returnStmt = new Parser.ReturnStatementNode(Converter.DUMMY_LOCATION,
					Arrays.stream(returnVars).mapToObj(var -> access(var, instruction)).collect(Collectors.toList()));
			beforeBreak.add(returnStmt);

			Parser.ExpressionNode breakIf = blockToExpr(currentBlock.idx());
			if (l.hasBlock(currentBlock.getTrueTarget())) {
				breakIf = new Parser.UnaryOperatorNode(breakIf, Parser.LexerTerminal.INVERT);
			}

			Parser.StatementNode if_ = new Parser.IfStatementNode(Converter.DUMMY_LOCATION, breakIf,
					new Parser.BlockNode(Converter.DUMMY_LOCATION, beforeBreak));

			this.stmts.add(if_);
		}
	}

	private Optional<SSAInstruction> instructionForDef(int def, BasicBlock b) {
		return b.instructions().stream().filter(i -> i instanceof SSAPhiInstruction && i.getDef() == def).findFirst();
	}

	private Optional<SSAInstruction> phiInstructionForUse(int use, BasicBlock b) {
		return b.instructions().stream().filter(i -> i instanceof SSAPhiInstruction)
				.filter(i -> IntStream.range(0, i.getNumberOfUses()).anyMatch(j -> i.getUse(j) == use)).findFirst();
	}
}