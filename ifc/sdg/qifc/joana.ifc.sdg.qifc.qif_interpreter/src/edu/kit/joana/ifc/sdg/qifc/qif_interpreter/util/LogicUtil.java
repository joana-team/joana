package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.*;
import org.logicng.predicates.CNFPredicate;
import org.logicng.solvers.MiniSat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

public class LogicUtil {

	public static final FormulaFactory ff = new FormulaFactory();

	public static void exportAsCNF(Formula f, String dest, List<Variable> samplingSet) throws IOException {

		Formula cnf = f.cnf();
		writeDimacsFile(dest, cnf, samplingSet, true);

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

	public static Variable[] createVars(int valNum, int width) {

		Variable[] vars = new Variable[width];
		for (int i = 0; i < width; i++) {
			Variable var = ff.variable(generateVarName(valNum, i));
			vars[i] = var;
		}
		return vars;
	}

	public static Variable[] createVars(int valNum, int width, String ident) {

		Variable[] vars = new Variable[width];
		for (int i = 0; i < width; i++) {
			Variable var = ff.variable(ident + "_" + generateVarName(valNum, i));
			vars[i] = var;
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

	public static boolean isConstant(Formula f) {
		return f.isConstantFormula();
	}

	public static boolean isConstant(Formula[] formulas) {
		return Arrays.stream(formulas).allMatch(Formula::isConstantFormula);
	}

	public static int numericValue(Formula[] binary) {
		assert (isConstant(binary));

		char[] binRep = new char[binary.length];
		IntStream.range(0, binary.length).forEach(i -> binRep[i] = (binary[i].equals(ff.constant(true))) ? '1' : '0');

		int num = Integer.parseInt(String.valueOf(binRep), 2);
		return (binRep[0] == '0') ? num : num - (1 << binRep.length);
	}

	public static Tristate isSat(Formula f) {
		MiniSat sat = MiniSat.miniSat(ff);
		sat.add(f);
		return sat.sat();
	}

	public static Formula isEqual(Formula[] a, Formula[] b) {
		return IntStream.range(0, a.length).mapToObj(i -> ff.equivalence(a[i], b[i])).reduce(ff.constant(true), ff::and);
	}

	/**
	 * Writes a given formula's internal data structure as a dimacs file.  Must only be called with a formula which is in CNF.
	 *
	 * @param fileName     the file name of the dimacs file to write
	 * @param formula      the formula
	 * @param samplingSet  set of h varaibles for which we want to estimate the # models
	 * @param writeMapping indicates whether an additional file for translating the ids to variable names shall be written
	 * @throws IOException              if there was a problem writing the file
	 * @throws IllegalArgumentException if the formula was not in CNF
	 */
	public static void writeDimacsFile(final String fileName, final Formula formula, List<Variable> samplingSet,
			final boolean writeMapping) throws IOException {
		final File file = new File(fileName.endsWith(".cnf") ? fileName : fileName + ".cnf");
		final SortedMap<Variable, Long> var2id = new TreeMap<>();
		long i = 1;
		Set<Variable> varsToMap = new TreeSet<>(formula.variables());
		varsToMap.addAll(samplingSet);
		for (final Variable var : varsToMap) {
			var2id.put(var, i++);
		}
		if (!formula.holds(CNFPredicate.get())) {
			throw new IllegalArgumentException("Cannot write a non-CNF formula to dimacs.  Convert to CNF first.");
		}
		final List<Formula> parts = new ArrayList<>();
		if (formula.type().equals(FType.LITERAL) || formula.type().equals(FType.OR)) {
			parts.add(formula);
		} else {
			for (final Formula part : formula) {
				parts.add(part);
			}
		}
		final StringBuilder sb = new StringBuilder("p cnf ");
		final int partsSize = formula.type().equals(FType.FALSE) ? 1 : parts.size();
		sb.append(var2id.size()).append(" ").append(partsSize).append(System.lineSeparator());

		// add sampling set
		sb.append("c ind ");
		samplingSet.forEach(v -> sb.append(var2id.get(v)).append(" "));
		sb.append("0").append(System.lineSeparator());

		for (final Formula part : parts) {
			for (final Literal lit : part.literals()) {
				sb.append(lit.phase() ? "" : "-").append(var2id.get(lit.variable())).append(" ");
			}
			sb.append(String.format(" 0%n"));
		}
		if (formula.type().equals(FType.FALSE)) {
			sb.append(String.format("0%n"));
		}
		try (final BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			writer.append(sb);
			writer.flush();
		}
		if (writeMapping) {
			final String mappingFileName =
					(fileName.endsWith(".cnf") ? fileName.substring(0, fileName.length() - 4) : fileName) + ".map";
			writeMapping(new File(mappingFileName), var2id);
		}
	}

	private static void writeMapping(final File mappingFile, final SortedMap<Variable, Long> var2id)
			throws IOException {
		final StringBuilder sb = new StringBuilder();
		for (final Map.Entry<Variable, Long> entry : var2id.entrySet()) {
			sb.append(entry.getKey()).append(";").append(entry.getValue()).append(System.lineSeparator());
		}
		try (final BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(mappingFile), StandardCharsets.UTF_8))) {
			writer.append(sb);
			writer.flush();
		}
	}

	public static Formula ternaryOp(Formula if_, Formula then_, Formula else_) {
		return ff.and(ff.implication(if_, then_), ff.implication(ff.not(if_), else_));
	}

	public static Formula[] ternaryOp(Formula if_, Formula[] then_, Formula[] else_) {
		Formula[] ret = new Formula[then_.length];
		IntStream.range(0, ret.length).forEach(i -> ret[i] = ternaryOp(if_, then_[i], else_[i]));
		return ret;
	}

	public static Formula[] applySubstitution(Formula[] f, Substitution s) {
		Formula[] res = new Formula[f.length];
		IntStream.range(0, res.length).forEach(i -> res[i] = f[i].substitute(s.toLogicNGSubstitution()));
		return res;
	}

	public static Formula applySubstitution(Formula f, Substitution s) {
		return f.substitute(s.toLogicNGSubstitution());
	}

	public static boolean containsAny(Formula f, Set<Variable> vars) {
		return vars.stream().anyMatch(f::containsVariable);
	}

	public static Formula[][] applySubstitution(Formula[][] f, Substitution s) {
		Formula[][] res = new Formula[f.length][f[0].length];
		IntStream.range(0, res.length).forEach(i -> res[i] = applySubstitution(f[i], s));
		return res;
	}

	public static Formula[] shl(Formula[] op1, Formula[] op2) {
		Formula[] res = new Formula[op1.length];
		for (int i = 0; i < op1.length; i++) {
			res[i] = ff.constant(false);
			for (int j = 0; j < Type.INTEGER.bitwidth(); j++) {
				Formula shiftVal = (i + j) > Type.INTEGER.bitwidth() - 1 ? ff.constant(false) :  op1[(i + j)];
				res[i] = ternaryOp(isEqual(asFormulaArray(twosComplement(j, Type.INTEGER.bitwidth())), op2), shiftVal, res[i]);
			}
		}
		return res;
	}
}