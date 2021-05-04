package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSANewInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.stream.IntStream;

public class Array<T extends Value> extends Value {

	private static final int LENGTH = Type.INTEGER.bitwidth();

	private Type elementType;
	private T[] arr;
	private Formula[][] valueDependencies;
	private Variable[][] vars;

	public Array(int valNum) {
		super(valNum);
		this.setType(Type.ARRAY);
		this.setWidth(Type.ARRAY.bitwidth());
	}

	public static Array<? extends Value> newArray(Type t, int valNum, boolean initWithVars)
			throws UnexpectedTypeException {
		if (t == Type.INTEGER) {
			Array<Int> array = new Array<>(valNum);
			array.arr = new Int[LENGTH];
			array.elementType = t;
			IntStream.range(0, LENGTH).forEach(i -> array.arr[i] = new Int(i));
			IntStream.range(0, LENGTH).forEach(i -> array.arr[i].setVal(0, -1));
			array.valueDependencies = (initWithVars) ? array.initVars(LENGTH) : array.initZeros(LENGTH);
			return array;
		}
		throw new UnexpectedTypeException(t);
	}


	public static Array<? extends Value> newArray(int valNum, Object[] val) throws UnexpectedTypeException {
		if (val[0] instanceof Integer) {
			Array<Int> array = new Array<>(valNum);
			array.arr = new Int[LENGTH];
			array.elementType = Type.INTEGER;
			IntStream.range(0, LENGTH).forEach(i -> array.arr[i] = new Int(i));
			IntStream.range(0, LENGTH).forEach(i -> array.arr[i].setVal(val[i], -1));
			array.valueDependencies = array.initZeros(LENGTH);
			return array;
		}
		throw new UnexpectedTypeException(Object.class.toString());
	}

	private Formula[][] initVars(int length) {
		Variable[][] initialValues = new Variable[length][this.elementType.bitwidth()];
		for (int i = 0; i < length; i++) {
			Variable[] initValue = LogicUtil.createVars(this.getValNum(), this.elementType.bitwidth(), "a" + i);
			initialValues[i] = initValue;
		}
		this.vars = initialValues;
		return initialValues;
	}

	private Formula[][] initZeros(int length) {
		Formula[][] initialValues = new Formula[length][this.elementType.bitwidth()];
		IntStream.range(0, length).forEach(k -> Arrays.fill(initialValues[k], LogicUtil.ff.constant(false)));
		return initialValues;
	}

	public static Array<? extends Value> newArray(SSANewInstruction instruction, Method m, boolean initWithVars) throws UnexpectedTypeException {
		Value length = m.getValue(instruction.getUse(0));
		Type content = Type.from(instruction.getConcreteType().getArrayElementType());
		assert (length.isConstant() & length instanceof Int);
		assert (content.isPrimitive());
		return Array.newArray(content, instruction.getDef(), initWithVars);
	}

	@Override public boolean verifyType(Object val) {
		return val instanceof Array;
	}

	@Override public boolean isArrayType() {
		return true;
	}

	public int length() {
		return LENGTH;
	}

	public Formula[] boolLength() {
		return LogicUtil.asFormulaArray(LogicUtil.twosComplement(LENGTH, this.getWidth()));
	}

	public Type elementType() {
		return this.elementType;
	}

	public T access(int idx) {
		return this.arr[idx];
	}

	public void store(Object val, int idx, int recursionDepth) {
		Value dest = arr[idx];
		assert (dest.verifyType(val));
		dest.setVal(val, recursionDepth);
	}

	public void addAssignment(Formula implicitIF, int idx, Formula assignmentCond, Formula[] assignedValue) {
		this.valueDependencies[idx] = LogicUtil.ternaryOp(LogicUtil.ff.and(implicitIF, assignmentCond), assignedValue, this.valueDependencies[idx]);
	}

	public Formula[] currentlyAssigned(int idx) {
		return valueDependencies[idx];
	}

	public Formula[][] getValueDependencies() {
		return valueDependencies;
	}

	public Variable[][] getArrayVars() {
		return vars;
	}

	public void setValueDependencies(Formula[][] deps) {
		this.valueDependencies = deps;
	}

	public void setValueDependencies(int idx, Formula[] deps) {
		this.valueDependencies[idx] = deps;
	}

	@Override
	public String getValAsString() {
		String[] currentVal = new String[length()];
		IntStream.range(0, currentVal.length).forEach(i -> currentVal[i] = this.arr[i].getValAsString());
		return Arrays.toString(currentVal);
	}

	public void setVal(Object[] val, int recursionDepth) {
		assert(val.length == this.length());
		IntStream.range(0, val.length).forEach(i -> this.arr[i].setVal(val[i], recursionDepth));
	}

	@Override
	public Object[] getVal() {
		Object[] val = new Object[this.length()];
		IntStream.range(0, length()).forEach(i -> val[i] = this.arr[i].getVal());
		return val;
	}
}
