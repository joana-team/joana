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

/**
 * @author martin.hecker@kit.edu <Martin Hecker>
 */
public class ArraySet<E> extends AbstractSet<E> implements Set<E>{
	
	private static final Comparator<Object> COMPARATOR = new Comparator<Object>() {
		@Override
		public int compare(Object e1, Object e2) {
			return Integer.compare(e1.hashCode(), e2.hashCode());
		}
	};
	// static enum GrowthStrategy { MINIMAL, DOUBLE, PLUSHALF };
	
	private E[] elements;
	
	//private int size;
	
	// protected abstract GrowthStrategy getGrowthStrategy();

	
	public ArraySet(Set<E> other) {
		@SuppressWarnings("unchecked")
		final E[] elements = (E[]) other.toArray();
		
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] == null) throw new NullPointerException();
		}
		Arrays.sort(elements, COMPARATOR);
		this.elements = elements;
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
	// adapted from java.util.ArraysbinarySearch0
	private int binarySearch0(int fromIndex, int toIndex, Object element) {
		final int key = element.hashCode(); 
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			while (mid >= 0 && elements[mid] == null) mid--;
			final int insertionPoint = mid + 1;
			//assert elements[mid] != null;
			if (mid < low) return -insertionPoint - 1;
			
			final E midElement = elements[mid];
			assert midElement != null;
			
			int midVal = midElement.hashCode();
			
			if (midVal < key)
				low = mid + 1;
			else if (midVal > key)
				high = mid - 1;
			else {
				boolean found = false;
				int i;
				
				i = mid;
				while(i < elements.length
				 &&  (elements[i] == null || elements[i].hashCode() == key)
				 //&&  (elements[i].hashCode() == key)
				 && !(found = element.equals(elements[i]))
				) { i++; }
				if (found) return i;

				i = mid;
				while(i > 0
				 &&  (elements[i] == null || elements[i].hashCode() == key)
				 //&&  (elements[i].hashCode() == key)
				 && !(found = element.equals(elements[i]))
				) { i--; }
				if (found) return i;
				
				
				return -insertionPoint - 1;
			}
		}
		return -(low + 1);  // key not found.
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
			public E next() {
				assert i <= elements.length;
				if (i == elements.length) throw new NoSuchElementException(); 
				return elements[i++];
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
//		final int newSize;
//		switch (getGrowthStrategy()) {
//			case MINIMAL:  newSize =             elements.length + 1;                      break;
//			case DOUBLE:   newSize = Math.max(1, elements.length * 2);                     break;
//			case PLUSHALF: newSize = Math.max(1, elements.length + (elements.length / 2)); break;
//			default: throw new IllegalStateException();
//		}
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
		
		return true;
	}
	
   public boolean addAll(Collection<? extends E> c) {
       boolean modified = false;
       for (E e : c)
           if (add(e))
               modified = true;
       return modified;
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
			return true;
		}
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
			return true;
		}
		return false;
	}
	

	@Override
	public void clear() {
		@SuppressWarnings("unchecked")
		E[] newElements = (E[]) new Object[0];
		elements = newElements;
	}
	
}
