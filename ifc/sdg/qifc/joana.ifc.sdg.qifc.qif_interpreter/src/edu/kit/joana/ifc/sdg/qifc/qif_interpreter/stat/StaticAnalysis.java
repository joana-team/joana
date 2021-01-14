package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class StaticAnalysis {

	private final Program program;
	private final Method entry;
	private final FormulaFactory f;

	/**
	 * Each value is represented by an array of propositional formulas, where each formula corresponds to the value of a single bit.
	 * The first array entry represents the most significant bit and so on
	 */
	private final Map<Integer, Formula[]> valsToLogicVars;

	public StaticAnalysis(Program program) throws InvalidClassFileException {
		this.program = program;
		this.entry = Method.getEntryMethodFromProgram(program);
		this.f = new FormulaFactory();
		this.valsToLogicVars = new HashMap<>();
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
	   transforms the constant value n into an array of boolean constants representing the same value.
	   If the binary representation needs more bits than {@code} Type.INTEGER.bitwidth(), the leading bits are cut off
	 */
	private Formula[] asFormulaArray(int n) {
		char[] binary = Integer.toBinaryString(n).toCharArray();
		System.out.println(binary);
		binary = trim(binary, Type.INTEGER.bitwidth(), '0');
		System.out.println(binary);
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

	private char[] trim(char[] arr, int size, char placeholder) {
		char[] trimmed = new char[size];
		for (int i = 0; i < size; i++) {
			trimmed[size - 1 - i] = (arr.length - 1 - i >= 0) ? arr[arr.length - 1 - i] : placeholder;
		}
		return trimmed;
	}

	public void run() {

		// first we generate variables for the entry method parameters
		int[] paramvalNums = entry.getIr().getParameterValueNumbers();

		for(int i = 0; i < paramvalNums.length; i++) {
			// first parameter is this-reference --> skip
			if (i == 0) continue;
			Formula[] vars = createVars(paramvalNums[i], entry.getParamType(i));
			this.valsToLogicVars.put(paramvalNums[i], vars);
		}
	}

	private class SATVisitor extends SSAInstruction.Visitor {

		@Override public void visitBinaryOp(SSABinaryOpInstruction instruction) {

			int def = instruction.getDef();
			assert(!valsToLogicVars.containsKey(def));

			int op1 = instruction.getUse(0);
			int op2 = instruction.getUse(1);

			IBinaryOpInstruction.Operator operator = (IBinaryOpInstruction.Operator) instruction.getOperator();

			Formula[] defForm = null;
			switch (operator) {
			case ADD:
				break;
			case SUB:
				break;
			case MUL:
				break;
			case DIV:
				break;
			case REM:
				break;
			case AND:
				break;
			case OR:
				parseOrFormula(def, op1, op2);
				break;
			case XOR:
				break;
			}
			valsToLogicVars.put(def, defForm);
		}

		// create formula for bitwise or of op1 and op2 and assign to def
		private Formula[] parseOrFormula(int def, int op1, int op2) {
			Formula[] op1Formula = valsToLogicVars.get(op1);
			Formula[] op2Formula = valsToLogicVars.get(op2);

			// sanity checks
			assert(op1Formula != null);
			assert(op2Formula != null);
			assert(op1Formula.length == op2Formula.length);

			Formula[] defForm = new Formula[op1Formula.length];

			for (int i = 0; i < op1Formula.length; i++) {
				defForm[i] = f.or(op1Formula[i], op2Formula[i]);
			}
			return defForm;
		}
	}
}
