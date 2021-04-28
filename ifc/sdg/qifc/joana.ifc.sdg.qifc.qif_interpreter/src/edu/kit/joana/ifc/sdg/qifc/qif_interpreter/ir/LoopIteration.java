package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import org.logicng.formulas.Formula;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * data class to represent what happens in a loop iteration.
 * The values we store are the values that the variables have after the execution of the {@code iteration}-th iteration of the loop
 */
public class LoopIteration {

	private final int iteration;
	private Formula jumpOutAfterThisIteration;
	private final Map<Integer, Formula[]> primitive;
	private final Map<Integer, Formula[][]> arr;

	public LoopIteration(int iteration) {
		this.iteration = iteration;
		this.primitive = new HashMap<>();
		this.arr = new HashMap<>();
	}

	public void addPrimitiveVal(int valNum, Formula[] deps) {
		this.primitive.put(valNum, deps);
	}

	public void addArr(int valNum, Formula[][] deps) {
		this.arr.put(valNum, deps);
	}

	public void setJumpOutAfterThisIteration(Formula jumpOutAfterThisIteration) {
		this.jumpOutAfterThisIteration = jumpOutAfterThisIteration;
	}

	public Formula[] getPrimitive(int valNum) {
		return this.primitive.get(valNum);
	}

	public Formula[][] getArray(int valNum) {
		return this.arr.get(valNum);
	}

	public int getIteration() {
		return this.iteration;
	}

	public Formula getJumpOutAfterThisIteration() {
		return this.jumpOutAfterThisIteration;
	}

	public Substitution makeSubstitution(Map<Integer, Formula[]> primitiveVars, Map<Integer, Formula[][]> arrayVars) {
		Substitution s = new Substitution();
		primitiveVars.keySet().forEach(i -> s.addMapping(primitiveVars.get(i), this.primitive.get(i)));
		arrayVars.keySet().forEach(i ->
				IntStream.range(0, arrayVars.get(i).length).forEach( j ->
						s.addMapping(arrayVars.get(i)[j], this.arr.get(i)[j])
						));
		return s;
	}

	public Map<Integer, Formula[]> getPrimitive() {
		return primitive;
	}

	public Map<Integer, Formula[][]> getArr() {
		return arr;
	}
}
