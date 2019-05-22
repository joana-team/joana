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
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

/**
 * A Set backed by an Object array. Optimized for size only, with severe consequences for speed.
 * 
 * {@link NotTightArraySet#add(Object)      is a O(log(n) + m) lookup, plus a O(n) memory copy}
 * {@link NotTightArraySet#remove(Object)   is a O(log(n) + m) lookup, plus a O(n) memory copy}
 * {@link NotTightArraySet#contains(Object) is a O(log(n) + m) lookup}
 * 
 * where m is the number of objects with the same {@link Object#hashCode()} as the given object [1],
 * and n is the current size of the backing array; 
 * 
 * 
 * The iteration-order is by the keys {@link Object#hashCode}, but no guarantee is given for
 * the order of keys with equal {@link Object#hashCode}.
 * 
 * [1] assuming O(1 {@link Object#hashCode()} implementations. 
 * 
 * @author martin.hecker@kit.edu <Martin Hecker>
 */
public class NotTightArraySet<K> extends AbstractSet<K> implements Set<K> {
	
	public static final Comparator<Object> COMPARATOR = new Comparator<Object>() {
		@Override
		public int compare(Object e1, Object e2) {
			if (e1 == null) return -1;
			if (e2 == null) return 1;
			return Integer.compare(e1.hashCode(), e2.hashCode());
		}
	};
	
	protected static final Object[] empty = new Object[0];
	protected static final int SIZE_UNKNOWN = -2;
	
	
	protected Object[] keys;
	
	protected int size;
//	protected int maxIndex;
	
	protected boolean compactEnabled;
	
//	protected boolean isCompact;
	
	public NotTightArraySet() {
		this(0);
	}
	
	public NotTightArraySet(int capacity) {
		this.compactEnabled = true;
		this.size = 0;
//		this.maxIndex = -1;
		this.keys   = new Object[capacity];
//		this.isCompact = true;
	}

	/*
	public NotTightArraySet(K[] entries) {
		this.keys   = new Object[entries.length];
		System.arraycopy(entries, 0, this.keys, 0, entries.length);
		Arrays.sort(this.keys, ENTRY_COMPARATOR);
		{
			int i; 
			for (i = entries.length - 1; i >= 0 && this.keys[i] != null; i--);
			assert i >= -1;
			this.maxIndex = i;
			this.size = i + 1;
		}
		this.isCompact = this.size == entries.length;
		
		assert invariant();
	}
	*/
	protected NotTightArraySet(K[] entries) {
		this.size = SIZE_UNKNOWN;
		this.keys   = entries;
		{
//			boolean isCompact = true;
//			int size;
//			for (size = entries.length; size > 0 && this.keys[size - 1] == null; size--);
//			this.maxIndex = size - 1;
//			
//			int i;
//			for (i = size - 1; i >= 0;  i--) {
//				if (this.keys[i] == null) {
//					isCompact = false;
//					size--;
//				}
//			}
//			this.size = size;
//			this.isCompact = isCompact;
//			boolean isCompact = true;
//			int size = entries.length;
//			int maxIndex;
//			for (maxIndex = entries.length - 1; maxIndex >= 0 && this.keys[maxIndex] == null; maxIndex--);
//			size = maxIndex + 1;			
//			for (int i = maxIndex - 1; i >= 0; i--) {
//				if (this.keys[i] == null) {
//					size--;
//					isCompact = false;
//				}
//			}
//			this.maxIndex = maxIndex;
//			this.size = size;
//			this.isCompact = isCompact;
		}
		assert invariant();
		
	}
	
	protected int maxIndex() {
		int maxIndex;
		for (maxIndex = keys.length - 1; maxIndex >= 0 && this.keys[maxIndex] == null; maxIndex--);
		return maxIndex;
	}

	protected void count() {
		if (size != SIZE_UNKNOWN) return;
		int size = 0;
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != null) size++;
		}
		this.size = size;
	}
	
	protected boolean invariant() {
		
		if (keys == null) return false;
		
		final int length = keys.length;
		
		
		int lastHashCode = Integer.MIN_VALUE;
		int realSize = 0;
		for (int i = 0; i < length; i++) {
			final Object key = keys[i];
			if (key == null) continue;
			
			realSize++;
			
			final int hashCode = key.hashCode();
			if (hashCode < lastHashCode) return false;
			
			// no duplicates
			for (int j = i - 1; j >= 0 && keys[j] != null && keys[j].hashCode() == hashCode; j--) {
				if (key.equals(keys[j])) return false;
			}
			
			lastHashCode = hashCode;
		}
		if (size != SIZE_UNKNOWN && size != realSize) return false;
		return true;
	}
	
	@Override
	public int size() {
		count();
		return size;
	}

	@Override
	public boolean isEmpty() {
		count();
		return size == 0;
	}

	
	/**
	 * @see Arrays#binarySearch(int[], int)
	 */
	// Adapted from java.util.ArraysbinarySearch0
	protected int binarySearch0(int fromIndex, int toIndex, Object element) {
		final int key = element.hashCode(); 
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int midCandidate = (low + high) >>> 1;
			assert low <= midCandidate && midCandidate <= high;
			int mid = midCandidate;
			while (mid >= low && keys[mid] == null) mid--;
			if (mid < low) {
				mid = midCandidate;
				while (mid <= high && keys[mid] == null) mid++;
			}
			
			
			if (mid < low || mid > high) {
				final int insertionPoint = low;
				return -insertionPoint - 1; // key not found.
			}
			final int insertionPoint = mid + 1;
			assert low <= mid && mid <= high;
			
			final Object midElement = keys[mid];
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
				 &&  (keys[i] == null || keys[i].hashCode() == key)
				 && !(found = element.equals(keys[i]))
				) { i++; }
				if (found) return i;

				// .. then left
				i = mid - 1;
				while(i >= low
				 &&  (keys[i] == null || keys[i].hashCode() == key)
				 && !(found = element.equals(keys[i]))
				) { i--; }
				if (found) return i;
				
				
				return -insertionPoint - 1; // key not found. 
			}
		}
		return -(low + 1); // key not found.
	}
	
	@Override
	public boolean contains(Object key) {
		if (key == null) return false;
		
		final int index = binarySearch0(0, keys.length, key);
		return index >=0;
	}
	
	
	
	@Override
	public Iterator<K> iterator() {
		return new ArrayIterator<>(keys);
	}

/*
	@Override
	public Set<K> keySet() {
		return new AbstractSet<K>() {

			@Override
			public int size() {
				return size;
			}

			@Override
			public boolean isEmpty() {
				return size == 0;
			}
			
			@Override
			public boolean contains(Object o) {
				return containsKey(o);
			}
			
			@Override
			public boolean remove(Object o) {
				return NotTightArraySet.this.remove(o) != null;
			}
			
			@Override
			public boolean removeAll(Collection<?> c) {
				final boolean wasCompactEnabled = compactEnabled; 
				compactEnabled = false;
				boolean result = super.removeAll(c);
				compactEnabled = wasCompactEnabled;
				return result;
			}

			@Override
			public Iterator<K> iterator() {
				return new ArrayIterator<>(keys);
			}
		};
	}
	
	@Override
	public Collection<V> values() {
		return new AbstractCollection<V>() {

			@Override
			public int size() {
				return size;
			}

			@Override
			public boolean isEmpty() {
				return size == 0;
			}

			@Override
			public boolean contains(Object o) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Iterator<V> iterator() {
				return new ArrayIterator<>(values);
			}

			@Override
			public boolean add(V e) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean remove(Object o) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void clear() {
				throw new UnsupportedOperationException();
			}
			
		};
	}
	
	@Override
	public Set<Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}
*/	
	
	@SuppressWarnings("unchecked")
	public static <K> NotTightArraySet<K> own(Object[] elements) {
		if (elements == null) return new NotTightArraySet<>((K[]) empty);
		return new NotTightArraySet<>((K[])elements);
	}
	
	public final K[] disown() {
		@SuppressWarnings("unchecked")
		K[] result = (K[]) keys;
		keys = null;
		return result;
	}


}
