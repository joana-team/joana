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
import java.util.Set;

/**
 * @author Martin Mohr
 */
public class MultiHashMap<K, T> extends MultiMap<K, T> {

	/* (non-Javadoc)
	 * @see edu.kit.joana.util.maps.MultiMap#createMap()
	 */
	@Override
	protected Map<K, Set<T>> createMap() {
		return new HashMap<K, Set<T>>();
	}

}
