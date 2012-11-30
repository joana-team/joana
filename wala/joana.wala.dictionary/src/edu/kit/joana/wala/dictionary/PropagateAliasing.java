/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.dictionary;

import java.util.HashSet;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;


public class PropagateAliasing {

	private static final class IntPair {
		final int i1;
		final int i2;

		IntPair(int i1, int i2) {
			this.i1 = i1;
			this.i2 = i2;
		}

		public int hashCode() {
			return i1 + (i2 << 6);
		}

		public boolean equals(Object obj) {
			if (obj instanceof IntPair) {
				IntPair other = (IntPair) obj;

				return other.i1 == i1 && other.i2 == i2;
			}

			return false;
		}
	}

	public static class Aliasing {

		private final Set<IntPair> aliases = new HashSet<PropagateAliasing.IntPair>();

		public void addAlias(int[] a1, int[] a2) {
			for (int i : a1) {
				for (int j : a2) {
					addAlias(i, j);
				}
			}
		}

		public void addAlias(int a1, int a2) {
			aliases.add(new IntPair(a1, a2));
			aliases.add(new IntPair(a2, a1));
		}

		public boolean isAlias(int a1, int a2) {
			return aliases.contains(new IntPair(a1, a2));
		}

	}

	public static void propagate(SDG sdg, Aliasing aliases) {

	}

}
