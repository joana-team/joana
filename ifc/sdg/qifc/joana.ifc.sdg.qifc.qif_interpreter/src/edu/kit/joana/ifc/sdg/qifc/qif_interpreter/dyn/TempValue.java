package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;

import java.util.stream.IntStream;

public class TempValue {

	int valNum;
	Formula[] tempVars;
	Formula[] real;
	Formula valueRestriction;

	public TempValue(int valNum, Method m, Formula[] deps) {
		this.valNum = valNum;
		this.tempVars = m.getDepsForValue(valNum);
		this.real = deps;
	}

	public void setReal(Formula[] deps) {
		this.real = deps;
	}

	public Formula asFormula() {
		return IntStream.range(0, tempVars.length).mapToObj(i -> LogicUtil.ff.equivalence(tempVars[i], real[i]))
				.reduce(valueRestriction, LogicUtil.ff::and);
	}

}