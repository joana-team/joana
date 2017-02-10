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
public class ParserException extends RecognitionException {

	private static final long serialVersionUID = 7617473783402918842L;

	private String message;
	private Throwable cause;

	public ParserException(String message, Throwable cause) {
		super();
		this.message = message;
		this.cause = cause;
	}

	public ParserException(String message) {
		super();
		this.message = message;
	}

	public ParserException(Throwable cause) {
		super();
		this.cause = cause;
	}

	public ParserException() {
		super();
	}

	@Override public String getMessage() {
		return message;
	}

	@Override public Throwable getCause() {
		return cause;
	}
}
