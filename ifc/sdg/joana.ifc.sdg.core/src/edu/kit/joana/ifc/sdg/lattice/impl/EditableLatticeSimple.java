/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.lattice.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.InvalidLatticeException;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;


/**
 * Simple implementation of <code>IEditableLattice</code> using adjacency
 * lists.
 *
 * @param <ElementType>
 *            the type of the elements contained in the lattice.
 */
public class EditableLatticeSimple<ElementType> extends LatticeBase<ElementType> implements IEditableLattice<ElementType> {
	private final List<ElementType> elements = new ArrayList<ElementType>();
	private final Map<ElementType, Collection<ElementType>> lower = new HashMap<ElementType, Collection<ElementType>>();
	private final Map<ElementType, Collection<ElementType>> greater = new HashMap<ElementType, Collection<ElementType>>();

	/**
	 * Constructor
	 */
	public EditableLatticeSimple() { }

	/**
	 * Constructor
	 *
	 * @param elements
	 *            the initial elements in the lattice
	 */
	public EditableLatticeSimple(Collection<ElementType> elements) {
		assert elements != null;
		for (ElementType e : elements)
			if (!this.elements.contains(e))
				this.elements.add(e);
	}

	public void addElement(ElementType element) {
		assert element != null;
		if (!elements.contains(element))
			elements.add(element);
	}

	public Collection<ElementType> getElements() {
		return elements;
	}

	public void removeElement(ElementType element) throws NotInLatticeException {
		assert element != null;
		if (!elements.contains(element))
			throw new NotInLatticeException("Element '" + element.toString() + "' is not in lattice");
		elements.remove(element);

		greater.remove(element);
		lower.remove(element);

		for (ElementType e : elements) {
			Collection<ElementType> greaterElements = greater.get(e);
			if (greaterElements != null) {
				greaterElements.remove(element);
				if (greaterElements.isEmpty())
					greater.remove(e);
			}
			Collection<ElementType> lowerElements = lower.get(e);
			if (lowerElements != null) {
				lowerElements.remove(element);
				if (lowerElements.isEmpty())
					lower.remove(e);
			}
		}
	}

	public void setImmediatelyGreater(ElementType element, ElementType greater) throws NotInLatticeException {
		assert element != null;
		assert greater != null;
		if (!elements.contains(element))
			throw new NotInLatticeException("Element '" + element.toString() + "' is not in lattice");
		if (!elements.contains(greater))
			throw new NotInLatticeException("Element '" + greater.toString() + "' is not in lattice");

		Collection<ElementType> greaterElements = this.greater.get(element);
		if (greaterElements == null)
			this.greater.put(element, greaterElements = new ArrayList<ElementType>());
		if (!greaterElements.contains(greater))
			greaterElements.add(greater);

		Collection<ElementType> lowerElements = this.lower.get(greater);
		if (lowerElements == null)
			this.lower.put(greater, lowerElements = new ArrayList<ElementType>());
		if (!lowerElements.contains(element))
			lowerElements.add(element);
	}

	public void setImmediatelyLower(ElementType element, ElementType lower) throws NotInLatticeException {
		setImmediatelyGreater(lower, element);
	}

	public void unsetImmediatelyGreater(ElementType element, ElementType greater) throws NotInLatticeException {
		assert element != null;
		assert greater != null;
		if (!elements.contains(element))
			throw new NotInLatticeException("Element '" + element.toString() + "' is not in lattice");
		if (!elements.contains(greater))
			throw new NotInLatticeException("Element '" + greater.toString() + "' is not in lattice");

		Collection<ElementType> greaterElements = this.greater.get(element);
		if (greaterElements != null) {
			greaterElements.remove(greater);
			if (greaterElements.isEmpty())
				this.greater.remove(element);
		}

		Collection<ElementType> lowerElements = this.lower.get(greater);
		if (lowerElements != null) {
			lowerElements.remove(element);
			if (lowerElements.isEmpty())
				this.lower.remove(greater);
		}
	}

	public void unsetImmediatelyLower(ElementType element, ElementType lower) throws NotInLatticeException {
		unsetImmediatelyGreater(lower, element);
	}

	public Collection<ElementType> getImmediatelyGreater(ElementType element) throws NotInLatticeException {
		assert element != null;
		if (!elements.contains(element))
			throw new NotInLatticeException("Element '" + element.toString() + "' is not in lattice");
		Collection<ElementType> ret = greater.get(element);
		if (ret == null)
			return new ArrayList<ElementType>();
		return ret;
	}

	public Collection<ElementType> getImmediatelyLower(ElementType element) throws NotInLatticeException {
		assert element != null;
		if (!elements.contains(element))
			throw new NotInLatticeException("Element '" + element.toString() + "' is not in lattice");
		Collection<ElementType> ret = lower.get(element);
		if (ret == null)
			return new ArrayList<ElementType>();
		return ret;
	}

	public ElementType greatestLowerBound(ElementType s, ElementType t) {
		assert s != null;
		assert t != null;
		assert elements.contains(s);
		assert elements.contains(t);
		@SuppressWarnings("deprecation")
		Collection<ElementType> greatestLowerBounds = LatticeUtil.greatestLowerBounds(s, t, this);
		if (greatestLowerBounds.size() != 1)
			throw new InvalidLatticeException("Graph is not a lattice");
		return greatestLowerBounds.iterator().next();
	}

	public ElementType leastUpperBound(ElementType s, ElementType t) {
		assert s != null;
		assert t != null;
		assert elements.contains(s);
		assert elements.contains(t);
		@SuppressWarnings("deprecation")
		Collection<ElementType> leastUpperBounds = LatticeUtil.leastUpperBounds(s, t, this);
		if (leastUpperBounds.size() != 1)
			throw new InvalidLatticeException("Graph is not a lattice");
		return leastUpperBounds.iterator().next();
	}
}
