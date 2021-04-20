package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

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
}
