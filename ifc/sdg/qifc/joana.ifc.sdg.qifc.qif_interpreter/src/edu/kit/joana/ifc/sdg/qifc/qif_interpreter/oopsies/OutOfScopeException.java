package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies;

import com.ibm.wala.ssa.SSAInstruction;

public class OutOfScopeException extends Exception{

	public OutOfScopeException() {
		this("Java feature is out of scope for this interpreter. Sorry.");
	}

	public OutOfScopeException(String msg) {
		super(msg);
	}

	public OutOfScopeException(SSAInstruction outOfScopeInstruction) {
		this("SSA Instruction " + outOfScopeInstruction.toString() + " is out of scope for this interpreter. Sorry.");
	}
}
