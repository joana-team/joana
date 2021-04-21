package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

public class Int extends Value {

	public Int(int valNum) {
		super(valNum);
		this.setType(Type.INTEGER);
		this.setWidth(Type.INTEGER.bitwidth());
	}

	@Override public boolean verifyType(Object val) {
		return val instanceof Integer;
	}

	@Override public boolean isArrayType() {
		return false;
	}

}
