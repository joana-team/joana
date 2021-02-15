package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import org.logicng.formulas.Formula;

import java.util.Arrays;

/**
 * Subclasses represent values of a specific type.
 * Captures all necessary information about a value to analyze and run the program
 */
public abstract class Value {

	private int valNum;
	private int width;
	private Type type;
	private Object val;
	private Formula[] deps;
	private boolean leaked;
	private boolean isConstant;

	public Value(int valNum) {
		this.valNum = valNum;
		this.leaked = false;
		this.isConstant = false;
	}

	public static Value createByType(int valNum, Type type) {
		switch (type) {
		case INTEGER:
			return new Int(valNum);
		default:
			return null;
		}
	}

	public static Value createConstant(int valNum, Type type, Object value) {
		if (type == Type.INTEGER) {
			Int i = new Int(valNum);
			i.setConstant(true);
			i.setVal(value);
			return i;
		}
		throw new IllegalStateException("Unexpected value: " + type);
	}

	public Formula getDepForBit(int i) {
		checkWidth(i);
		return this.deps[i];
	}

	public void setDepforBit(int i, Formula dep) {
		checkWidth(i);
		this.deps[i] = dep;
	}

	private void checkWidth(int i) {
		if (i >= this.deps.length) {
			throw new ArrayIndexOutOfBoundsException("Invald Array Access: Value type only has width " + this.type.bitwidth());
		}
	}

	public abstract boolean verifyType(Object val);

	// --------------------------- getters and setters ---------------------------------------

	public int getValNum() {
		return valNum;
	}

	public void setValNum(int valNum) {
		this.valNum = valNum;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Object getVal() {
		return val;
	}

	public void setVal(Object val) {
		if(!verifyType(val)) {
			throw new IllegalArgumentException("Error: Wrong input parameter. Expected type " + this.type);
		}
		this.val = val;
	}

	public Formula[] getDeps() {
		return deps;
	}

	public void setDeps(Formula[] deps) {
		this.deps = deps;
	}

	@Override public String toString() {
		return String.format("%d %s %s", this.valNum, this.val.toString(), Arrays.toString(this.deps)) + ((leaked) ? " Leaked" : "");
	}

	public void leak() {
		this.leaked = true;
	}

	public boolean isConstant() {
		return isConstant;
	}

	public void setConstant(boolean constant) {
		isConstant = constant;
	}
}
