/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.violations;


/**
 * Common interface for all security violations which JOANA can find.
 * @author Martin Mohr
 */
public interface IViolation {
	public void accept(IViolationVisitor v);
}
