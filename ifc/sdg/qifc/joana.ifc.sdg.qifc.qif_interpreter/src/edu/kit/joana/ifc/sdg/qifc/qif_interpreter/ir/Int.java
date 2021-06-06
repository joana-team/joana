package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;

import java.util.Arrays;
import java.util.stream.IntStream;

public class Int extends Value {

	private BitLatticeValue[] constantBits;

	public Int(int valNum) {
		super(valNum);
		this.setType(Type.INTEGER);
		this.setWidth(Type.INTEGER.bitwidth());
		this.setDeps(initDeps());
		this.constantBits = new BitLatticeValue[this.getWidth()];
		Arrays.fill(this.constantBits, BitLatticeValue.UNKNOWN);
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

	@Override public boolean isEffectivelyConstant(Method m) {
		return this.isConstant() || this.constantBits != null && Arrays.stream(this.constantBits)
				.allMatch(blv -> blv != BitLatticeValue.UNKNOWN) && !BasicBlock
				.getBBlockForInstruction(m.getDef(this.getValNum()), m.getCFG()).isLoopHeader();
	}

	private Formula[] initDeps() {
		Formula[] defaultInit = new Formula[Type.INTEGER.bitwidth()];
		Arrays.fill(defaultInit, LogicUtil.ff.constant(false));
		this.setDefaultInit(true);
		return defaultInit;
	}

	@Override public Formula getDepForBit(int i) {
		return (this.constantBits[i] == BitLatticeValue.UNKNOWN) ?
				super.getDepForBit(i) :
				this.constantBits[i].asPropFormula();
	}

	@Override public Formula[] getDeps() {
		Formula[] deps = new Formula[this.getWidth()];
		IntStream.range(0, this.getWidth()).forEach(i -> deps[i] = this.getDepForBit(i));
		return deps;
	}

	@Override public boolean[] getConstantBits() {
		boolean[] constantBits = new boolean[this.getWidth()];
		IntStream.range(0, this.getWidth())
				.forEach(i -> constantBits[i] = this.constantBits[i] != BitLatticeValue.UNKNOWN);
		return constantBits;
	}
}