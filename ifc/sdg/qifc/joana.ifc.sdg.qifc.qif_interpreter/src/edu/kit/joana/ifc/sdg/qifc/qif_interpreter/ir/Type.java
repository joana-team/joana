package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.types.TypeReference;
import nildumu.typing.Types;

import java.util.Collections;

public enum Type {

	INTEGER(16), ARRAY(3), CUSTOM(-1);

	private final int bitWidth;
	public static final Types nTypes = new Types();

	Type(int bitWidth) {
		this.bitWidth = bitWidth;
	}

	public static Type from(TypeReference paramType) {
		if (paramType.equals(TypeReference.Int)) {
			return INTEGER;
		} else if (paramType.isArrayType()) {
			return ARRAY;
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

	public boolean isPrimitive() {
		if (this == INTEGER) {
			return true;
		}
		return false;
	}

	public nildumu.typing.Type nildumuType() {
		switch (this) {
		case INTEGER:
			return nTypes.INT;
		case ARRAY:
			return nTypes.getOrCreateFixedArrayType(nTypes.INT, Collections.singletonList(INTEGER.bitWidth));
		case CUSTOM:
			return nTypes.VAR;
		}
		return null;
	}
}