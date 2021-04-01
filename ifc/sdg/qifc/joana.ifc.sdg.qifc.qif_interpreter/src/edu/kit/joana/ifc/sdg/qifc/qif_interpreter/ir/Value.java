package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.Stack;

/**
 * Subclasses represent values of a specific type.
 * Captures all necessary information about a value to analyze and run the program
 */
public abstract class Value {

	private int valNum;
	private int width;
	private Type type;
	private final Stack<Object> val;
	private boolean modified;
	private Formula[] deps;
	private Variable[] vars;
	private boolean leaked;
	private boolean isConstant;

	public Value(int valNum) {
		this.valNum = valNum;
		this.leaked = false;
		this.isConstant = false;
		this.val = new Stack<>();
		this.modified = false;
	}

	public void addVars(Variable[] vars) {
		assert (vars.length == width);
		this.vars = vars;
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
		return val.peek();
	}

	public boolean assigned() {
		return !(val == null);
	}

	/**
	 * contract: in each function call, at most 1 element can be added to an object's value stack
	 * if the value is already marked as modified, we need to pop a value, before we can add the new one
	 * @param val
	 */
	public void setVal(Object val) {
		if (!verifyType(val)) {
			throw new IllegalArgumentException("Error: Wrong input parameter. Expected type " + this.type);
		}

		if (modified) {
			this.val.pop();
		}

		this.val.push(val);

		// if the value is a constant, we dont mark it as modified
		// as this value doesnt change, we also dont need to reset it after a function call is finished
		if(!isConstant) {
			this.modified = true;
		}
	}

	/**
	 * clean up after function call is finished
	 */
	public void resetValue() {
		if (modified) {
			this.val.pop();
			modified = false;
		}
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

	public boolean isLeaked() {
		return leaked;
	}

	public Variable[] getVars() {
		return this.vars;
	}
}
