/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util;

import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author Juergen Graf <grafj@ipd.info.uni-karlsruhe.de>
 *
 * @param <K> type of the key values
 * @param <V> type of the value values
 */
public final class CacheMap<K, V> extends HashMap<K, V> {

	private static final long serialVersionUID = -5853335404878957666L;

	private int cacheSize;
	private LinkedList<K> fifo;

	public CacheMap(int cacheSize) {
		super(cacheSize);
		this.cacheSize = cacheSize;
		fifo = new LinkedList<K>();
	}

    public V put(K key, V value) {
    	if (fifo.size() == cacheSize) {
    		K delete = fifo.removeFirst();
    		super.remove(delete);
    	}

    	fifo.add(key);

    	return super.put(key, value);
    }

    public V remove(Object key) {
    	fifo.remove(key);

    	return super.remove(key);
    }

}
