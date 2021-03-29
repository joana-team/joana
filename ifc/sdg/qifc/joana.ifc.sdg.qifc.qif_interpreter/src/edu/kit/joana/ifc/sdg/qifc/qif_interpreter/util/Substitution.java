package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class Substitution {

	private final Map<Variable, Formula> subst;

	public Substitution() {
		subst = new HashMap<>();
	}

	public void addMapping(Variable var, Formula f) {
		this.subst.put(var, f);
	}

	public void addMapping(Formula[] var, Formula[] f) {
		assert (Arrays.stream(var).allMatch(ff -> ff instanceof Variable));
		assert (var.length == f.length);
		IntStream.range(0, var.length).forEach(i -> this.addMapping(((Variable) var[i]), f[i]));
	}

	public Formula getMapping(Variable var) {
		return subst.get(var);
	}

	public void join(Substitution substitution) {
		this.subst.putAll(substitution.subst);
	}

	public org.logicng.datastructures.Substitution toLogicNGSubstitution() {
		org.logicng.datastructures.Substitution s = new org.logicng.datastructures.Substitution();
		this.subst.forEach(s::addMapping);
		return s;
	}
}
