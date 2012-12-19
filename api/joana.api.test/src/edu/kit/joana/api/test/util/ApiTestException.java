/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test.util;

/**
 * @author Juergen Graf <graf@kit.edu>
 */
public class ApiTestException extends Exception {

	private static final long serialVersionUID = 835676727152993254L;

	public ApiTestException() {
		super();
	}

	public ApiTestException(String message) {
		super(message);
	}

	public ApiTestException(Throwable cause) {
		super(cause);
	}

	public ApiTestException(String message, Throwable cause) {
		super(message, cause);
	}

}
