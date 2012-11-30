/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.io;

@SuppressWarnings("serial")
public class MethodNotFoundException extends Exception {

	private String methodName;
	private String annotation;

	public MethodNotFoundException(String methodName) {
		super();
		this.methodName = methodName;
	}

	public MethodNotFoundException(String annotation, String methodName) {
		super();
		this.methodName = methodName;
		this.annotation = annotation;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

}
