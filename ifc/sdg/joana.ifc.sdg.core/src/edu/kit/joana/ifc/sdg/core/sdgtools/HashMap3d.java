/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.sdgtools;

import java.util.HashMap;

public class HashMap3d<K1, K2, T> {

	/**
	 * @uml.property  name="x"
	 * @uml.associationEnd  qualifier="keyx:java.lang.Object java.util.HashMap"
	 */
	HashMap<K1, HashMap<K2, T>> x = new HashMap<K1, HashMap<K2, T>>();

	public void put(K1 keyx, K2 keyy, T value) {
		HashMap<K2, T> y = x.get(keyx);
		if (y == null) {
			y = new HashMap<K2, T>();
			x.put(keyx, y);
		}
		y.put(keyy, value);
	}

	public T get(K1 keyx, K2 keyy) {
		HashMap<K2, T> y = x.get(keyx);
		if (y == null) {
			return null;
		}
		return y.get(keyy);
	}

	public boolean containsKey(K1 keyx, K2 keyy) {
		if (!x.containsKey(keyx)) {
			return false;
		} else {
			HashMap<K2, T> y = x.get(keyx);
			return y.containsKey(keyy);
		}

	}

	public String toString() {
		StringBuffer ret = new StringBuffer();
		for (K1 xkey : x.keySet()) {
			HashMap<K2, T> now = x.get(xkey);
			for (K2 ykey : now.keySet()) {
				T value = now.get(ykey);
				ret.append(xkey + " X " + ykey + " -> " + value);
			}
		}
		return ret.toString();
	}
}
