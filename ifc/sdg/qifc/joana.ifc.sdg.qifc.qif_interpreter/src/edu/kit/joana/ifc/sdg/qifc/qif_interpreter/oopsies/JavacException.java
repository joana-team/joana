package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies;

public class JavacException extends Exception {

	public JavacException() {
		this("");
	}

	public JavacException(String msg) {
		super("Error while compiling input program.\n" + msg);
	}

}
