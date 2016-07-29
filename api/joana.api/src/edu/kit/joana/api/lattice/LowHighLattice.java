/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.lattice;

import java.util.Collection;
import java.util.Iterator;

import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.InvalidLatticeException;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;

import static edu.kit.joana.api.lattice.BuiltinLattices.STD_SECLEVEL_LOW;
import static edu.kit.joana.api.lattice.BuiltinLattices.STD_SECLEVEL_HIGH;

/**
 * 
 * The implementations compare lattice arguments l using l.hashCode, and if-cascaded.
 * We would use String switch here, if we had any confidence that javac wouldn't generate calls to String.equals
 * (using perfect hashing). See, e.g.,
 *   https://blogs.oracle.com/darcy/entry/project_coin_string_switch_break
 *   http://javarevisited.blogspot.de/2014/05/how-string-in-switch-works-in-java-7.html
 * 
 * We might have decided to compare using == and require clients only use String constants {@link LowHighLattice#STD_SECLEVEL_LOW} 
 * and {@link LowHighLattice#STD_SECLEVEL_HIGH}, but this might bee too unexpected and cause subtle bugs. 
 * 
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class LowHighLattice implements IStaticLattice<String> {
	public final static LowHighLattice INSTANCE = new LowHighLattice();
	
	private LowHighLattice() {
	}
	// make sure STD_SECLEVEL_LOW and STD_SECLEVEL_HIGH actually have different hash codes.
	static {
		if (STD_SECLEVEL_LOW.hashCode() == STD_SECLEVEL_HIGH.hashCode()) throw new IllegalArgumentException();
	}
	
	private final static Collection<String> elements = new Collection<String>() {

		@Override
		public int size() {	return 2; }

		@Override
		public boolean isEmpty() { return false; }

		@Override
		public boolean contains(Object o) {
			if (! (o instanceof String)) return false;
			String s = (String) o;
			return (STD_SECLEVEL_LOW.equals(s) || STD_SECLEVEL_HIGH.equals(s));
		}

		@Override
		public Iterator<String> iterator() {
			return new Iterator<String>() {
				int  at = 0;
				@Override
				public boolean hasNext() {
					return at < 2;
				}

				@Override
				public String next() {
					return (at++ == 0 ? STD_SECLEVEL_LOW : STD_SECLEVEL_HIGH);
				}
			};
		}

		@Override
		public Object[] toArray() {
			return new String[] { STD_SECLEVEL_LOW, STD_SECLEVEL_HIGH };
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T[] toArray(T[] a) {
			// String is final, so bug of, type system!!
			if (a.length >= 2) { 
				a[0] = (T) STD_SECLEVEL_LOW;
				a[1] = (T) STD_SECLEVEL_HIGH;
				return a;
			} else {
				return (T[]) new String[] { STD_SECLEVEL_LOW, STD_SECLEVEL_HIGH };
			}
		}

		@Override
		public boolean add(String e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return c.stream().allMatch( x -> x.equals(STD_SECLEVEL_LOW) || x.equals(STD_SECLEVEL_HIGH));
		}

		@Override
		public boolean addAll(Collection<? extends String> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
		
	}; 

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#greatestLowerBound(java.lang.Object, java.lang.Object)
	 */
	@Override
	public String greatestLowerBound(String s, String t) throws NotInLatticeException {
		assert ((STD_SECLEVEL_LOW.equals(s) || STD_SECLEVEL_HIGH.equals(s)) &&
		        (STD_SECLEVEL_LOW.equals(t) || STD_SECLEVEL_HIGH.equals(t)));
		if (s.hashCode() == STD_SECLEVEL_LOW.hashCode()) {
			return STD_SECLEVEL_LOW;
		} else {
			return t;
		}
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#leastUpperBound(java.lang.Object, java.lang.Object)
	 */
	@Override
	public String leastUpperBound(String s, String t) throws NotInLatticeException {
		assert ((STD_SECLEVEL_LOW.equals(s) || STD_SECLEVEL_HIGH.equals(s)) &&
		        (STD_SECLEVEL_LOW.equals(t) || STD_SECLEVEL_HIGH.equals(t)));
		if (s.hashCode() == STD_SECLEVEL_HIGH.hashCode()) {
			return STD_SECLEVEL_HIGH;
		} else {
			return t;
		}
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#getTop()
	 */
	@Override
	public String getTop() throws InvalidLatticeException {
		return STD_SECLEVEL_HIGH;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#getBottom()
	 */
	@Override
	public String getBottom() throws InvalidLatticeException {
		return STD_SECLEVEL_LOW;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#getElements()
	 */
	@Override
	public Collection<String> getElements() {
		return elements;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.lattice.IStaticLattice#isLeq(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean isLeq(String l1, String l2) {
		assert ((STD_SECLEVEL_LOW.equals(l1) || STD_SECLEVEL_HIGH.equals(l1)) &&
		        (STD_SECLEVEL_LOW.equals(l2) || STD_SECLEVEL_HIGH.equals(l2)));
		if (l1.hashCode() == STD_SECLEVEL_LOW.hashCode()) {
			return true;
		} else {
			return l1.hashCode() == l2.hashCode();
		}
	}
	
	
}
