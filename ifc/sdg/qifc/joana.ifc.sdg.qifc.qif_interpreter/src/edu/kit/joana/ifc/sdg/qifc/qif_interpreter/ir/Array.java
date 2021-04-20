package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSANewInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;

import java.util.stream.IntStream;

public class Array<T extends Value> extends Value {

	private Type elementType;
	private int length;
	private T[] arr;

	public Array(int valNum) {
		super(valNum);
		this.setType(Type.ARRAY);
		this.setWidth(Type.ARRAY.bitwidth());
	}

	public static Array<? extends Value> newArray(Type t, int length, int valNum) throws UnexpectedTypeException {
		if (t == Type.INTEGER) {
			Array<Int> array = new Array<Int>(valNum);
			array.length = length;
			array.arr = new Int[length];
			array.elementType = t;
			IntStream.range(0, length).forEach(i -> array.arr[i] = new Int(i));
			return array;
		}
		throw new UnexpectedTypeException(t);
	}

	public static Array<? extends Value> newArray(SSANewInstruction instruction, Method m) throws UnexpectedTypeException {
		Value length = m.getValue(instruction.getUse(0));
		Type content = Type.from(instruction.getConcreteType().getArrayElementType());
		assert (length.isConstant() & length instanceof Int);
		assert (content.isPrimitive());
		return Array.newArray(content, (Integer) length.getVal(), instruction.getDef());
	}


	@Override public boolean verifyType(Object val) {
		return val instanceof Array;
	}

	public int length() {
		return this.length;
	}

	public Formula[] boolLength() {
		return LogicUtil.asFormulaArray(LogicUtil.twosComplement(length, this.getWidth()));
	}

	public Type elementType() {
		return this.elementType;
	}

	public T access(int idx) {
		return this.arr[idx];
	}

	public void store(Object val, int idx, int recursionDepth) {
		Value dest = arr[idx];
		assert(dest.verifyType(val));
		dest.setVal(val, recursionDepth);
	}
}
