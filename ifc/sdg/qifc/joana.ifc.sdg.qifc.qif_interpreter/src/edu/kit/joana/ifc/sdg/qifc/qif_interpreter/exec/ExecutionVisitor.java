package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.exec;

import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.ssa.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.MissingValueException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ParameterException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class ExecutionVisitor implements SSAInstruction.IVisitor {

	private static final String OUTPUT_FUNCTION = "edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out.print(I)V";
	private PrintStream out;

	private final Method m;
	private final Interpreter interpreter;
	private BBlock block;
	private int prevBlockIdx;
	private int nextBlockIdx;

	private boolean containsOutOfScopeInstruction;
	private SSAInstruction outOfScopeInstruction;

	public int getReturnValue() {
		return returnValue;
	}

	private int returnValue = -1;

	public ExecutionVisitor(Method m, Interpreter i) {
		this.m = m;
		this.out = System.out; // default
		this.interpreter = i;
	}

	public ExecutionVisitor(Method m, PrintStream out, Interpreter i) {
		this.m = m;
		this.out = out;
		this.interpreter = i;
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

		start.getWalaBasicBlock().iteratePhis().forEachRemaining(this::visitPhi);

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
					.asList(start.getCFG().getWalaCFG().getNormalSuccessors(start.getWalaBasicBlock()));
			assert (normalSuccs.size() == 1);
			return normalSuccs.get(0).getNumber();
		}
	}

	@Override public void visitGoto(SSAGotoInstruction instruction) {
		nextBlockIdx = block.getCFG().getMethod().getBlockStartingAt(instruction.getTarget()).idx();
	}

	@Override public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
		Array<? extends Value> arr = (Array<? extends Value>) m.getValue(instruction.getArrayRef());
		Type resultType = arr.elementType();
		Object val = arr.access((Integer)m.getValue(instruction.getIndex()).getVal()).getVal();
		setDefValue(instruction, resultType, val);
	}

	@Override public void visitArrayStore(SSAArrayStoreInstruction instruction) {
		Value assigned = m.getValue(instruction.getValue());
		int idx = (int) m.getValue(instruction.getIndex()).getVal();
		((Array<? extends Value>) m.getValue(instruction.getArrayRef())).store(assigned.getVal(), idx,
				m.getRecursionDepth());
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
			def = IntegerArithmetic.add(op1, op2);
			break;
		case SUB:
			def = IntegerArithmetic.sub(op1, op2);
			break;
		case MUL:
			def = IntegerArithmetic.mult(op1, op2);
			break;
		case DIV:
			def = IntegerArithmetic.div(op1, op2);
			break;
		case REM:
			def = IntegerArithmetic.mod(op1, op2);
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
			result = IntegerArithmetic.sub(op1, op2) < 0;
			break;
		case GE:
			result = IntegerArithmetic.sub(op1, op2) >= 0;
			break;
		case GT:
			result = IntegerArithmetic.sub(op1, op2) > 0;
			break;
		case LE:
			result = IntegerArithmetic.sub(op1, op2) <= 0;
			break;
		default:
			throw new IllegalStateException("Unexpected value: " + operator);
		}

		List<ISSABasicBlock> succs = Util
				.asList(block.getCFG().getWalaCFG().getNormalSuccessors(block.getWalaBasicBlock()));
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
		Iterator<ISSABasicBlock> orderedPredsIter = block.getCFG().getWalaCFG()
				.getPredNodes(block.getWalaBasicBlock());

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
		} else {
			String bcString = instruction.getDeclaredTarget().getSignature();
			Method target = (m.getProg().hasMethod(bcString)) ?
					m.getProg().getMethod(bcString) :
					new Method(instruction.getDeclaredTarget(), m.getProg());

			List<String> args = new ArrayList<>();
			// first arg is this-reference -> skip
			IntStream.range(1, instruction.getNumberOfUses())
					.forEach(use -> args.add(String.valueOf(m.getValue(instruction.getUse(use)).getValAsString())));

			Object returnVal = null;
			Type returnType = null;

			try {
				Interpreter i = new Interpreter(m.getProg());
				i.executeMethod(target, args);
				if (!target.isVoid()) {
					returnVal = target.getValue(target.getReturnValue()).getVal();
					returnType = target.getValue(target.getReturnValue()).getType();

				}
				target.resetValues();
				if (returnVal != null) {

					if (returnType == Type.ARRAY) {
						Array<? extends Value> arr = (m.hasValue(instruction.getDef())) ? m.getArray(instruction.getDef()) : Array.newArray(target.getArray(target.getReturnValue()).elementType(), instruction.getDef(), false);
						arr.setVal((Object[]) returnVal, m.getRecursionDepth());
						m.addValue(instruction.getDef(), arr);
					} else {
						setDefValue(instruction, returnType, returnVal);
					}
				}
			} catch (OutOfScopeException | ParameterException | UnexpectedTypeException e) {
				e.printStackTrace();
			}
		}
	}

	@Override public void visitNew(SSANewInstruction instruction) {
		if (!m.hasValue(instruction.getDef())) {
			if (instruction.getConcreteType().isArrayType()) {
				try {
					Value res = Array.newArray(instruction, m, false);
					m.addValue(instruction.getDef(), res);
				} catch (UnexpectedTypeException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override public void visitArrayLength(SSAArrayLengthInstruction instruction) {
		setDefValue(instruction, Type.INTEGER, ((Array<? extends Value>) m.getValue(instruction.getArrayRef())).length());
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
		if (!m.hasValue(i.getDef())) {
			edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value defVal = Value.createPrimitiveByType(i.getDef(), type);
			m.addValue(i.getDef(), defVal);
		}
		try {
			m.setValue(i.getDef(), def);
		} catch (MissingValueException e) {
			e.printStackTrace();
		}
	}
}
