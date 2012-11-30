/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.lattice;

import java.util.Collection;

/**
 * Basic operations available on a immutable lattices.
 *
 * @param <ElementType>
 *            the type of the elements contained in the lattice.
 */
public interface IStaticLattice<ElementType> {
	/**
	 * Calculates the unique greatest lower bound (glb) of the elements in the
	 * lattice, aka infimum or meet.
	 *
	 * @param s
	 *            the first parameter of the glb operation.
	 * @param t
	 *            the second parameter of the glb operation.
	 * @return the glb of <code>s</code> and </code>t</code>
	 * @throws NotInLatticeException
	 *             ff either <code>s</code> or <code>t</code> are not in the
	 *             lattice.
	 */
	public ElementType greatestLowerBound(ElementType s, ElementType t) throws NotInLatticeException;

	/**
	 * Calculates the unique least upper bound (lub) of the elements in the
	 * lattice, aka supremum or join.
	 *
	 * @param s
	 *            the first parameter of the lub operation.
	 * @param t
	 *            the second parameter of the lub operation.
	 * @return the lub of <code>s</code> and </code>t</code>
	 * @throws NotInLatticeException
	 *             ff either <code>s</code> or <code>t</code> are not in the
	 *             lattice.
	 */
	public ElementType leastUpperBound(ElementType s, ElementType t) throws NotInLatticeException;

	/**
	 * Returns the unique top element of the lattice.
	 *
	 * @return the unique top element of the lattice.
	 *
	 * @throws InvalidLatticeException
	 *             if the graph does not have a unique top element.
	 */
	public ElementType getTop() throws InvalidLatticeException;

	/**
	 * Returns the unique bottom element of the lattice.
	 *
	 * @return the unique bottom element of the lattice.
	 *
	 * @throws InvalidLatticeException
	 *             if the graph does not have a unique bottom element.
	 */
	public ElementType getBottom() throws InvalidLatticeException;

    /**
     * Returns all elements contained in the lattice.
     *
     * @return all element contained in the lattice.
     */
    public Collection<ElementType> getElements();
}
