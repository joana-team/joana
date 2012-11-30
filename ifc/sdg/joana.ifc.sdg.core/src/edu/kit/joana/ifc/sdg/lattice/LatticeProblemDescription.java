/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.lattice;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Container for describing a problem found during lattice validation. Provides
 * storage for a message describing the problem and a <code>Collection</code>
 * of lattice elements providing a problem locating aid for the user.
 *
 * @param <ElementType>
 *            the type of the elements contained the validated lattice.
 */
public class LatticeProblemDescription<ElementType> {

	/**
	 * Constructor
	 *
	 * @param message
	 *            a message describing the validation problem.
	 */
	public LatticeProblemDescription(String message) {
		assert message != null;
		this.message = message;
	}

	/**
	 * Constructor
	 *
	 * @param message
	 *            a message describing the validation problem.
	 * @param involvedNodes
	 *            a <code>Collection</code> of nodes involved in the problem.
	 */
	public LatticeProblemDescription(String message, Collection<ElementType> involvedNodes) {
		assert message != null;
		assert involvedNodes != null;
		this.message = message;
		this.involvedNodes = involvedNodes;
	}

	/**
	 * Constructor
	 *
	 * @param message
	 *            a message describing the validation problem.
	 * @param involvedNode
	 *            a node involved in the problem.
	 */
	public LatticeProblemDescription(String message, ElementType involvedNode) {
		this.message = message;
		this.involvedNodes = new ArrayList<ElementType>();
		involvedNodes.add(involvedNode);
	}

	/**
	 * A message describing the validation problem.
	 */
	public String message;

	/**
	 * A <code>Collection</code> of elements involved in the problem, or
	 * <code>null</code>.
	 */
	public Collection<ElementType> involvedNodes;

	@Override
	public String toString() {
		if (involvedNodes != null)
			return message + ": " + involvedNodes;
		return message;
	}
}
