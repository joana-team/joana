/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.server;

public class ServerException extends Exception {

	private static final long serialVersionUID = 641750433405534652L;

	protected ServerException(String msg) {
		super(msg);
	}

	protected ServerException(Throwable cause) {
		super(cause);
	}

}
