/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.collections;

import java.util.Map;

import edu.kit.joana.util.graph.IntegerIdentifiable;

public class SimpleVector<K extends IntegerIdentifiable, V>  extends SimpleVectorBase<K, V> implements Map<K, V> {
	public SimpleVector(int initialLeftCapacity, int initialRightCapacity) {
		super(initialLeftCapacity, initialRightCapacity);
	}
	
	@Override
	protected final int getId(K k) {
		return k.getId();
	}
}
