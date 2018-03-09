/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.lattice;

import java.util.Collection;
import java.util.HashSet;

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

    /**
     * Transitive greater elements of a given lattice element.
	 *
	 * @param s
	 *            the element for which to collect all greater elements.
	 *
	 * @return all transitive greater elements of <code>s</code>
     */
	default public Collection<ElementType> collectAllGreaterElements(ElementType s) {
		Collection<ElementType> greaterElements = new HashSet<ElementType>();

		for (ElementType e : getElements()) {

			if (greatestLowerBound(s, e).equals(s)) {
				// inf(s,e) = s ==> s <= e
				greaterElements.add(e);
			}
		}

		return greaterElements;
    }

    /**
     * Transitive lower elements of a given lattice element.
	 *
	 * @param s
	 *            the element for which to collect all lower elements.
	 *
	 * @return all transitive lower elements of <code>s</code>
     */
	default public Collection<ElementType> collectAllLowerElements(ElementType s) {
		Collection<ElementType> lowerElements = new HashSet<ElementType>();

		for (ElementType e : getElements()) {
			if (leastUpperBound(s, e).equals(s)) {
				lowerElements.add(e);
			}
		}

		return lowerElements;
    }

	/**
     * Collects all elements that must not interfere with a given lattice element,
     * i.e. are the elements for which an attacker of the given level must not learn anything.
     * These are exactly the elements that are greater than or incomparable to the given element.
	 *
	 * @param s
	 *            the element for which to collect all non-interfering elements.
	 *
	 * @return all non-interfering of <code>s</code>
     */
	default public Collection<ElementType> collectNoninterferingElements(ElementType s) {
		Collection<ElementType> noninterferingElements = new HashSet<ElementType>();

		for (ElementType e : getElements()) {
			if (!leastUpperBound(s, e).equals(s)) {
				noninterferingElements.add(e);
			}
		}

		return noninterferingElements;
    }

    /**
	 * Given a lattice l and two elements l1 and l2 or l's carrier set, returns whether l1
	 * is lower than or equal to l2. Note that l1 and l2 really have to be elements of l's carrier
	 * set, otherwise the correct functionality of this method cannot be guaranteed.
	 * @param l1 an element of the given lattice
	 * @param l2 another element of the given lattice
	 * @return {@code true} if the first given element is lower than or equal to the second given
	 * element.
	 */
	default public boolean isLeq(ElementType l1, ElementType l2) {
		return leastUpperBound(l1, l2).equals(l2);
	}

}
