/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.collections;

import java.util.AbstractCollection;
import java.util.Iterator;

/**
 * 
 * Each element may only be added once!!!
 * 
 * @author Martin Hecker <martin.hecker@kit.edu>
 */

public class IntrusiveList<T extends Intrusable<T>> extends AbstractCollection<T>  { //TODO: support more operations
	private T head;
	
	public IntrusiveList() {
		this.head = null;
	}
	
	public T poll() {
		if (head == null) {
			return null;
		}
		
		T first = head;
		head = head.getNext();
		first.setNext(null);
		return first;
	}
	
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
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isEmpty() {
		return head == null;
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
	public boolean add(T e) {
		assert e.getNext() == null;
		e.setNext(head);
		head = e;
		return true;
	}
}
