package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import org.logicng.formulas.Formula;

import java.util.HashMap;
import java.util.Map;

public class Int extends Value {

	// some constants to avoid creating unnecessary objects
	// all constants have valNum -1
	private static final Map<Integer, Int> constants = new HashMap<>();

	public Int(int valNum) {
		super(valNum);
		this.setType(Type.INTEGER);
		this.setWidth(Type.INTEGER.bitwidth());
	}

	@Override public boolean verifyType(Object val) {
		return val instanceof Integer;
	}

	private static Int createConstant(int n, Formula[] deps) {
			Int newConst = new Int(-1);
			newConst.setVal(n);
			newConst.setDeps(deps);
			constants.put(n, newConst);
			return newConst;
	}

	public static boolean hasConstant(int n) {
		return constants.containsKey(n);
	}

	public static Int getOrCreateConstant(int n, Formula[] deps) {
		if (!constants.containsKey(n)) {
			createConstant(n, deps);
		}
		return constants.get(n);
	}

	public static Int getConstant(int n) {
		return constants.get(n);
	}

}
