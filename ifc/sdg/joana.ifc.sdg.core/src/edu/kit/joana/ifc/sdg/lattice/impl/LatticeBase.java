/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.lattice.impl;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.InvalidLatticeException;


/**
 * Base class for implementations of <code>IEditableLattice</code>.
 * Pre-implements operations that can be provided regardless of the actual
 * extending class.
 *
 * @param <ElementType>
 *            the type of the elements in the lattice.
 */
public abstract class LatticeBase<ElementType> implements IEditableLattice<ElementType> {

	/**
	 * Returns the unique bottom element of the lattice.
	 *
	 * @reutrn the unique bottom element of the lattice.
	 * @throws InvalidLatticeException
	 *             if the graph does not have a unique bottom element.
	 */
	public ElementType getBottom() throws InvalidLatticeException {
		Collection<ElementType> bottoms = this.findBottomElements(getElements());
		if (bottoms.size() != 1)
			throw new InvalidLatticeException("No unique bottom element found");
		return bottoms.iterator().next();
	}

	/**
	 * Returns the unique top element of the lattice.
	 *
	 * @return the unique top element of the lattice.
	 * @throws InvalidLatticeException
	 *             if the graph does not have a unique top element.
	 */
	public ElementType getTop() throws InvalidLatticeException {
		Collection<ElementType> tops = this.findTopElements(getElements());
		if (tops.size() != 1)
			throw new InvalidLatticeException("No unique top element found");
		return tops.iterator().next();
	}
}
