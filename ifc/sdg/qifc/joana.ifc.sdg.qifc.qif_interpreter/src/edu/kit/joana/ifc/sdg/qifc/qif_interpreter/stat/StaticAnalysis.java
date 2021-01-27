package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Int;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class StaticAnalysis {

	private final Program program;
	private final Method entry;
	private final FormulaFactory f;

	public StaticAnalysis(Program program) throws InvalidClassFileException {
		this.program = program;
		this.entry = program.getEntryMethod();
		this.f = new FormulaFactory();
	}

	/*
	constructor used for testing
	 */
	public StaticAnalysis(FormulaFactory ff) {
		this.program = null;
		this.entry = null;
		this.f = ff;
	}

	private Formula[] createVars(int valNum, Type type) {
		// For now we dont allow user-defined types
		assert(type != Type.CUSTOM);

		Formula[] vars = new Formula[type.bitwidth()];
		for (int i = 0; i < type.bitwidth(); i++) {
			Variable var = f.variable(generateVarName(valNum, i));
			vars[i] = f.literal(var.name(), true);
		}
		return vars;
	}

	private String generateVarName(int valNum, int bit) {
		return valNum + "::" + bit;
	}

	/*
	   transforms the constant value n into an array of boolean constants representing the same value as binary.
	   If the binary representation needs more bits than {@code} Type.bitwidth(), the leading bits are cut off
	 */
	private Formula[] asFormulaArray(int n) {
		char[] binary = Integer.toBinaryString(n).toCharArray();
		binary = trim(binary, Type.INTEGER.bitwidth(), '0');
		Formula[] form = new Formula[Type.INTEGER.bitwidth()];

		int startIdx = binary.length - Type.INTEGER.bitwidth();
		for (int i = 0; i < binary.length; i++) {
			if (binary[startIdx + i] == '1') {
				form[i] = f.constant(true);
			} else {
				form[i] = f.constant(false);
			}
		}
		return form;
	}

	// trim the array arr to the specified size. If arr is longer than size, the front is cut off.
	// If arr is shorter than size, it is prefixed with the appropriate amount of the placeholder char
	private char[] trim(char[] arr, int size, char placeholder) {
		char[] trimmed = new char[size];
		for (int i = 0; i < size; i++) {
			trimmed[size - 1 - i] = (arr.length - 1 - i >= 0) ? arr[arr.length - 1 - i] : placeholder;
		}
		return trimmed;
	}

	public void computeSATDeps() {

		// create literals for method parameters
		int[] params = this.entry.getIr().getParameterValueNumbers();
		for (int i = 1; i < params.length; i++) {
			program.setDepsForvalue(params[i], createVars(params[i], entry.getParamType(i)));
		}

		entry.getIr().visitAllInstructions(new SATVisitor());
	}

	public class SATVisitor extends SSAInstruction.Visitor {

		@Override public void visitBinaryOp(SSABinaryOpInstruction instruction) {
			System.out.println("Visiting " + instruction);

			int op1ValNum = instruction.getUse(0);
			int op2ValNum = instruction.getUse(1);

			if (!program.hasValue(op1ValNum)) {
				// if there doesn't exist a value object for this valueNumber at this point, it has to be constant
				createConstant(op1ValNum);
			}
			Formula[] op1 = program.getDepsForValue(op1ValNum);

			if (!program.hasValue(op2ValNum)) {
				createConstant(op2ValNum);
			}
			Formula[] op2 = program.getDepsForValue(op2ValNum);

			int def = instruction.getDef();

			IBinaryOpInstruction.Operator operator = (IBinaryOpInstruction.Operator) instruction.getOperator();

			// make sure Value object for def exists
			program.getOrCreateValue(def, Type.getResultType(operator, program.type(op1ValNum), program.type(op2ValNum)), entry);
			Formula[] defForm = null;
			switch (operator) {
			case ADD:
				defForm = add(op1, op2);
				break;
			case SUB:
				defForm = sub(op1, op2);
				break;
			case MUL:
				defForm = mult(op1, op2);
				break;
			case DIV:
				break;
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
			program.setDepsForvalue(def, defForm);
		}

		@Override public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
			System.out.println("Visiting " + instruction);
			int def = instruction.getDef();
			int opValNum = instruction.getUse(0);

			if (!program.hasValue(opValNum)) {
				createConstant(opValNum);
			}
			Formula[] op = program.getDepsForValue(opValNum);

			IUnaryOpInstruction.Operator operator = (IUnaryOpInstruction.Operator) instruction.getOpcode();

			// make sure Value object for def exists
			program.getOrCreateValue(def, Type.getResultType(operator, program.type(opValNum)), entry);

			switch(operator) {
			case NEG:
				program.setDepsForvalue(def, not(op));
				break;
			}
		}

		// create formula for bitwise or of op1 and op2 and assign to def
		public Formula[] or(Formula[] op1, Formula[] op2) {
			Formula[] defForm = new Formula[op1.length];

			for (int i = 0; i < op1.length; i++) {
				defForm[i] = f.or(op1[i], op2[i]);
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
			return f.and(f.or(op1, op2), f.or(f.not(op1), f.not(op2)));
		}

		// create formula for bitwise or of op1 and op2 and assign to def
		public Formula[] and(Formula[] op1, Formula[] op2) {

			Formula[] defForm = new Formula[op1.length];

			for (int i = 0; i < op1.length; i++) {
				defForm[i] = f.and(op1[i], op2[i]);
			}
			return defForm;
		}

		public Formula[] add(Formula[] op1, Formula[] op2) {
			Formula[] res = new Formula[op1.length];
			Formula carry = f.constant(false);

			for (int i = op1.length - 1; i >= 0; i--) {
				res[i] = xor(xor(op1[i], op2[i]), carry);
				carry = f.or(f.or(f.and(op1[i], op2[i]), f.and(op1[i], carry), f.and(op2[i], carry)));
			}
			return res;
		}

		public Formula[] sub(Formula[] a, Formula[] b) {
			Formula carry = f.constant(false);
			Formula[] res = new Formula[a.length];

			for (int i = a.length - 1; i >= 0; i--) {
				res[i] = f.and(f.or(a[i], xor(b[i], carry)), f.or(f.not(a[i]), f.equivalence(b[i], carry)));
				carry = f.or(f.and(a[i], b[i], carry), f.and(f.not(a[i]), f.or(b[i], carry)));
			}

			return res;
		}

		public Formula[] mult(Formula[] op1, Formula[] op2) {
			Formula[][]carry = new Formula[op1.length][op1.length];

			for (int i = op1.length - 1; i >= 0; i--) {

				for (int j = op1.length - 1; j >= 0; j--) {
					if (j > i) {
						carry[i][j] = f.constant(false);
					} else {
						carry[i][j] = f.and(op1[i], op2[j + (op1.length - i - 1)]);
					}
				}
			}

			Formula[] res = asFormulaArray(0);
			for(int i = 0; i < op1.length; i++) {
				res = add(res, carry[i]);
			}

			return res;
		}


		public Formula[] not(Formula[] op) {
			Formula[] defForm = new Formula[op.length];

			for (int i = 0; i < op.length; i++) {
				defForm[i] = f.not(op[i]);
			}
			return defForm;
		}
	}

	private void createConstant(int op1) {
		Int constant = Int.getOrCreateConstant(entry.getIntConstant(op1), asFormulaArray(entry.getIntConstant(op1)));
		constant.setDeps(asFormulaArray(entry.getIntConstant(op1)));
		program.createValue(op1, constant);
	}
}
