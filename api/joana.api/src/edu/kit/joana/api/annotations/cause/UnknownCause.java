/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations.cause;

import edu.kit.joana.api.SPos;

/**
 * TODO: @author Add your name here.
 */
public class UnknownCause implements AnnotationCause {
	public static final UnknownCause INSTANCE = new UnknownCause();
	public String toString() {
		return "unknown";
	};

	@Override
	public SPos getSourcePosition() {
		return null;
	}
}
