package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.types.TypeReference;

public enum Type {

	INTEGER(3),
	CUSTOM(-1);

	private final int bitWidth;

	Type(int bitWidth) {
		this.bitWidth = bitWidth;
	}

	public static Type from(TypeReference paramType) {
		if(paramType.equals(TypeReference.Int)) {
			return INTEGER;
		}
		return CUSTOM;
	}

	public int bitwidth() {
		return this.bitWidth;
	}

	public static Type getResultType(IBinaryOpInstruction.Operator op, Type fst, Type snd) {
		if (fst == INTEGER && snd == INTEGER) {
			return INTEGER;
		}
		throw new IllegalStateException("Instruction not typeable");
	}

	public static Type getResultType(IUnaryOpInstruction.Operator op, Type use) {
		return use;
	}
}
