package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.ssa.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.ISATAnalysisFragment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;

public class SATVisitor implements SSAInstruction.IVisitor {
	private static final String OUTPUT_FUNCTION = "edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out.print(I)V";

	private boolean containsOutOfScopeInstruction;
	private SSAInstruction outOfScopeInstruction;
	private BBlock block;
	private ISATAnalysisFragment m;

	public void visitBlock(ISATAnalysisFragment m, BBlock b) throws OutOfScopeException {
		this.block = b;
		this.m = m;

		for (SSAInstruction i : b.instructions()) {
			i.visit(this);
		}

		if (containsOutOfScopeInstruction) {
			throw new OutOfScopeException(outOfScopeInstruction);
		}
	}

	public SATVisitor() {
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

		if (m.getDepsForValnum(op1ValNum) == null) {
			// if there doesn't exist a value object for this valueNumber at this point, it has to be constant
			StaticAnalysis.createConstant(op1ValNum, m);
		}
		Formula[] op1 = m.getDepsForValnum(op1ValNum);

		if (m.getDepsForValnum(op2ValNum) == null) {
			StaticAnalysis.createConstant(op2ValNum, m);
		}
		Formula[] op2 = m.getDepsForValnum(op2ValNum);

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
		// do nothing
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
		// do nothing
		// marking the value as leaked is happening during execution in the interpreter
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
		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value defVal;

		// find the operator for which there already exists as value object
		int op = (m.hasValnum(instruction.getUse(0))) ? instruction.getUse(0) : instruction.getUse(1);

		if (!m.hasValnum(instruction.getDef())) {
			m.createValnum(instruction.getDef(), instruction);
		}
		// TODO: create new vars as soon as a value is created anywhere to stop formula arrays from containing null --> then this line will become überflüssig
		m.setDepsForValnum(instruction.getDef(),
				LogicUtil.createVars(instruction.getDef(), m.getDepsForValnum(instruction.getDef()).length));
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
		Formula[] op1 = m.getDepsForValnum(instruction.getUse(0));
		Formula[] op2 = m.getDepsForValnum(instruction.getUse(1));

		int def = instruction.getDef();

		IBinaryOpInstruction.Operator operator = (IBinaryOpInstruction.Operator) instruction.getOperator();

		// make sure Value object for def exists
		if (!m.hasValnum(def)) {
			m.createValnum(instruction.getDef(), instruction);
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
		m.setDepsForValnum(def, defForm);
	}

	@Override public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
		int def = instruction.getDef();
		int opValNum = instruction.getUse(0);

		if (!m.hasValnum(opValNum)) {
			StaticAnalysis.createConstant(opValNum, m);
		}
		Formula[] op = m.getDepsForValnum(opValNum);

		IUnaryOpInstruction.Operator operator = (IUnaryOpInstruction.Operator) instruction.getOpcode();

		// make sure Value object for def exists
		if (!m.hasValnum(def)) {
			m.createValnum(instruction.getDef(), instruction);
		}

		if (operator == IUnaryOpInstruction.Operator.NEG) {
			m.setDepsForValnum(def, LogicUtil.neg(op));
		}
	}

	@Override public void visitConversion(SSAConversionInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

}
