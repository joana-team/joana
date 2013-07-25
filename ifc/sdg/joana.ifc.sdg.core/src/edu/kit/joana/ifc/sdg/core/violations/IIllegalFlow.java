/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.violations;


/**
 * An illegal flow is a violation in which information flows from a source to a sink.
 * @author Martin Mohr
 */
public interface IIllegalFlow<T> extends IViolation<T> {
	T getSource();
	T getSink();
	String getAttackerLevel();
}