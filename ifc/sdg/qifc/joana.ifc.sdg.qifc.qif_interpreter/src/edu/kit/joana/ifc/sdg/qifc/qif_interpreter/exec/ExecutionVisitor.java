package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.exec;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.ssa.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.MissingValueException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ExecutionVisitor implements SSAInstruction.IVisitor {

	private static final String OUTPUT_FUNCTION = "edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out.print(I)V";
	private PrintStream out;

	private final Method m;
	private BBlock block;
	private int prevBlockIdx;
	private int nextBlockIdx;

	private boolean containsOutOfScopeInstruction;
	private SSAInstruction outOfScopeInstruction;

	public int getReturnValue() {
		return returnValue;
	}

	private int returnValue = -1;

	public ExecutionVisitor(Method m) {
		this.m = m;
		this.out = System.out; // default
	}

	public ExecutionVisitor(Method m, PrintStream out) {
		this.m = m;
		this.out = out;
	}

	/**
	 * executes a single basic block and returns the index of tha basic block to be executed next. If the return value is -1, the program is terminated
	 *
	 * @param start        the basic block to execute
	 * @param prevBlockIdx the idx of the basic block that was previously executed (needed to  correctly evaluate phis). If it is the first block of a program to be executed, this value should be -1
	 * @return the index of the next block to be executed or -1 if the execution is finished
	 */
	public int executeBlock(BBlock start, int prevBlockIdx) throws OutOfScopeException {
		if (start.isExitBlock()) {
			return -1;
		}

		block = start;
		this.prevBlockIdx = prevBlockIdx;
		this.nextBlockIdx = -1;

		start.getWalaBasicBLock().iteratePhis().forEachRemaining(this::visitPhi);

		for (SSAInstruction i : start.instructions()) {
			i.visit(this);
		}

		if (containsOutOfScopeInstruction) {
			throw new OutOfScopeException(outOfScopeInstruction);
		}

		if (nextBlockIdx != -1) {
			// idx of the next block has already been set by a conditional / jump instruction. We can directly return it
			return nextBlockIdx;
		} else {
			// no control flow relevant instruction has been executed, so we simply continue w/ the next basic block.
			// we can be sure it exists because {@code} start is not the exit block
			List<ISSABasicBlock> normalSuccs = Util
					.asList(start.getCFG().getWalaCFG().getNormalSuccessors(start.getWalaBasicBLock()));
			assert (normalSuccs.size() == 1);
			return normalSuccs.get(0).getNumber();
		}
	}

	@Override public void visitGoto(SSAGotoInstruction instruction) {
		nextBlockIdx = block.getCFG().getMethod().getBlockStartingAt(instruction.getTarget()).idx();
	}

	@Override public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
		containsOutOfScopeInstruction = true;
	}

	@Override public void visitArrayStore(SSAArrayStoreInstruction instruction) {
		containsOutOfScopeInstruction = true;
	}

	@Override public void visitBinaryOp(SSABinaryOpInstruction instruction) {
		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value[] operands = getUses(instruction);
		Integer op1 = (Integer) operands[0].getVal();
		Integer op2 = (Integer) operands[1].getVal();

		IBinaryOpInstruction.Operator operator = (IBinaryOpInstruction.Operator) instruction.getOperator();

		assert (op1 != null);
		assert (op2 != null);

		Object def;
		switch (operator) {
		case ADD:
			def = op1 + op2;
			break;
		case SUB:
			def = op1 - op2;
			break;
		case MUL:
			def = op1 * op2;
			break;
		case DIV:
			def = op1 / op2;
			break;
		case REM:
			def = op1 % op2;
			break;
		case AND:
			def = op1 & op2;
			break;
		case OR:
			def = op1 | op2;
			break;
		case XOR:
			def = op1 ^ op2;
			break;
		default:
			throw new IllegalStateException("Unexpected value: " + operator);
		}
		setDefValue(instruction, Type.getResultType(operator, operands[0].getType(), operands[1].getType()), def);
	}

	@Override public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
		int use = (Integer) getUses(instruction)[0].getVal();
		if (!instruction.getOpcode().equals(IUnaryOpInstruction.Operator.NEG)) {
			throw new IllegalStateException("Unexpected value: " + instruction.getOpcode());
		} else {
			setDefValue(instruction, getUses(instruction)[0].getType(), -use);
		}
	}

	@Override public void visitConversion(SSAConversionInstruction instruction) {
		containsOutOfScopeInstruction = true;
	}

	@Override public void visitComparison(SSAComparisonInstruction instruction) {
		containsOutOfScopeInstruction = true;
	}

	@Override public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value[] operands = getUses(instruction);
		Integer op1 = (Integer) operands[0].getVal();
		Integer op2 = (Integer) operands[1].getVal();

		IConditionalBranchInstruction.Operator operator = (IConditionalBranchInstruction.Operator) instruction
				.getOperator();
		boolean result;

		switch (operator) {
		case EQ:
			result = op1.equals(op2);
			break;
		case NE:
			result = !op1.equals(op2);
			break;
		case LT:
			result = op1 < op2;
			break;
		case GE:
			result = op1 >= op2;
			break;
		case GT:
			result = op1 > op2;
			break;
		case LE:
			result = op1 <= op2;
			break;
		default:
			throw new IllegalStateException("Unexpected value: " + operator);
		}

		List<ISSABasicBlock> succs = Util
				.asList(block.getCFG().getWalaCFG().getNormalSuccessors(block.getWalaBasicBLock()));
		assert (succs.size() == 2);

		BBlock trueTargetBlock = block.getCFG().getMethod().getBlockStartingAt(instruction.getTarget());
		succs.removeIf(b -> b.getNumber() == trueTargetBlock.idx());
		assert (succs.size() == 1);

		nextBlockIdx = (result) ? trueTargetBlock.idx() : succs.get(0).getNumber();
	}

	@Override public void visitSwitch(SSASwitchInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitReturn(SSAReturnInstruction instruction) {
		this.returnValue = instruction.getResult();
	}

	@Override public void visitGet(SSAGetInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitPut(SSAPutInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitPhi(SSAPhiInstruction instruction) {
		Iterator<ISSABasicBlock> orderedPredsIter = block.getCFG().getWalaCFG().getPredNodes(block.getWalaBasicBLock());

		int i = 0;
		while (orderedPredsIter.hasNext()) {
			int blockNum = orderedPredsIter.next().getNumber();
			if (blockNum == prevBlockIdx) {
				break;
			}
			i++;
		}
		Object op = getUses(instruction)[i].getVal();
		setDefValue(instruction, getUses(instruction)[i].getType(), op);
	}

	@Override public void visitPi(SSAPiInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitInvoke(SSAInvokeInstruction instruction) {

		if (instruction.getCallSite().getDeclaredTarget().getSignature().equals(OUTPUT_FUNCTION)) {
			int leaked = instruction.getUse(0);
			edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value leakedVal = m.getValue(leaked);
			leakedVal.leak();
			out.println(getUses(instruction)[0].getVal());
			out.println("Leaked info: " + Arrays.toString(getUses(instruction)[0].getDeps()));
		}
	}

	@Override public void visitNew(SSANewInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitArrayLength(SSAArrayLengthInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitThrow(SSAThrowInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitMonitor(SSAMonitorInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitCheckCast(SSACheckCastInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitInstanceof(SSAInstanceofInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	private edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value[] getUses(SSAInstruction i) {
		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value[] vals = new edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value[i
				.getNumberOfUses()];
		for (int j = 0; j < i.getNumberOfUses(); j++) {
			vals[j] = m.getValue(i.getUse(j));
		}
		return vals;
	}

	private void setDefValue(SSAInstruction i, Type type, Object def) {
		if (!m.hasValnum(i.getDef())) {
			edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value defVal = Value.createByType(i.getDef(), type);
			m.addValue(i.getDef(), defVal);
		}
		try {
			m.setValue(i.getDef(), def);
		} catch (MissingValueException e) {
			e.printStackTrace();
		}
	}

}
