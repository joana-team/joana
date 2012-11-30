/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.io;

public class MessageParseException extends Exception {

	private static final long serialVersionUID = 8651097837868881289L;

	public MessageParseException(String msg) {
		super(msg);
	}

	public MessageParseException(Throwable cause) {
		super(cause);
	}

}
