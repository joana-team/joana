/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util;

public class NotImplementedException extends RuntimeException {

	private static final long serialVersionUID = -2211946482508944247L;

	public NotImplementedException() {
		super();
	}

	public NotImplementedException(String msg) {
		super(msg);
	}

}
