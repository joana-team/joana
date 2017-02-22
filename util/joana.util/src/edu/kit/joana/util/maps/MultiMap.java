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
package edu.kit.joana.util.maps;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 * This class represents a map in which single keys are mapped to sets of values instead
 * of single values.<br>
 * 
 * @author Martin Mohr 
 *
 **/
public abstract class MultiMap<K, T> {


	protected final Map<K, Set<T>> map;

	protected MultiMap() {
		this.map = createMap();
	}

	protected abstract Map<K, Set<T>> createMap();
	/***
	 * Adds a value to the value set of the given key.
	 * @param key key to add value for
	 * @param value value to add to the value set of the given key
	 */
	public void add(K key, T value) {
		Set<T> vals;
		if (map.containsKey(key)) {
			vals = map.get(key);
		} else {
			vals = new HashSet<T>();
			map.put(key, vals);
		}
		vals.add(value);
	}

	public Set<K> keySet() {
		return map.keySet();
	}

	public Set<Map.Entry<K, Set<T>>> entrySet() {
		return map.entrySet();
	}
	/**
	 * Returns all values associated to the given key. In particular, an empty set is returned, if
	 * the given key is not contained in this map.
	 * @param key key to retrieve value set for
	 * @return set of all values associated with key
	 */
	public Set<T> get(K key) {
		Set<T> vals = map.get(key);
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
