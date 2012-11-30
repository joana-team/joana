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
 * Defines the minimal operations on graph elements required to perform lattice
 * calculations.
 *
 * @param <ElementType>
 *            the type of the elements this object implements the required
 *            relations for.
 */
public interface ILatticeOperations<ElementType> {
	/**
	 * Returns all immediately (i.e. not transitive) lower elements of a
	 * provided element.
	 *
	 * @param element
	 *            the element to return all immediately lower elements for.
	 * @return all immediately lower elements of <code>element</code>.
	 * @throws NotInLatticeException
	 *             if the relations defined by this
	 *             <code>ILatticeOperations</code> elements are not defined
	 *             for <code>element</code>.
	 */
	public Collection<ElementType> getImmediatelyLower(ElementType element) throws NotInLatticeException;

	/**
	 * Returns all immediately (i.e. not transitive) greater elements of a
	 * provided element.
	 *
	 * @param element
	 *            the element to return all immediately greater elements for.
	 * @return all immediately greater elements of <code>element</code>.
	 * @throws NotInLatticeException
	 *             if the relations defined by this
	 *             <code>ILatticeOperations</code> elements are not defined
	 *             for <code>element</code>.
	 */
	public Collection<ElementType> getImmediatelyGreater(ElementType element) throws NotInLatticeException;
}
