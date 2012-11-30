/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 24.05.2004
 *
 */
package edu.kit.joana.ifc.sdg.lattice;

/**
 * NotInLatticeException.java indicates that an Object used as argument for a
 * lattice operation actually isn't there
 */
public class NotInLatticeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	String description = "";

	/**
	 * Constructor
	 * @param description the description of the problem
	 */
	public NotInLatticeException(String description) {
		this.description = description;
	}

	public String toString() {
		return description;
	}
}
