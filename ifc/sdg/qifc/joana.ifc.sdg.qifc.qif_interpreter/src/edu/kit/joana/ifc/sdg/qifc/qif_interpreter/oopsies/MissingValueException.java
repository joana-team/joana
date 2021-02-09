package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies;

import com.ibm.wala.ssa.SSAInstruction;

public class MissingValueException extends Exception {

	public MissingValueException() {
		this("Error: Value not found");
	}

	public MissingValueException(String msg) {
		super(msg);
	}

	public MissingValueException(SSAInstruction i, int use) {
		this(String.format("Error: Couldn't find value: Value number %d for instruction %s", use, i.toString()));
	}

	public MissingValueException(int valNum) {
		this(String.format("Error: Couldn't find value: Value number %d", valNum));
	}

}
