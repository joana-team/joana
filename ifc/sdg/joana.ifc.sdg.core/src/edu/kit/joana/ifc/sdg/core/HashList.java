/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core;

import java.util.HashMap;
import java.util.Iterator;
//import java.util.LinkedList;
import java.util.LinkedHashMap;

public class HashList<T> {
	private HashMap<T, T> valuesSet = new LinkedHashMap<T, T>();

	public void addFirst(T value) {
		valuesSet.put(value, value);
	}

	public T removeFirst() {
		Iterator<T> i = valuesSet.keySet().iterator();
		T ret = i.next();
		i.remove();

		return ret;
	}

	public boolean isEmpty() {
		return valuesSet.isEmpty();
	}

	public int size() {
		return valuesSet.size();
	}
}
