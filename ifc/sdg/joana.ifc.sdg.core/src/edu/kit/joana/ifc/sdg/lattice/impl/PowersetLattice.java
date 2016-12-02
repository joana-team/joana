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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import edu.kit.joana.ifc.sdg.lattice.ILatticeOperations;
import edu.kit.joana.ifc.sdg.lattice.InvalidLatticeException;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;

/**
 * @author Martin Hecker <martin.hecket@kit.edu>
 */
public class PowersetLattice<ElementType> implements ILatticeOperations<Set<ElementType>> {

	
	private Set<Set<ElementType>> powerset;
	
	private final Set<ElementType> bottom;
	private final Set<ElementType> top;
	
	/**
	 * 
	 */
	public PowersetLattice(Set<ElementType> top) {
		this.top = new HashSet<>(top);
		this.bottom = Collections.emptySet();
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.ILatticeOperations#getImmediatelyGreater(java.lang.Object)
	 */
	@Override
	public Collection<Set<ElementType>> getImmediatelyGreater(Set<ElementType> element) throws NotInLatticeException {
		if (!top.containsAll(element)) throw new NotInLatticeException("not: " + element + " ⊆ " + top);
		
		final Set<ElementType> missing = Sets.difference(top, element);
		return missing.stream().map(
			x -> {
				Set<ElementType> g = new HashSet<>(element);
				g.add(x);
				return g;
			}
		).collect(Collectors.toCollection(() -> new ArrayList<>(missing.size())));
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.ILatticeOperations#getImmediatelyLower(java.lang.Object)
	 */
	@Override
	public Collection<Set<ElementType>> getImmediatelyLower(Set<ElementType> element) throws NotInLatticeException {
		if (!top.containsAll(element)) throw new NotInLatticeException("not: " + element + " ⊆ " + top);
		
		return element.stream().map(
			x -> {
				Set<ElementType> g = new HashSet<>(element);
				g.remove(x);
				return g;
			}
		).collect(Collectors.toCollection(() -> new ArrayList<>(element.size())));
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#getTop()
	 */
	@Override
	public Set<ElementType> getTop() throws InvalidLatticeException {
		return top;
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#getBottom()
	 */
	@Override
	public Set<ElementType> getBottom() throws InvalidLatticeException {
		return bottom;
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#getElements()
	 */
	@Override
	public Collection<Set<ElementType>> getElements() {
		if (powerset == null) {
			powerset = Sets.powerSet(top);
		}
		return powerset;
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#greatestLowerBound(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Set<ElementType> greatestLowerBound(Set<ElementType> s, Set<ElementType> t) throws NotInLatticeException {
		return Sets.intersection(s, t);
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#leastUpperBound(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Set<ElementType> leastUpperBound(Set<ElementType> s, Set<ElementType> t) throws NotInLatticeException {
		return Sets.union(s, t);
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.ILatticeOperations#collectAllGreaterElements(java.lang.Object)
	 */
	@Override
	public Collection<Set<ElementType>> collectAllGreaterElements(Set<ElementType> s) {
		final Set<ElementType> missing = Sets.difference(top, s);
		return Sets.powerSet(missing)
		           .stream()
		           .map( t -> Sets.union(s, t))
		           .collect(Collectors.toList());
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#collectAllLowerElements(java.lang.Object)
	 */
	@Override
	public Collection<Set<ElementType>> collectAllLowerElements(Set<ElementType> s) {
		return Sets.powerSet(s);
	}
}
