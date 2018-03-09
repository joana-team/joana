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
import java.util.HashSet;
import java.util.List;

/**
 * Defines the minimal operations on graph elements required to perform lattice
 * calculations.
 *
 * @param <ElementType>
 *            the type of the elements this object implements the required
 *            relations for.
 */
public interface ILatticeOperations<ElementType> extends IStaticLattice<ElementType> {
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
		greaterElements.add(s);
		boolean changed = false;
		do {
			changed = false;
			Collection<ElementType> toAdd = new ArrayList<ElementType>();
			for (ElementType e : greaterElements)
				for (ElementType p : getImmediatelyGreater(e))
					if (!greaterElements.contains(p)) {
						toAdd.add(p);
						changed = true;
					}
			greaterElements.addAll(toAdd);
		} while (changed);
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
		lowerElements.add(s);
		boolean changed = false;
		do {
			changed = false;
			Collection<ElementType> toAdd = new ArrayList<ElementType>();
			for (ElementType e : lowerElements)
				for (ElementType p : getImmediatelyLower(e))
					if (!lowerElements.contains(p)) {
						toAdd.add(p);
						changed = true;
					}
			lowerElements.addAll(toAdd);
		} while (changed);
		return lowerElements;
    }
    
    /**
     * Does a bottom-up sweep of the graph and returns all elements not
	 * reachable from the unique bottom. This function requires the graph to
	 * have a unique bottom element.
	 *
	 * @param inElements
	 *            the elements of the graph.
	 * @return all elements that could not be reached during the bottom-up
	 *         sweep, or <code>null</code> if all elements could be reached.
	 * @throws InvalidLatticeException
	 *             if the graph does not have a unique bottom element
     */
	default public  Collection<ElementType> findUnreachableFromBottom(Collection<ElementType> inElements) throws InvalidLatticeException {
		ArrayList<ElementType> elements = new ArrayList<ElementType>(inElements);
		boolean seen[] = new boolean[elements.size()];
		Collection<ElementType> bottoms = findBottomElements(elements);
		if (bottoms.size() != 1)
			throw new InvalidLatticeException("Graph does not have a unique bottom");
		LatticeUtil.markReachableUp(elements, bottoms.iterator().next(), seen, this);
		ArrayList<ElementType> ret = new ArrayList<ElementType>();
		for (int i = 0; i < seen.length; i++)
			if (!seen[i])
				ret.add(elements.get(i));
		if (ret.isEmpty())
			return null;
		return ret;
    }

    /**
	 * Does a top-down sweep of the graph and returns all elements not reachable
	 * from the unique top. This function requires the graph to have a unique
	 * top element.
	 *
	 * @param inElements
	 *            the elements of the graph.
	 * @return all elements that could not be reached during the top-down sweep,
	 *         or <code>null</code> if all elements could be reached.
	 * @throws InvalidLatticeException
	 *             if the graph does not have a unique top element
     */
	default public Collection<ElementType> findUnreachableFromTop(Collection<ElementType> inElements) throws InvalidLatticeException {
		ArrayList<ElementType> elements = new ArrayList<ElementType>(inElements);
		boolean seen[] = new boolean[elements.size()];
		Collection<ElementType> tops = findTopElements(elements);
		if (tops.size() != 1)
			throw new InvalidLatticeException("Graph does not have a unique top");
		LatticeUtil.markReachableDown(elements, tops.iterator().next(), seen, this);
		ArrayList<ElementType> ret = new ArrayList<ElementType>();
		for (int i = 0; i < seen.length; i++)
			if (!seen[i])
				ret.add(elements.get(i));
		if(ret.isEmpty())
			return null;
		return ret;
    }
    
    /**
	 * Finds all least upper bounds (lub) for two given elements.
	 *
	 * @param s
	 *            the first parameter for the lub operation.
	 * @param t
	 *            the second parameter for the lub operation.
	 * @return all least upper bounds of the elements <code>s</code> and
	 *         <code>t</code>
     */
	default public Collection<ElementType> leastUpperBounds(ElementType s, ElementType t) {
		assert s != null;
		assert t != null;

		// GBs = {x in elements | x >= s}
		Collection<ElementType> gbs = collectAllGreaterElements(s);

		// GBt = {x in elements | x >= t}
		Collection<ElementType> gbt = collectAllGreaterElements(t);

		// CGB = GBs intersect GBt
		List<ElementType> cgb = new ArrayList<ElementType>();
		for (ElementType a : gbs)
			if (gbt.contains(a))
				cgb.add(a);

		// return min(CLB)
		return LatticeUtil.min(cgb, this);
    }
    
    /**
	 * Finds all greatest lower bounds (glb) for two given elements.
	 *
	 * @param s
	 *            the first parameter for the glb operation.
	 * @param t
	 *            the second parameter for the glb operation.
	 * @return all greatest lower bounds of the elements <code>s</code> and
	 *         <code>t</code>
     */
	default public Collection<ElementType> greatestLowerBounds(ElementType s, ElementType t) {
		assert s != null;
		assert t != null;

		// LBs = {x in elements | x <= s}
		Collection<ElementType> lbs = collectAllLowerElements(s);

		// LBt = {x in elements | x <= t}
		Collection<ElementType> lbt = collectAllLowerElements(t);

		// CLB = LBs intersect LBt
		List<ElementType> clb = new ArrayList<ElementType>();
		for (ElementType a : lbs)
			if (lbt.contains(a))
				clb.add(a);

		// return max(CLB)
		return LatticeUtil.max(clb, this);
    }
    
    /**
	 * Finds all elements that do not have a predecessor.
	 *
	 * @param inElements
	 *            the elements of the graph.
	 * @return all elements in <code>inElements</code> that do not have a
	 *         predecessor.
     */
	default public Collection<ElementType> findTopElements(Collection<ElementType> inElements) {
		Collection<ElementType> tops = new ArrayList<ElementType>();

		for (ElementType e : inElements) {
			if (getImmediatelyGreater(e).size() == 0)
				tops.add(e);
		}
		return tops;
    }
    
    /**
	 * Finds all elements that do not have a successor.
	 *
	 * @param inElements
	 *            the elements of the graph.
	 * @return all elements in <code>inElements</code> that do not have a
	 *         successor.
     */
	default public Collection<ElementType> findBottomElements(Collection<ElementType> inElements) {
		Collection<ElementType> bottoms = new ArrayList<ElementType>();

		for (ElementType e : inElements) {
			if (getImmediatelyLower(e).size() == 0)
				bottoms.add(e);
		}
		return bottoms;
    }
}
