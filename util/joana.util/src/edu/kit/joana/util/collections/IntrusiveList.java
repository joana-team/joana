/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.collections;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * 
 * Each element may only be added once!!!
 * 
 * @author Martin Hecker <martin.hecker@kit.edu>
 */

public class IntrusiveList<T extends Intrusable<T>> extends AbstractList<T>  { //TODO: support more operations
	private int size;
	private T head;
	
	public IntrusiveList() {
		this.size = 0;
		this.head = null;
	}
	
	public T poll() {
		assert (size > 0) == (head != null);
		if (head == null) {
			return null;
		}
		
		T first = head;
		head = head.getNext();
		size--;
		first.setNext(null);
		return first;
	}
	
	@Override
	public T get(int index) {
		T current = head;
		while (index > 0 && current != null) {
			current = current.getNext();
			index--;
		}
		if (current == null) {
			throw new IndexOutOfBoundsException();
		}
		return current;
	}

	@Override
	public int size() {
		return size;
	}
	
	@Override
	public boolean isEmpty() {
		assert size >= 0;
		return size == 0;
	}
	
	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			T current = head;
			
			@Override
			public boolean hasNext() {
				return current != null;
			}
			
			@Override
			public T next() {
				final T result = current;
				current = current.getNext();
				return result;
			}
		};
	}
	
	@Override
	public ListIterator<T> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<T> listIterator(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(T e) {
		if (e.getNext() != null) throw new IllegalArgumentException();
		e.setNext(head);
		head = e;
		size++;
		return true;
	}

}
