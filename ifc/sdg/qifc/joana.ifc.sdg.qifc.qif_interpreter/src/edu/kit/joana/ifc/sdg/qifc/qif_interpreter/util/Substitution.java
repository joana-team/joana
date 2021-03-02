package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.HashMap;
import java.util.Map;

public class Substitution {

	private final Map<Variable, Formula> subst;

	public Substitution() {
		subst = new HashMap<>();
	}

	public void addMapping(Variable var, Formula f) {
		this.subst.put(var, f);
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
