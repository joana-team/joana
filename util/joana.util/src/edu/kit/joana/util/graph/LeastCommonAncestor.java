/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.graph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public final class LeastCommonAncestor {
	private LeastCommonAncestor() {}
	
	public static interface PseudoTreeNode<T> {
		T getNext();
	}
	
	public static <T extends PseudoTreeNode<T>> T lca(Iterable<T> ts) {
		final Iterator<T> it = ts.iterator();
		if (!ts.iterator().hasNext()) throw new IllegalArgumentException();
		T t =  it.next();
		while (it.hasNext() && t != null) {
			t = lca(t, it.next());
		}
		return t;
	}
	
	public static <T extends PseudoTreeNode<T>> T lca(T n, T m) {
		Set<T> pin = new HashSet<>();
		pin.add(n);
		
		Set<T> pim = new HashSet<>();
		pim.add(m);
		
		while (true) {
			if (pin.contains(m)) return m;
			
			final T nn = n.getNext();
			if (nn == null) return lin(n, pin, m, pim);
			
			if (pin.contains(nn)) return lin(n, pin, m, pim);
			
			pin.add(nn);
			n = m;
			m = nn;
			
			final Set<T> tmp = pim;
			pim = pin;
			pin = tmp;
		}
	}
	
	private static <T extends PseudoTreeNode<T>> T lin(final T n, final Set<T> pin, T m, Set<T> pim) {
		while (true) {
			final T mm = m.getNext();
			if (mm == null) return null;
			if (pin.contains(mm)) return mm;
			if (pim.contains(mm)) return null;
			pim.add(mm);
			m = mm;
		}
	}
}
