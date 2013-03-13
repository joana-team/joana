/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.maps;

import java.util.HashMap;
import java.util.Map;

/**
 * This class implements a map, in which the keys are pairs. That is, for each
 * pair of keys, there is at most one value.
 * 
 * @author Martin Mohr
 */
public class BinaryMap<K1, K2, T> {

	private final Map<K1, Map<K2, T>> mappings = new HashMap<K1, Map<K2, T>>();

	public void put(K1 keyx, K2 keyy, T value) {
		final Map<K2, T> yMap;
		if (!mappings.containsKey(keyx)) {
			yMap = new HashMap<K2, T>();
			mappings.put(keyx, yMap);
		} else {
			yMap = mappings.get(keyx);
		}
		yMap.put(keyy, value);
	}

	/**
	 * Returns the value associated to the given pair. If no value is associated
	 * to the given pair, {@code null} is returned.
	 * 
	 * @param keyx
	 *            first component of key to retrieve value for
	 * @param keyy
	 *            second component of key to retrieve value for
	 * @return the value associated to the given pair, or {@code null}, if there
	 *         is no such value.
	 */
	public T get(K1 keyx, K2 keyy) {
		if (!containsKey(keyx, keyy)) {
			return null;
		} else {
			return mappings.get(keyx).get(keyy);
		}
	}

	public boolean containsKey(K1 keyx, K2 keyy) {
		if (!mappings.containsKey(keyx)) {
			return false;
		} else {
			Map<K2, T> yMap = mappings.get(keyx);
			return yMap.containsKey(keyy);
		}
	}

	public String toString() {
		if (mappings.isEmpty()) {
			return "{}";
		} else {
			StringBuffer ret = new StringBuffer("{");
			for (Map.Entry<K1, Map<K2, T>> e1 : mappings.entrySet()) {
				K1 keyX = e1.getKey();
				for (Map.Entry<K2, T> e2 : e1.getValue().entrySet()) {
					K2 keyY = e2.getKey();
					T value = e2.getValue();
					ret.append(String.format("(%s, %s) --> %s, ", keyX, keyY, value));
				}
			}
			ret.replace(ret.length() - 2, ret.length(), "}");

			return ret.toString();
		}
	}
}
