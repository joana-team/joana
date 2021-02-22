package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

public class StaticAnalysis {

	private final Program program;
	private final Method entry;
	public final FormulaFactory f;

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

	/*
	constructor used for testing
	 */
	public StaticAnalysis(FormulaFactory ff, Program p) {
		this.program = p;
		this.entry = p.getEntryMethod();
		this.f = ff;
	}

	public Formula[] createVars(int valNum, Type type) {
		// For now we dont allow user-defined types
		assert (type != Type.CUSTOM);

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

	/**
	 * turns character '0' into 'false', everything else into 'true'
	 * @param arr a character array
	 * @return array representing the input characters as boolean constants
	 */
	public Formula[] asFormulaArray(char[] arr) {
		Formula[] form = new Formula[arr.length];

		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == '0') {
				form[i] = f.constant(false);
			} else {
				form[i] = f.constant(true);
			}
		}
		return form;
	}

	/**
	 * returns the two's complement of number n as a char array of a specified length.
	 * The least significant bit will be at the highest index of the array
	 * @param n the converted number
	 * @return two's complement of n
	 */
	public char[] twosComplement(int n, int length) {
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

	// trim the array arr to the specified size. If arr is longer than size, the front is cut off.
	// If arr is shorter than size, it is prefixed with the appropriate amount of the placeholder char 0
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
			entry.setDepsForvalue(params[i], createVars(params[i], entry.getParamType(i)));
		}

		// initialize formula arrays for all constant values
		entry.getProgramValues().values().stream().filter(Value::isConstant).forEach(c -> {
			try {
				c.setDeps(asFormulaArray(binaryRep(c.getVal(), c.getType())));
			} catch (UnexpectedTypeException e) {
				e.printStackTrace();
			}
		});

		SATVisitor sv = new SATVisitor(this);

		for (BBlock bBlock: program.getEntryMethod().getCFG().getBlocks()) {
			try {
				sv.visitBlock(program.getEntryMethod(), bBlock, -1);
			} catch (OutOfScopeException e) {
				e.printStackTrace();
			}
		}
	}

	private char[] binaryRep(Object val, Type type) throws UnexpectedTypeException {
		if (type == Type.INTEGER) {
			return twosComplement((Integer) val, Type.INTEGER.bitwidth());
		} else {
			throw new UnexpectedTypeException(type);
		}
	}

	public void createConstant(int op1) {
		Int constant = (Int) entry.getValue(op1);
		assert constant != null;
		constant.setDeps(asFormulaArray(twosComplement((Integer) constant.getVal(), constant.getWidth())));
	}

}
