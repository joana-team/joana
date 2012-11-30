/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.lattice;

/**
 * An exception thrown if a operation on a lattice could not be performed
 * because certain required conditions were not met.
 *
 */
public class InvalidLatticeException extends RuntimeException {
	private static final long serialVersionUID = -8436368114035844572L;

	/**
	 * Constructor
	 *
	 * @param message
	 *            a message describing the failed precondition.
	 */
	public InvalidLatticeException(String message) {
		super(message);
	}
}
