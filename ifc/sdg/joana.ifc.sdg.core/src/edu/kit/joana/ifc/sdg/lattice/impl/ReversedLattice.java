/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.lattice.impl;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.lattice.ILatticeOperations;
import edu.kit.joana.ifc.sdg.lattice.InvalidLatticeException;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class ReversedLattice<ElementType> implements ILatticeOperations<ElementType> {

	private final ILatticeOperations<ElementType> lattice;
	/**
	 * 
	 */
	public ReversedLattice(ILatticeOperations<ElementType> lattice) {
		this.lattice = lattice;
	}
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#greatestLowerBound(java.lang.Object, java.lang.Object)
	 */
	@Override
	public ElementType greatestLowerBound(ElementType s, ElementType t) throws NotInLatticeException {
		return lattice.leastUpperBound(s, t);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#leastUpperBound(java.lang.Object, java.lang.Object)
	 */
	@Override
	public ElementType leastUpperBound(ElementType s, ElementType t) throws NotInLatticeException {
		return lattice.greatestLowerBound(s, t);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#getTop()
	 */
	@Override
	public ElementType getTop() throws InvalidLatticeException {
		return lattice.getBottom();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#getBottom()
	 */
	@Override
	public ElementType getBottom() throws InvalidLatticeException {
		return lattice.getTop();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#getElements()
	 */
	@Override
	public Collection<ElementType> getElements() {
		return lattice.getElements();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.ILatticeOperations#getImmediatelyLower(java.lang.Object)
	 */
	@Override
	public Collection<ElementType> getImmediatelyLower(ElementType element) throws NotInLatticeException {
		return lattice.getImmediatelyGreater(element);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.ILatticeOperations#getImmediatelyGreater(java.lang.Object)
	 */
	@Override
	public Collection<ElementType> getImmediatelyGreater(ElementType element) throws NotInLatticeException {
		return lattice.getImmediatelyLower(element);
	}
	

}
