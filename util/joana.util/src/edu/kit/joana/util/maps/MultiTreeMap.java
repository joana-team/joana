/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.maps;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Martin Mohr
 */
public class MultiTreeMap<K extends Comparable<? super K>, T> extends MultiMap<K, T> {

	/* (non-Javadoc)
	 * @see edu.kit.joana.util.maps.MultiMap#createMap()
	 */
	@Override
	protected Map<K, Set<T>> createMap() {
		return new TreeMap<K, Set<T>>();
	}

}
