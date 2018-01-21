/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.collections;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

/**
 * A Set backed by an Object array. Optimized for size only, with severe consequences for speed.
 * 
 * {@link ArraySet#add(Object)      is a O(log(n) + m) lookup, plus a O(n) memory copy}
 * {@link ArraySet#remove(Object)   is a O(log(n) + m) lookup, plus a O(n) memory copy}
 * {@link ArraySet#contains(Object) is a O(log(n) + m) lookup}
 * 
 * where m is the number of objects with the same {@link Object#hashCode()} as the given object [1].
 * 
 * Currently, {@link ArraySet#removeAll(Collection)} is somewhat optimized,
 * but {@link ArraySet#addAll(Collection) is not yet}.
 * 
 * The iteration-order is by the elements {@link Object#hashCode}, but no guarantee is given for
 * the order of elements with equal {@link Object#hashCode} (hence, we cannot implement {@link SortedSet}.
 *  
 * 
 * [1] assuming O(1 {@link Object#hashCode()} implementations. 
 * 
 * @author martin.hecker@kit.edu <Martin Hecker>
 */
public final class ArraySet<E> extends AbstractSet<E> implements Set<E>{
	
	private static final Comparator<Object> COMPARATOR = new Comparator<Object>() {
		@Override
		public int compare(Object e1, Object e2) {
			return Integer.compare(e1.hashCode(), e2.hashCode());
		}
	};
	
	private static final Object[] empty = new Object[0];
	
	private Object[] elements;
	
	public ArraySet(Set<E> other) {
		final Object[] elements = other.toArray();
		
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] == null) throw new NullPointerException();
		}
		Arrays.sort(elements, COMPARATOR);
		this.elements = elements;
		
		assert invariant();
	}
	
	private ArraySet(Object[] elements) {
		this.elements = elements;
		
		assert invariant();
	}
	
	public static <E> ArraySet<E> own(Object[] elements) {
		if (elements == null) return new ArraySet<>(empty);
		return new ArraySet<>(elements);
	}
	
	private boolean invariant() {
		if (elements == null) return false;
		int lastHashCode = Integer.MIN_VALUE;
		for (int i = 0; i < elements.length; i++) {
			final Object element = elements[i];
			if (element == null) return false;
			
			final int hashcode = elements[i].hashCode();
			if (hashcode < lastHashCode) return false;
			
			// no duplicates
			for (int j = i - 1; j >= 0 && elements[j].hashCode() == hashcode; j--) {
				if (element.equals(elements[j])) return false;
			}
		}
		return true;
	}
	
	@Override
	public int size() {
		return elements.length;
	}

	@Override
	public boolean isEmpty() {
		return elements.length == 0;
	}

	
	/**
	 * @see Arrays#binarySearch(int[], int)
	 */
	// Adapted from java.util.ArraysbinarySearch0
	// This may be called in a state where some elements are null, see, e.g., 
	// ArraySet#removeAll().
	private int binarySearch0(int fromIndex, int toIndex, Object element) {
		final int key = element.hashCode(); 
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			assert low <= mid && mid <= high;
			while (mid >= 0 && elements[mid] == null) mid--;
			final int insertionPoint = mid + 1;
			
			if (mid < low) return -insertionPoint - 1; // key not found.
			assert low <= mid && mid <= high;
			
			final Object midElement = elements[mid];
			assert midElement != null;
			
			int midVal = midElement.hashCode();
			
			if (midVal < key)
				low = mid + 1;
			else if (midVal > key)
				high = mid - 1;
			else {
				if (element.equals(midElement)) return mid; // lucky shot
				
				// among all elements s.t. key == elements[i].hashCode(),
				// we have to find the one for which element.equals(elements[i]).
				boolean found = false;
				int i;
				
				// lets look right
				i = mid + 1;
				while(i <= high
				 &&  (elements[i] == null || elements[i].hashCode() == key)
				 && !(found = element.equals(elements[i]))
				) { i++; }
				if (found) return i;

				// .. then left
				i = mid - 1;
				while(i >= low
				 &&  (elements[i] == null || elements[i].hashCode() == key)
				 && !(found = element.equals(elements[i]))
				) { i--; }
				if (found) return i;
				
				
				return -insertionPoint - 1; // key not found. 
			}
		}
		return -(low + 1); // key not found.
	}
	
	@Override
	public boolean contains(Object o) {
		if (o == null) return false;
		
		final int index = binarySearch0(0, elements.length, o);
		return index >=0;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			int i = 0;

			@Override
			public boolean hasNext() {
				return i < elements.length;
			}

			@Override
			@SuppressWarnings("unchecked")
			public E next() {
				assert i <= elements.length;
				if (i == elements.length) throw new NoSuchElementException(); 
				return (E) elements[i++];
			}
			
		};
	}

	@Override
	public Object[] toArray() {
		return Arrays.copyOf(elements, elements.length);
	}

	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		if (a.length < elements.length) {
			a = (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), elements.length);
		}
		
		System.arraycopy(elements, 0, a, 0, elements.length);
		if (a.length > elements.length) {
			a[elements.length] = null;
		}
		return a;
	}

	@Override
	public boolean add(E e) {
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
	public boolean addAll(Collection<? extends E> c) {
		// TODO this is correct, but very slow. Needs to be optimized. 
		return super.addAll(c);
	}
	
	@Override
	public boolean remove(Object o) {
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
	public boolean removeAll(Collection<?> c) {
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
	public boolean retainAll(Collection<?> c) {
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
	public void clear() {
		@SuppressWarnings("unchecked")
		E[] newElements = (E[]) new Object[0];
		elements = newElements;
		
		assert invariant();
		return;
	}
	
	public E[] disown() {
		@SuppressWarnings("unchecked")
		E[] result = (E[]) elements;
		elements = null;
		return result;
	}
}
