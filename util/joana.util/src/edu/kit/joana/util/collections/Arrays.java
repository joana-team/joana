/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.collections;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class Arrays {
	
	public static boolean isSorted(int[] a) {
		for (int i = 1; i < a.length; i++) {
			if (a[i] < a[i-1]) return false;
		}
		return true;
	}
	
	public static boolean sortedDisjoint(int[] a, int[] b) {
		if (a == null || b == null) return true;
		if (a.length == 0 || b.length == 0) return true;

		assert isSorted(a);
		assert isSorted(b);
		
		int i = 0 ;
		int j = 0;
		
		do {
			int x;
			int y = b[j++];
			
			do {
				x = a[i++];
			} while (i < a.length && x < y);
			
			if (x == y) {
				assert !sortedDisjointSlow(a, b);
				return false;
			}
		} while (j < b.length);
		
		assert sortedDisjointSlow(a, b);
		return true;
	}
	
	private static boolean sortedDisjointSlow(int[] a, int[] b) {
		if (a == null || b == null) return true;
		final Set<Integer> sa = new HashSet<>(a.length);
		final Set<Integer> sb = new HashSet<>(b.length);
		for (int x : a) {
			sa.add(x);
		}
		for (int y : b) {
			sb.add(y);
		}
		
		return Collections.disjoint(sa, sb);
	}

}
