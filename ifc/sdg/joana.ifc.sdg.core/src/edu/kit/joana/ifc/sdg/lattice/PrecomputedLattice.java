/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.lattice;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Table;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class PrecomputedLattice implements IStaticLattice<String> {

	private final Set<String> elements;
	private final Table<String, String, String> glb;
	private final Table<String, String, String> lub;
	private final String top;
	private final String bottom;
	
	/**
	 * 
	 */
	public <ElementType> PrecomputedLattice(IStaticLattice<ElementType> lattice) {
		final Collection<ElementType> latticeElements = lattice.getElements();
		
		this.elements = latticeElements.stream().map(Object::toString).collect(Collectors.toSet());
		if (this.elements.size() != latticeElements.size()) throw new IllegalArgumentException("Lattice element Names are not unique");
		
		final Builder<String, String, String> glbBuilder = new ImmutableTable.Builder<>();
		final Builder<String, String, String> lubBuilder = new ImmutableTable.Builder<>();
		for (ElementType e1 : latticeElements) {
			for (ElementType e2 : latticeElements) {
				glbBuilder.put(e1.toString(), e2.toString(), lattice.greatestLowerBound(e1, e2).toString());
				lubBuilder.put(e1.toString(), e2.toString(), lattice.leastUpperBound(e1, e2).toString());
			}
		}
		
		this.glb = glbBuilder.build();
		this.lub = lubBuilder.build();
		this.top    = lattice.getTop().toString();
		this.bottom = lattice.getTop().toString();
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#greatestLowerBound(java.lang.Object, java.lang.Object)
	 */
	@Override
	public String greatestLowerBound(String s, String t) throws NotInLatticeException {
		return glb.get(s, t);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#leastUpperBound(java.lang.Object, java.lang.Object)
	 */
	@Override
	public String leastUpperBound(String s, String t) throws NotInLatticeException {
		return lub.get(s, t);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#getTop()
	 */
	@Override
	public String getTop() throws InvalidLatticeException {
		return top;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#getBottom()
	 */
	@Override
	public String getBottom() throws InvalidLatticeException {
		return bottom;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#getElements()
	 */
	@Override
	public Collection<String> getElements() {
		return Collections.unmodifiableSet(this.elements);
	}
	

}
