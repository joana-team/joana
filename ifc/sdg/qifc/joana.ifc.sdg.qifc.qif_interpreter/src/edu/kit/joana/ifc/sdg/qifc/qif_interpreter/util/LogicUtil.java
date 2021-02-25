package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.writers.FormulaDimacsFileWriter;

import java.io.IOException;

public class LogicUtil {

	public static final FormulaFactory ff = new FormulaFactory();

	public static void exportAsCNF(Formula f, String dest) throws IOException {

		Formula cnf = f.cnf();
		FormulaDimacsFileWriter.write(dest, cnf, true);

	}

	public static Formula[] neg(Formula[] op) {
		Formula[] defForm = new Formula[op.length];

		// invert
		for (int i = 0; i < op.length; i++) {
			defForm[i] = ff.not(op[i]);
		}

		// add 1
		defForm = add(defForm, asFormulaArray(twosComplement(1, defForm.length)));
		return defForm;
	}

	// create formula for bitwise or of op1 and op2 and assign to def
	public static Formula[] or(Formula[] op1, Formula[] op2) {
		Formula[] defForm = new Formula[op1.length];

		for (int i = 0; i < op1.length; i++) {
			defForm[i] = ff.or(op1[i], op2[i]);
		}
		return defForm;
	}

	// create formula for bitwise or of op1 and op2 and assign to def
	public static Formula[] xor(Formula[] op1, Formula[] op2) {
		Formula[] defForm = new Formula[op1.length];

		for (int i = 0; i < op1.length; i++) {
			defForm[i] = xor(op1[i], op2[i]);
		}
		return defForm;
	}

	public static Formula xor(Formula op1, Formula op2) {
		return ff.and(ff.or(op1, op2), ff.or(ff.not(op1), ff.not(op2)));
	}

	// create formula for bitwise or of op1 and op2 and assign to def
	public static Formula[] and(Formula[] op1, Formula[] op2) {

		Formula[] defForm = new Formula[op1.length];

		for (int i = 0; i < op1.length; i++) {
			defForm[i] = ff.and(op1[i], op2[i]);
		}
		return defForm;
	}

	public static Formula[] add(Formula[] op1, Formula[] op2) {
		Formula[] res = new Formula[op1.length];
		Formula carry = ff.constant(false);

		for (int i = op1.length - 1; i >= 0; i--) {
			res[i] = xor(xor(op1[i], op2[i]), carry);
			carry = ff.or(ff.or(ff.and(op1[i], op2[i]), ff.and(op1[i], carry), ff.and(op2[i], carry)));
		}
		return res;
	}

	public static Formula[] sub(Formula[] a, Formula[] b) {
		Formula carry = ff.constant(false);
		Formula[] res = new Formula[a.length];

		for (int i = a.length - 1; i >= 0; i--) {
			res[i] = ff.and(ff.or(a[i], xor(b[i], carry)), ff.or(ff.not(a[i]), ff.equivalence(b[i], carry)));
			carry = ff.or(ff.and(a[i], b[i], carry), ff.and(ff.not(a[i]), ff.or(b[i], carry)));
		}

		return res;
	}

	public static Formula[] mult(Formula[] op1, Formula[] op2) {
		Formula[][] carry = new Formula[op1.length][op1.length];

		for (int i = op1.length - 1; i >= 0; i--) {

			for (int j = op1.length - 1; j >= 0; j--) {
				if (j > i) {
					carry[i][j] = ff.constant(false);
				} else {
					carry[i][j] = ff.and(op1[i], op2[j + (op1.length - i - 1)]);
				}
			}
		}

		Formula[] res = asFormulaArray(twosComplement(0, op1.length));
		for (int i = 1; i < op1.length; i++) {
			res = add(res, carry[i]);
		}
		res = sub(res, carry[0]);
		return res;
	}

	public static Formula equalsZero(Formula[] diff) {

		Formula res = ff.equivalence(ff.constant(false), diff[0]);
		for (int i = 1; i < diff.length; i++) {
			res = ff.and(ff.equivalence(ff.constant(false), diff[i]), res);
		}
		return res;
	}

	public static Formula[] createVars(int valNum, Type type) {
		// For now we dont allow user-defined types
		assert (type != Type.CUSTOM);

		Formula[] vars = new Formula[type.bitwidth()];
		for (int i = 0; i < type.bitwidth(); i++) {
			Variable var = ff.variable(generateVarName(valNum, i));
			vars[i] = ff.literal(var.name(), true);
		}
		return vars;
	}

	private static String generateVarName(int valNum, int bit) {
		return valNum + "::" + bit;
	}

	/**
	 * turns character '0' into 'false', everything else into 'true'
	 *
	 * @param arr a character array
	 * @return array representing the input characters as boolean constants
	 */
	public static Formula[] asFormulaArray(char[] arr) {
		Formula[] form = new Formula[arr.length];

		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == '0') {
				form[i] = ff.constant(false);
			} else {
				form[i] = ff.constant(true);
			}
		}
		return form;
	}

	/**
	 * returns the two's complement of number n as a char array of a specified length.
	 * The least significant bit will be at the highest index of the array
	 *
	 * @param n the converted number
	 * @return two's complement of n
	 */
	public static char[] twosComplement(int n, int length) {
		char[] binary = Integer.toBinaryString(Math.abs(n)).toCharArray();

		binary = trim(binary, length, '0');

		if (n >= 0) {
			return binary;
		}

		// invert
		for (int i = 0; i < binary.length; i++) {
			binary[i] = (binary[i] == '0') ? '1' : '0';
		}

		// subtract 1
		for (int i = binary.length - 1; i >= 0; i--) {
			if (binary[i] == '0') {
				binary[i] = '1';
				break;
			} else {
				binary[i] = '0';
			}
		}
		return binary;
	}

	public static char[] binaryRep(Object val, Type type) throws UnexpectedTypeException {
		if (type == Type.INTEGER) {
			return twosComplement((Integer) val, Type.INTEGER.bitwidth());
		} else {
			throw new UnexpectedTypeException(type);
		}
	}

	// trim the array arr to the specified size. If arr is longer than size, the front is cut off.
	// If arr is shorter than size, it is prefixed with the appropriate amount of the placeholder char 0
	private static char[] trim(char[] arr, int size, char placeholder) {
		char[] trimmed = new char[size];
		for (int i = 0; i < size; i++) {
			trimmed[size - 1 - i] = (arr.length - 1 - i >= 0) ? arr[arr.length - 1 - i] : placeholder;
		}
		return trimmed;
	}

}
