/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 29.03.2005
 *
 */
package edu.kit.joana.ifc.sdg.core.sdgtools;

import java.util.HashMap;
import java.util.HashSet;

/**
 * @author naxan
 *
 * HashMapToList allows to associate several values to a single key
 *
 */
public class HashMap1toN<K, T> {
	/**
	 * @uml.property  name="map"
	 * @uml.associationEnd  qualifier="key:java.lang.Object java.util.HashSet"
	 */
	HashMap<K, HashSet<T>> map = new HashMap<K, HashSet<T>>();

	/***
	 * Doesnt allow double value for a single key
	 * @param key
	 * @param value
	 */
	public void put(K key, T value) {
		HashSet<T> vals = map.get(key);
		if (vals == null) {
			vals = new HashSet<T>();
			map.put(key, vals);
		}
		vals.add(value);
	}

	/***
	 *
	 * @param key
	 * @return List of all values associated with key
	 */
	public HashSet<T> get(K key) {
		HashSet<T> vals = map.get(key);
		return vals == null ? new HashSet<T>() : vals;
	}

	public void clear() {
		map.clear();
	}

	@Override
	public String toString() {
		return map.toString();
	}
}
