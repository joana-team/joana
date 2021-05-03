package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import org.logicng.formulas.Formula;

@FunctionalInterface
public interface TernaryOperator<T> {

	public T apply(Formula cond, T x, T y);

}
