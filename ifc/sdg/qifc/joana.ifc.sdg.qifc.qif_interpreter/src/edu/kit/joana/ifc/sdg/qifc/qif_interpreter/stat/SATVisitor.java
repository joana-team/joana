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
import org.logicng.formulas.Formula;

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
		Formula[] diff = sub(op1, op2);

		switch (operator) {
		case EQ:
			defForm = equalsZero(diff);
			break;
		case NE:
			defForm = staticAnalysis.f.not(equalsZero(diff));
			break;
		case LT:
			defForm = staticAnalysis.f.equivalence(staticAnalysis.f.constant(true), diff[0]);
			break;
		case GE:
			defForm = staticAnalysis.f.equivalence(staticAnalysis.f.constant(false), diff[0]);
			break;
		case GT:
			defForm = staticAnalysis.f.and(staticAnalysis.f.not(equalsZero(diff)),
					staticAnalysis.f.equivalence(staticAnalysis.f.constant(false), diff[0]));
			break;
		case LE:
			defForm = staticAnalysis.f
					.or(equalsZero(diff), staticAnalysis.f.equivalence(staticAnalysis.f.constant(true), diff[0]));
			break;
		default:
			throw new IllegalStateException("Unexpected value: " + operator);
		}
		block.setCondExpr(defForm);
	}

	private Formula equalsZero(Formula[] diff) {

		Formula res = staticAnalysis.f.equivalence(staticAnalysis.f.constant(false), diff[0]);
		for (int i = 1; i < diff.length; i++) {
			res = staticAnalysis.f.and(staticAnalysis.f.equivalence(staticAnalysis.f.constant(false), diff[i]));
		}
		return res;
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
		if (instruction.getCallSite().getDeclaredTarget().getSignature().equals(OUTPUT_FUNCTION)) {
			m.getValue(instruction.getUse(0)).leak();
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

	/**
	 * Use fresh variables for the defined value, bc depending on the program, the values that this instruction depends on may not have been evaluated yet
	 * Remember to add equality constraints later on!
	 *
	 * @param instruction a phi instruction
	 */
	@Override public void visitPhi(SSAPhiInstruction instruction) {
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
		m.setDepsForvalue(instruction.getDef(), staticAnalysis.createVars(defVal.getValNum(), defVal.getType()));
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
			defForm = sub(op1, op2);
			break;
		case ADD:
			defForm = add(op1, op2);
			break;
		case MUL:
			defForm = mult(op1, op2);
			break;
		case DIV:
		case REM:
			break;
		case AND:
			defForm = and(op1, op2);
			break;
		case OR:
			defForm = or(op1, op2);
			break;
		case XOR:
			defForm = xor(op1, op2);
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
			m.setDepsForvalue(def, neg(op));
		}
	}

	@Override public void visitConversion(SSAConversionInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	// create formula for bitwise or of op1 and op2 and assign to def
	public Formula[] or(Formula[] op1, Formula[] op2) {
		Formula[] defForm = new Formula[op1.length];

		for (int i = 0; i < op1.length; i++) {
			defForm[i] = staticAnalysis.f.or(op1[i], op2[i]);
		}
		return defForm;
	}

	// create formula for bitwise or of op1 and op2 and assign to def
	public Formula[] xor(Formula[] op1, Formula[] op2) {
		Formula[] defForm = new Formula[op1.length];

		for (int i = 0; i < op1.length; i++) {
			defForm[i] = xor(op1[i], op2[i]);
		}
		return defForm;
	}

	public Formula xor(Formula op1, Formula op2) {
		return staticAnalysis.f.and(staticAnalysis.f.or(op1, op2),
				staticAnalysis.f.or(staticAnalysis.f.not(op1), staticAnalysis.f.not(op2)));
	}

	// create formula for bitwise or of op1 and op2 and assign to def
	public Formula[] and(Formula[] op1, Formula[] op2) {

		Formula[] defForm = new Formula[op1.length];

		for (int i = 0; i < op1.length; i++) {
			defForm[i] = staticAnalysis.f.and(op1[i], op2[i]);
		}
		return defForm;
	}

	public Formula[] add(Formula[] op1, Formula[] op2) {
		Formula[] res = new Formula[op1.length];
		Formula carry = staticAnalysis.f.constant(false);

		for (int i = op1.length - 1; i >= 0; i--) {
			res[i] = xor(xor(op1[i], op2[i]), carry);
			carry = staticAnalysis.f.or(staticAnalysis.f
					.or(staticAnalysis.f.and(op1[i], op2[i]), staticAnalysis.f.and(op1[i], carry),
							staticAnalysis.f.and(op2[i], carry)));
		}
		return res;
	}

	public Formula[] sub(Formula[] a, Formula[] b) {
		Formula carry = staticAnalysis.f.constant(false);
		Formula[] res = new Formula[a.length];

		for (int i = a.length - 1; i >= 0; i--) {
			res[i] = staticAnalysis.f.and(staticAnalysis.f.or(a[i], xor(b[i], carry)),
					staticAnalysis.f.or(staticAnalysis.f.not(a[i]), staticAnalysis.f.equivalence(b[i], carry)));
			carry = staticAnalysis.f.or(staticAnalysis.f.and(a[i], b[i], carry),
					staticAnalysis.f.and(staticAnalysis.f.not(a[i]), staticAnalysis.f.or(b[i], carry)));
		}

		return res;
	}

	public Formula[] mult(Formula[] op1, Formula[] op2) {
		Formula[][] carry = new Formula[op1.length][op1.length];

		for (int i = op1.length - 1; i >= 0; i--) {

			for (int j = op1.length - 1; j >= 0; j--) {
				if (j > i) {
					carry[i][j] = staticAnalysis.f.constant(false);
				} else {
					carry[i][j] = staticAnalysis.f.and(op1[i], op2[j + (op1.length - i - 1)]);
				}
			}
		}

		Formula[] res = staticAnalysis.asFormulaArray(staticAnalysis.twosComplement(0, op1.length));
		for (int i = 1; i < op1.length; i++) {
			res = add(res, carry[i]);
		}
		res = sub(res, carry[0]);
		return res;
	}

	public Formula[] neg(Formula[] op) {
		Formula[] defForm = new Formula[op.length];

		// invert
		for (int i = 0; i < op.length; i++) {
			defForm[i] = staticAnalysis.f.not(op[i]);
		}

		// add 1
		defForm = add(defForm, staticAnalysis.asFormulaArray(staticAnalysis.twosComplement(1, defForm.length)));
		return defForm;
	}
}
