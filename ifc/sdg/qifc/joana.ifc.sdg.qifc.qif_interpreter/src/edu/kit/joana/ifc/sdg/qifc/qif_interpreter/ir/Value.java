package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import org.logicng.formulas.Formula;

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

	public Value(int valNum) {
		this.valNum = valNum;
	}

	public static Value createByType(int valNum, Type type) {
		switch(type) {
		case INTEGER:
			return new Int(valNum);
		default:
			return null;
		}
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
		this.val = val;
	}

	public Formula[] getDeps() {
		return deps;
	}

	public void setDeps(Formula[] deps) {
		this.deps = deps;
	}
}
