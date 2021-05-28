package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;

import java.util.Arrays;

public class Int extends Value {

	private BitLatticeValue[] constantBits;

	public Int(int valNum) {
		super(valNum);
		this.setType(Type.INTEGER);
		this.setWidth(Type.INTEGER.bitwidth());
		this.setDeps(initDeps());
	}

	@Override public boolean verifyType(Object val) {
		return val instanceof Integer;
	}

	@Override public boolean isArrayType() {
		return false;
	}

	@Override public String getValAsString() {
		return String.valueOf(this.getVal());
	}

	@Override public void setConstantBitMask(BitLatticeValue[] constantBits) {
		this.constantBits = constantBits;
	}

	@Override public BitLatticeValue[] getConstantBitMask() {
		return this.constantBits;
	}

	@Override public boolean isEffectivelyConstant() {
		return this.constantBits != null && Arrays.stream(this.constantBits)
				.allMatch(blv -> blv != BitLatticeValue.UNKNOWN);
	}

	private Formula[] initDeps() {
		Formula[] defaultInit = new Formula[Type.INTEGER.bitwidth()];
		Arrays.fill(defaultInit, LogicUtil.ff.constant(false));
		return defaultInit;
	}

}