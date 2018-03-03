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
import java.util.function.Predicate;

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
public class ArraySet<E> extends AbstractSet<E> implements Set<E>{
	
	private static final Comparator<Object> COMPARATOR = new Comparator<Object>() {
		@Override
		public int compare(Object e1, Object e2) {
			return Integer.compare(e1.hashCode(), e2.hashCode());
		}
	};
	
	protected static final Object[] empty = new Object[0];
	
	protected Object[] elements;
	
	public ArraySet(Set<E> other) {
		final Object[] elements = other.toArray();
		
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] == null) throw new NullPointerException();
		}
		Arrays.sort(elements, COMPARATOR);
		this.elements = elements;
		
		assert invariant();
	}
	
	protected ArraySet(Object[] elements) {
		this.elements = elements;
		
		assert invariant();
	}
	
	public static <E> ArraySet<E> own(Object[] elements) {
		if (elements == null) return new ArraySet<>(empty);
		return new ArraySet<>(elements);
	}
	
	protected boolean invariant() {
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
	public final int size() {
		return elements.length;
	}

	@Override
	public final boolean isEmpty() {
		return elements.length == 0;
	}

	
	/**
	 * @see Arrays#binarySearch(int[], int)
	 */
	// Adapted from java.util.ArraysbinarySearch0
	// This may be called in a state where some elements are null, see, e.g., 
	// ArraySet#removeAll().
	protected int binarySearch0(int fromIndex, int toIndex, Object element) {
		final int key = element.hashCode(); 
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			assert low <= mid && mid <= high;
			Object midCandidate = null;
			{
				while (mid >= 0 && (midCandidate = elements[mid]) == null) mid--;
			}
			final int insertionPoint = mid + 1;
			
			if (mid < low) return -insertionPoint - 1; // key not found.
			assert low <= mid && mid <= high;
			
			final Object midElement = midCandidate;
			assert midElement != null;
			
			final int midVal = midElement.hashCode();
			
			if (midVal < key) {
				low = mid + 1;
			} else if (midVal > key) {
				high = mid - 1;
			} else {
				if (element.equals(midElement)) return mid; // lucky shot
				
				// among all elements s.t. key == elements[i].hashCode(),
				// we have to find the one for which element.equals(elements[i]).
				boolean found = false;
				int i;
				Object elementAtI;
				
				// lets look right
				i = mid + 1;
				while(
				    (i <= high)
				 && (    (elementAtI = elements[i]) == null
				      || (elementAtI.hashCode() == key)
				    )
				 && (!(found = element.equals(elementAtI)))
				) { i++; }
				if (found) return i;
				
				// .. then left
				i = mid - 1;
				while(
				    (i >= low)
				 && (    (elementAtI = elements[i]) == null
				      || (elementAtI.hashCode() == key)
				    )
				 && (!(found = element.equals(elementAtI)))
				) { i--; }
				if (found) return i;
				
				return -insertionPoint - 1; // key not found. 
			}
		}
		return -(low + 1); // key not found.
	}
	
	protected int binarySearch(int fromIndex, int toIndex, Object element) {
		final int key = element.hashCode(); 
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			final int mid = (low + high) >>> 1;
			assert low <= mid && mid <= high;
			
			
			if (mid < low) {
				final int insertionPoint = mid + 1;
				return -insertionPoint - 1; // key not found.
			}
			assert low <= mid && mid <= high;
			
			final Object midElement = elements[mid];
			assert midElement != null;
			
			final int midVal = midElement.hashCode();
			
			if (midVal < key) {
				low = mid + 1;
			} else if (midVal == key) {
				if (element.equals(midElement)) return mid; // lucky shot
				
				// among all elements s.t. key == elements[i].hashCode(),
				// we have to find the one for which element.equals(elements[i]).
				boolean found = false;
				int i;
				Object elementAtI;
				
				// lets look right
				i = mid + 1;
				while(
				    (i <= high)
				 && ((elementAtI = elements[i]).hashCode() == key)
				 && (!(found = element.equals(elementAtI)))
				) { i++; }
				if (found) return i;
				
				// .. then left
				i = mid - 1;
				while(
				    (i >= low)
				 && ((elementAtI = elements[i]).hashCode() == key)
				 && (!(found = element.equals(elementAtI)))
				) { i--; }
				if (found) return i;
				
				final int insertionPoint = mid + 1;
				return -insertionPoint - 1; // key not found. 
			} else {
				high = mid - 1;
			}
		}
		return -(low + 1); // key not found.
	}
	
	@Override
	public final boolean contains(Object o) {
		if (o == null) return false;
		
		final int index = binarySearch(0, elements.length, o);
		return index >=0;
	}

	@Override
	public final Iterator<E> iterator() {
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
	public final Object[] toArray() {
		return Arrays.copyOf(elements, elements.length);
	}

	
	@Override
	@SuppressWarnings("unchecked")
	public final <T> T[] toArray(T[] a) {
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
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();	}
	
	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();	}
	
	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();	}
	
	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();	}
	
	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		throw new UnsupportedOperationException();
	}
}
