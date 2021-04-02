package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.ssa.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

public class SATVisitor implements SSAInstruction.IVisitor {
	private static final String OUTPUT_FUNCTION = "edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out.print(I)V";

	private final StaticAnalysis staticAnalysis;
	private boolean containsOutOfScopeInstruction;
	private SSAInstruction outOfScopeInstruction;
	private BBlock block;
	private Method m;

	public void visitBlock(Method m, BBlock b, int prevBlock) throws OutOfScopeException {
		this.block = b;
		this.m = m;

		for (SSAInstruction i : b.instructions()) {
			i.visit(this);
		}

		if (containsOutOfScopeInstruction) {
			throw new OutOfScopeException(outOfScopeInstruction);
		}
	}

	public SATVisitor(StaticAnalysis staticAnalysis) {
		this.staticAnalysis = staticAnalysis;
		this.containsOutOfScopeInstruction = false;
	}

	@Override public void visitGoto(SSAGotoInstruction instruction) {
		// do nothing
	}

	@Override public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;

	}

	@Override public void visitArrayStore(SSAArrayStoreInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitComparison(SSAComparisonInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
		int op1ValNum = instruction.getUse(0);
		int op2ValNum = instruction.getUse(1);

		if (m.getValue(op1ValNum).getDeps() == null) {
			// if there doesn't exist a value object for this valueNumber at this point, it has to be constant
			staticAnalysis.createConstant(op1ValNum);
		}
		Formula[] op1 = m.getDepsForValue(op1ValNum);

		if (m.getValue(op2ValNum).getDeps() == null) {
			staticAnalysis.createConstant(op2ValNum);
		}
		Formula[] op2 = m.getDepsForValue(op2ValNum);

		IConditionalBranchInstruction.Operator operator = (IConditionalBranchInstruction.Operator) instruction
				.getOperator();

		Formula defForm;
		Formula[] diff = LogicUtil.sub(op1, op2);

		switch (operator) {
		case EQ:
			defForm = LogicUtil.equalsZero(diff);
			break;
		case NE:
			defForm = LogicUtil.ff.not(LogicUtil.equalsZero(diff));
			break;
		case LT:
			defForm = LogicUtil.ff.equivalence(LogicUtil.ff.constant(true), diff[0]);
			break;
		case GE:
			defForm = LogicUtil.ff.equivalence(LogicUtil.ff.constant(false), diff[0]);
			break;
		case GT:
			defForm = LogicUtil.ff.and(LogicUtil.ff.not(LogicUtil.equalsZero(diff)),
					LogicUtil.ff.equivalence(LogicUtil.ff.constant(false), diff[0]));
			break;
		case LE:
			defForm = LogicUtil.ff
					.or(LogicUtil.equalsZero(diff), LogicUtil.ff.equivalence(LogicUtil.ff.constant(true), diff[0]));
			break;
		default:
			throw new IllegalStateException("Unexpected value: " + operator);
		}
		block.setCondExpr(defForm);
	}

	@Override public void visitSwitch(SSASwitchInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitReturn(SSAReturnInstruction instruction) {
		if (!instruction.returnsVoid()) {
			m.addPossibleReturn(instruction.getUse(0));
			m.setReturnValue(instruction.getUse(0));
		}
	}

	@Override public void visitGet(SSAGetInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitPut(SSAPutInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitInvoke(SSAInvokeInstruction instruction) {
		if (instruction.getCallSite().getDeclaredTarget().getSignature().equals(OUTPUT_FUNCTION)) {
			m.getValue(instruction.getUse(0)).leak();
		} else if (instruction.getNumberOfDefs() > 0) {
			SimpleInvocationHandler handler = new SimpleInvocationHandler(m, instruction);
			handler.analyze();
		}
	}

	private boolean isRecursiveCall(SSAInvokeInstruction instruction) {
		return m.identifier().equals(instruction.getDeclaredTarget().getSignature());
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

	/**
	 * Use fresh variables for the defined value, bc depending on the program, the values that this instruction depends on may not have been evaluated yet
	 * Remember to add equality constraints later on!
	 *
	 * @param instruction a phi instruction
	 */
	@Override public void visitPhi(SSAPhiInstruction instruction) {
		if (!block.isLoopHeader()) {
			handleCondPhi(instruction);
		} else {
			handleLoopPhi(instruction);
		}
	}

	private void handleLoopPhi(SSAPhiInstruction instruction) {
		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value defVal;

		// find the operator for which there already exists as value object
		int op = (m.hasValue(instruction.getUse(0))) ? instruction.getUse(0) : instruction.getUse(1);

		if (!m.hasValue(instruction.getDef())) {
			defVal = edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value
					.createByType(instruction.getDef(), m.getValue(op).getType());
			m.addValue(instruction.getDef(), defVal);
		} else {
			defVal = m.getValue(instruction.getDef());
		}
		assert defVal != null;
		Variable[] vars = LogicUtil.createVars(defVal.getValNum(), defVal.getType().bitwidth());
		m.setDepsForvalue(instruction.getDef(), vars);
		m.addVarsToValue(instruction.getDef(), vars);

	}

	private void handleCondPhi(SSAPhiInstruction instruction) {
		Formula[] op1 = m.getDepsForValue(instruction.getUse(0));
		Formula[] op2 = m.getDepsForValue(instruction.getUse(1));

		BBlock predZero = block.preds().get(0);

		BBlock condBlock = m.getCFG().getImmDom(block);
		assert (condBlock.isCondHeader());
		// trueTarget will be a "proper" basic block, however we have inserted dummy blocks between the conditional head and the target block.
		// So instead of the target block we access its only pred --> the dummy block
		BBlock trueTarget = m.getBlockStartingAt(
				((SSAConditionalBranchInstruction) condBlock.getWalaBasicBlock().getLastInstruction())
						.getTarget()).preds().get(0);

		assert (trueTarget.isDummy());

		Formula pathZero = (m.getCFG().isDominatedBy(predZero, trueTarget)) ?
				condBlock.getCondExpr() :
				LogicUtil.ff.not(condBlock.getCondExpr());

		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value defVal;
		if (!m.hasValue(instruction.getDef())) {
			defVal = edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value
					.createByType(instruction.getDef(), m.getValue(instruction.getUse(0)).getType());
			m.addValue(instruction.getDef(), defVal);
		} else {
			defVal = m.getValue(instruction.getDef());
		}
		assert defVal != null;
		m.setDepsForvalue(instruction.getDef(), LogicUtil.ternaryOp(pathZero, op1, op2));
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

	@Override public void visitBinaryOp(SSABinaryOpInstruction instruction) {
		Formula[] op1 = m.getDepsForValue(instruction.getUse(0));
		Formula[] op2 = m.getDepsForValue(instruction.getUse(1));

		int def = instruction.getDef();

		IBinaryOpInstruction.Operator operator = (IBinaryOpInstruction.Operator) instruction.getOperator();

		// make sure Value object for def exists
		if (!m.hasValue(def)) {
			edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value defVal = edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value
					.createByType(def,
							Type.getResultType(operator, m.type(instruction.getUse(0)), m.type(instruction.getUse(1))));
			m.addValue(def, defVal);
		}
		Formula[] defForm = null;
		switch (operator) {
		case SUB:
			if (LogicUtil.isConstant(op2) && LogicUtil.numericValue(op2) == 0) {
				defForm = op1;
			} else {
				defForm = LogicUtil.sub(op1, op2);
			}
			break;
		case ADD:
			if (LogicUtil.isConstant(op1) && LogicUtil.numericValue(op1) == 0) {
				defForm = op2;
			} else if (LogicUtil.isConstant(op2) && LogicUtil.numericValue(op2) == 0) {
				defForm = op1;
			} else {
				defForm = LogicUtil.add(op1, op2);
			}
			break;
		case MUL:
			if (LogicUtil.isConstant(op1) && LogicUtil.numericValue(op1) == 1) {
				defForm = op2;
			} else if (LogicUtil.isConstant(op2) && LogicUtil.numericValue(op2) == 1) {
				defForm = op1;
			} else {
				defForm = LogicUtil.mult(op1, op2);
			}
			break;
		case DIV:
		case REM:
			break;
		case AND:
			defForm = LogicUtil.and(op1, op2);
			break;
		case OR:
			defForm = LogicUtil.or(op1, op2);
			break;
		case XOR:
			defForm = LogicUtil.xor(op1, op2);
			break;
		}
		assert defForm != null;
		m.setDepsForvalue(def, defForm);
	}

	@Override public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
		int def = instruction.getDef();
		int opValNum = instruction.getUse(0);

		if (!m.hasValue(opValNum)) {
			staticAnalysis.createConstant(opValNum);
		}
		Formula[] op = m.getDepsForValue(opValNum);

		IUnaryOpInstruction.Operator operator = (IUnaryOpInstruction.Operator) instruction.getOpcode();

		// make sure Value object for def exists
		if (!m.hasValue(def)) {
			edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value defVal = Value
					.createByType(def, Type.getResultType(operator, m.type(opValNum)));
			m.addValue(def, defVal);
		}

		if (operator == IUnaryOpInstruction.Operator.NEG) {
			m.setDepsForvalue(def, LogicUtil.neg(op));
		}
	}

	@Override public void visitConversion(SSAConversionInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

}
