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
		boolean onPath(Object o);
		void addToPath(Object o);
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
		if (n == m) return n;
		
		Object pathN = new Object();
		Object pathM = new Object();
		
		Set<T> pin = new HashSet<>();
		assert pin.add(n);
		n.addToPath(pathN);
		
		Set<T> pim = new HashSet<>();
		assert pim.add(m);
		m.addToPath(pathM);
		
		while (true) {
			assert (pin.contains(m) == m.onPath(pathN));
			assert (!m.onPath(pathN));
			
			final T nn = n.getNext();
			if (nn == null) return lin(pathN, pathM, n, pin, m, pim);
			
			assert (pin.contains(nn) == nn.onPath(pathN));
			if (nn.onPath(pathN)) return lin(pathN, pathM, n, pin, m, pim);
			
			assert (pim.contains(nn) == nn.onPath(pathM));
			if (nn.onPath(pathM)) return nn;
			
			assert pin.add(nn);
			nn.addToPath(pathN);
			n = m;
			m = nn;
			
			{
				final Set<T> tmp = pim;
				pim = pin;
				pin = tmp;
			}
			
			{
				final Object tmp = pathM;
				pathM = pathN;
				pathN = tmp;
			}
			
			
		}
	}
	
	private static <T extends PseudoTreeNode<T>> T lin(final Object pathN, final Object pathM, final T n, final Set<T> pin, T m, Set<T> pim) {
		while (true) {
			final T mm = m.getNext();
			if (mm == null) return null;
			
			assert (pin.contains(mm) == mm.onPath(pathN));
			if (mm.onPath(pathN)) return mm;
			
			assert (pim.contains(mm) == mm.onPath(pathM)); 
			if (mm.onPath(pathM)) return null;
			
			assert pim.add(mm);
			mm.addToPath(pathM);
			
			m = mm;
		}
	}
}
