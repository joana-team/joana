/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.maps;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MapUtils {

	private MapUtils() {

	}

	/**
	 * Computes the (almost) inverse of the given map. If m(x) is the value set for each x and the resulting map is
	 * denoted by m', then m' is defined by m'(y) := {x | y = m(x)}.
	 * @param <A> key type
	 * @param <B> value type
	 * @param m map to invert
	 * @return the map m' defined by m'(y) := {x | y = m(x)}
	 */
	public static <A,B> Map<B, Set<A>> invertSimple(Map<A, B> m) {
		Map<B, Set<A>> inverse = new HashMap<B, Set<A>>();


		for (A key : m.keySet()) {
			B val = m.get(key);

			Set<A> preImageOfVal;
			if (inverse.containsKey(val)) {
				preImageOfVal = inverse.get(val);
			} else {
				preImageOfVal = new HashSet<A>();
				inverse.put(val, preImageOfVal);
			}
			preImageOfVal.add(key);
		}
		return inverse;
	}

	/**
	 * Computes the (almost) inverse of the given map. If m(x) is the value set for each x and the resulting map is
	 * denoted by m', then m' is defined by m'(y) := {x | y in m(x)}.
	 * @param <A> key type
	 * @param <B> value type
	 * @param m map to invert
	 * @return the map m' defined by m'(y) := {x | y in m(x)}
	 */
	public static <A,B> Map<B, Set<A>> invert(Map<A, Set<B>> m) {
		Map<B, Set<A>> inverse = new HashMap<B, Set<A>>();


		for (A key : m.keySet()) {
			Set<B> val = m.get(key);
			for (B x : val) {
				Set<A> preImageOfX;
				if (inverse.containsKey(x)) {
					preImageOfX = inverse.get(x);
				} else {
					preImageOfX = new HashSet<A>();
					inverse.put(x, preImageOfX);
				}
				preImageOfX.add(key);
			}
		}

		return inverse;
	}

	/**
	 * Computes the concatenation of the two given maps. The resulting map m3 is defined by <p>
	 * m3(x) := {z | ex. y in m1(x) s.t. z in m2(y)}
	 * @param <A> key type
	 * @param <B> value type
	 * @param m map to invert
	 * @return the map m' defined by m'(y) := {x | y in m(x)}
	 */
	public static <A, B, C> Map<A, Set<C>> concat(Map<A, Set<B>> m1, Map<B, Set<C>> m2) {
		Map<A, Set<C>> result = new HashMap<A, Set<C>>();
		for (A x : m1.keySet()) {
			Set<C> res = new HashSet<C>();
			// res contains all m2.get(a) for a in m1.get(x)
			for (B a : m1.get(x)) {
				if (m2.containsKey(a)) {
					res.addAll(m2.get(a));
				}
			}

			result.put(x, res);
		}

		return result;
	}
}
