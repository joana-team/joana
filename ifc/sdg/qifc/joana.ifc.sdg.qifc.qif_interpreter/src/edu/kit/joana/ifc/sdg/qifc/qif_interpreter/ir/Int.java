package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import org.logicng.formulas.Formula;

import java.util.HashMap;
import java.util.Map;

public class Int extends Value {

	public Int(int valNum) {
		super(valNum);
		this.setType(Type.INTEGER);
		this.setWidth(Type.INTEGER.bitwidth());
	}

	@Override public boolean verifyType(Object val) {
		return val instanceof Integer;
	}

}
