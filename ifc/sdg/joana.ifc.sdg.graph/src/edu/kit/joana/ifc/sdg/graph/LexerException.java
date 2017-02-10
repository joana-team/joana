/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;

import org.antlr.runtime.RecognitionException;

/**
 * @author Simon Bischof <simon.bischof@kit.edu>
 */
public class LexerException extends RecognitionException {

	private static final long serialVersionUID = 2895739359870679267L;

	private String message;
	private Throwable cause;

	public LexerException(String message, Throwable cause) {
		super();
		this.message = message;
		this.cause = cause;
	}

	public LexerException(String message) {
		super();
		this.message = message;
	}

	public LexerException(Throwable cause) {
		super();
		this.cause = cause;
	}

	public LexerException() {
		super();
	}

	@Override public String getMessage() {
		return message;
	}

	@Override public Throwable getCause() {
		return cause;
	}
}
