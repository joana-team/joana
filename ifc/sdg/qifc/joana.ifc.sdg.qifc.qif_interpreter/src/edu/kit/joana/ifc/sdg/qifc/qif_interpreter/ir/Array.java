package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSANewInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.util.Triple;
import org.logicng.formulas.Formula;

import java.util.Arrays;
import java.util.Stack;
import java.util.stream.IntStream;

public class Array<T extends Value> extends Value {

	private Type elementType;
	private int length;
	private T[] arr;
	private Stack<Triple<Formula, Formula, Formula[]>>[] possibleAssignments;

	public Array(int valNum) {
		super(valNum);
		this.setType(Type.ARRAY);
		this.setWidth(Type.ARRAY.bitwidth());

	}

	private Stack<Triple<Formula, Formula, Formula[]>>[] initAssignments(int length) {
		Stack<Triple<Formula, Formula, Formula[]>>[] possibleAssignments = new Stack[length];
		Formula[] initValue = new Formula[this.elementType.bitwidth()];
		Arrays.fill(initValue, LogicUtil.ff.constant(false));
		for (int i = 0; i < length; i++) {
			possibleAssignments[i] = new Stack<>();
			possibleAssignments[i]
					.push(Triple.triple(LogicUtil.ff.constant(true), LogicUtil.ff.constant(true), initValue));
		}
		return possibleAssignments;
	}

	public static Array<? extends Value> newArray(Type t, int length, int valNum) throws UnexpectedTypeException {
		if (t == Type.INTEGER) {
			Array<Int> array = new Array<>(valNum);
			array.length = length;
			array.arr = new Int[length];
			array.elementType = t;
			IntStream.range(0, length).forEach(i -> array.arr[i] = new Int(i));
			array.possibleAssignments = array.initAssignments(length);
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
		assert (dest.verifyType(val));
		dest.setVal(val, recursionDepth);
	}

	public void addAssignment(Formula implicitIF, int idx, Formula assignmentCond, Formula[] assignedValue) {
		this.possibleAssignments[idx].push(Triple.triple(implicitIF, assignmentCond, assignedValue));
	}

	public Formula[] currentlyAssigned(int idx) {
		Stack<Triple<Formula, Formula, Formula[]>> element = this.possibleAssignments[idx];
		Formula[] res = element.peek().getRight();

		for (int i = element.size() - 1; i > 0; i--) {
			res = LogicUtil.ternaryOp(LogicUtil.ff.and(element.get(i).getLeft(), element.get(i).getMiddle()),
					element.get(i).getRight(), res);
		}

		return res;
	}
}
