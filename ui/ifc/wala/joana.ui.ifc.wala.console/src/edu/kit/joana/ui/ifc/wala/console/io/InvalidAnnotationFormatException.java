/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.io;

@SuppressWarnings("serial")
public class InvalidAnnotationFormatException extends Exception {

	private String invalidAnnotation;

	public InvalidAnnotationFormatException() {
		super();
	}

	public InvalidAnnotationFormatException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidAnnotationFormatException(String invalidAnnotation) {
		super();
		this.invalidAnnotation = invalidAnnotation;
	}

	public InvalidAnnotationFormatException(Throwable cause) {
		super(cause);
	}

	public String getInvalidAnnotation() {
		return invalidAnnotation;
	}
}
