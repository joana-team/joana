/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

class ArrayIterator<T> implements Iterator<T> {
	protected static final int NONE = -1;
	
	protected final Object[] array;
	private int i;
	protected int last;
	
	public ArrayIterator(Object[] array) {
		this.array = array;
		this.i = 0;
		this.last = NONE;
	}
	private void findNext() {
		while (i < array.length && array[i] == null) i++;
		assert (i <= array.length);
	}
	@Override
	public boolean hasNext() {
		findNext();
		return i < array.length;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T next() {
		findNext();
		if (i == array.length) throw new NoSuchElementException();
		last = i;
		return (T) array[i++];
	}
}