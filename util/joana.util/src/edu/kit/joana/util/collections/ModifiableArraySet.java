/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.collections;

import java.util.Collection;
import java.util.Set;

/**
 * Martin Hecker <martin.hecker@kit.edu>
 * 
 */
public final class ModifiableArraySet<E> extends ArraySet<E> {

	private ModifiableArraySet(Object[] elements) {
		super(elements);
	}
	public ModifiableArraySet(Set<E> other) {
		super(other);
	}
	
	public static <E> ModifiableArraySet<E> own(Object[] elements) {
		if (elements == null) return new ModifiableArraySet<>(empty);
		return new ModifiableArraySet<>(elements);
	}
	
	@Override
	public final boolean add(E e) {
		if (e == null) throw new NullPointerException();
		
		final int index = binarySearch0(0, elements.length, e);
		if (index >= 0) return false;
		
		final int insert = -index - 1;
		assert insert >= 0;
		assert insert == elements.length || elements[insert] != null;
		
		final int newSize = elements.length + 1;

		@SuppressWarnings("unchecked")
		E[] newElements = (E[]) new Object[newSize];
		if (insert > 0) {
			System.arraycopy(elements,      0, newElements,          0, insert);
		}
		if (insert < elements.length) {
			System.arraycopy(elements, insert, newElements, insert + 1, elements.length - insert);
		}
		newElements[insert] = e;
		
		this.elements = newElements;
		
		assert invariant();
		return true;
	}

	@Override
	public final boolean addAll(Collection<? extends E> c) {
		// TODO this is correct, but very slow. Needs to be optimized. 
		return super.addAll(c);
	}
	
	@Override
	public final boolean remove(Object o) {
		if (o == null) throw new NullPointerException();
		
		final int remove = binarySearch0(0, elements.length, o);
		if (remove < 0) return false;
		
		assert (remove < elements.length);
		assert (elements.length > 0);
		
		@SuppressWarnings("unchecked")
		E[] newElements = (E[]) new Object[elements.length - 1];
		System.arraycopy(elements,          0, newElements,      0, remove);
		System.arraycopy(elements, remove + 1, newElements, remove, elements.length - remove - 1  );
		this.elements = newElements;
		
		assert invariant();
		return true;
	}
	
	@Override
	public final boolean removeAll(Collection<?> c) {
		int removed = 0;
		for (Object o : c) {
			final int remove = binarySearch0(0, elements.length, o);
			if (remove < 0) continue;
			
			assert (remove < elements.length);
			assert (elements.length > 0);
			
			elements[remove] = null;
			removed++;
		}
		
		if (removed > 0) {
			compact(removed);
			
			assert invariant();
			return true;
		}
		
		assert invariant();
		return false;
	}
	
	private void compact(int removed) {
		@SuppressWarnings("unchecked")
		E[] newElements = (E[]) new Object[elements.length - removed];
		
		int k = 0;
		int i = 0;
		
		while (true) {
			while (i < elements.length && elements[i] == null) i++;
			if (i == elements.length)  {
				this.elements = newElements;
				return;
			}
			assert elements[i] != null;
			
			int j = i;
			while (j < elements.length && elements[j] != null) j++;
			assert j == elements.length || elements[j] == null;
			
			int length = j - i;
			assert length > 0;
			
			System.arraycopy(elements, i, newElements,      k, length);
			k += length;
			
			i = j;
		}
	}

	@Override
	public final boolean retainAll(Collection<?> c) {
		int removed = 0;
		for (int i = 0; i < elements.length; i++) {
			assert elements[i] != null;
			if (!c.contains(elements[i])) {
				elements[i] = null;
				removed++;
			}
		}
		
		if (removed > 0) {
			compact(removed);
			
			assert invariant();
			return true;
		}
		
		assert invariant();
		return false;
	}
	

	@Override
	public final void clear() {
		@SuppressWarnings("unchecked")
		E[] newElements = (E[]) new Object[0];
		elements = newElements;
		
		assert invariant();
		return;
	}
	
	public final E[] disown() {
		@SuppressWarnings("unchecked")
		E[] result = (E[]) elements;
		elements = null;
		return result;
	}

}
