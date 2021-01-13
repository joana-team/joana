package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

public enum Type {

	INTEGER(3),
	CUSTOM(-1);

	private final int bitWidth;

	Type(int bitWidth) {
		this.bitWidth = bitWidth;
	}

	public int bitwidth() {
		return this.bitWidth;
	}
}
