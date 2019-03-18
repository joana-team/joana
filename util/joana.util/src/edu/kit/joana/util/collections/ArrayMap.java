/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.collections;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A Map backed by an Object array. Optimized for size only, with severe consequences for speed.
 * 
 * {@link ArrayMap#add(Object)      is a O(log(n) + m) lookup, plus a O(n) memory copy}
 * {@link ArrayMap#remove(Object)   is a O(log(n) + m) lookup, plus a O(n) memory copy}
 * {@link ArrayMap#contains(Object) is a O(log(n) + m) lookup}
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
public final class ArrayMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
	
	private final static double GROWTH_FACTOR = 1.5;
	
	private static Comparator<Entry<?, ?>> ENTRY_COMPARATOR = new Comparator<Entry<?, ?>>() {
		@Override
		public int compare(Entry<?, ?> e1, Entry<?, ?> e2) {
			return Integer.compare(e1.getKey().hashCode(), e2.getKey().hashCode());
		}
	};
	
	private Object[] keys;
	private Object[] values;
	
	private int size;
	private int maxIndex;
	
	private boolean compactEnabled;
	
	private boolean isCompact;
	
	public ArrayMap() {
		this(0);
	}
	
	public ArrayMap(int capacity) {
		this.compactEnabled = true;
		this.size = 0;
		this.maxIndex = -1;
		this.keys   = new Object[capacity];
		this.values = new Object[capacity];
		this.isCompact = true;
	}
	
	public ArrayMap(Entry<K, V>[] entries) {
		for (Entry<K, V> entry : entries) {
			assert entry.getKey() != null;
			if (entry.getValue() == null) throw new NullPointerException();
		}
		
		this.size = entries.length;
		this.maxIndex = entries.length -1;
		Arrays.sort(entries, ENTRY_COMPARATOR);
		this.keys   = new Object[size];
		this.values = new Object[size];
		for (int i = 0; i < size; i++) {
			final Entry<K, V> e = entries[i];
			this.keys[i]   = e.getKey();
			this.values[i] = e.getValue();
		}
		this.isCompact = true;
		
		assert invariant();
	}
	
	@SuppressWarnings("unchecked")
	public ArrayMap(Map<K, V> other) {
		this((Entry<K, V>[]) other.entrySet().toArray(new Entry<?, ?>[0]));
	}

	
	private boolean isCompact() {
		for (int i = 0; i < size; i++) {
			if (keys[i] == null) return false;
		}
		return true;
	}
	private boolean invariant() {
		
		if (keys == null) return false;
		if (values == null) return false;
		
		if (keys.length != values.length) return false;
		final int length = keys.length;
		
		if (isCompact && !isCompact()) {
			return false;
		}
		
		if (maxIndex >= length) {
			return false;
		}
		
		int lastHashCode = Integer.MIN_VALUE;
		int realSize = 0;
		boolean predecessorWasNull = false;
		for (int i = 0; i < length; i++) {
			final Object key = keys[i];
			final Object value = values[i]; 
			assert (key == null) == (value == null);
			if (key == null) {
				predecessorWasNull = true;
				continue;
			}
			assert i <= maxIndex;
			
			realSize++;
			
			final int hashCode = key.hashCode();
			if (hashCode < lastHashCode) return false;
			
			// keys with same hashCode are stored continuously
			if (predecessorWasNull && hashCode == lastHashCode) return false;
			
			// no duplicates
			for (int j = i - 1; j >= 0 && keys[j] != null && keys[j].hashCode() == hashCode; j--) {
				if (key.equals(keys[j])) return false;
			}
			
			predecessorWasNull = false;
		}
		if (realSize != size) return false;
		return true;
	}
	
	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	
	/**
	 * @see Arrays#binarySearch(int[], int)
	 */
	// Adapted from java.util.ArraysbinarySearch0
	private int binarySearch0(int fromIndex, int toIndex, Object element) {
		final int key = element.hashCode(); 
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			assert low <= mid && mid <= high;
			while (mid >= 0 && keys[mid] == null) mid--;
			final int insertionPoint = mid + 1;
			
			if (mid < low) return -insertionPoint - 1; // key not found.
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
	public boolean containsKey(Object key) {
		if (key == null) return false;
		
		final int index = binarySearch0(0, maxIndex + 1, key);
		return index >=0;
	}
	
	@Override
	public V get(Object key) {
		if (key == null) return null;
		
		final int index = binarySearch0(0, maxIndex + 1, key);
		if (index >= 0) {
			assert index < size && size <= values.length;
			@SuppressWarnings("unchecked")
			V result = (V) values[index];
			
			return result;
		} else {
			return null;
		}
	}
	
	@Override
	public V put(K key, V value) {
		if (key == null || value == null) throw new NullPointerException();
		assert keys.length == values.length;
		final int length = keys.length;
		
		{
			final int index = binarySearch0(0, maxIndex + 1, key);
			if (index >= 0)  {
				assert keys[index] != null;
	
				@SuppressWarnings("unchecked")
				final V oldValue = (V) values[index];
				assert values[index] != null;
	
				values[index] = value;
				return oldValue;
			}
	
			final int insert = -index - 1;
			assert insert >= 0;
	
			if (insert != length && keys[insert] == null) {
				keys[insert] = key;
				values[insert] = value;
				maxIndex = Math.max(maxIndex, insert);
				size++;
				
				assert invariant();
				return null;
			}
	
			if (insert == length && size < length) { // maybe do not do this if |length - size| is very small?
				compact(false);
				assert length == keys.length && length == values.length;
				assert (keys[size] == null);
				keys[size] = key;
				values[size] = value;
				assert maxIndex == size - 1;
				maxIndex = size;
				size++;
				assert invariant();
				return null;
			}
			
			if (size == length) { // maybe even do this a little sooner?
				final int newLength = (int) (GROWTH_FACTOR * (length + 1));
					
				Object[] newKeys   = new Object[newLength];
				Object[] newValues = new Object[newLength];
				if (insert > 0) {
					System.arraycopy(keys,   0, newKeys,   0, insert);
					System.arraycopy(values, 0, newValues, 0, insert);
				}
				if (insert < length) {
					System.arraycopy(keys,   insert, newKeys,   insert + 1, length - insert);
					System.arraycopy(values, insert, newValues, insert + 1, length - insert);
				}
				newKeys[insert] = key;
				newValues[insert] = value;

				this.keys   = newKeys;
				this.values = newValues;
				maxIndex = maxIndex + 1;
				size++;
				
				isCompact = true;
				
				assert invariant();
				return null;
			}
		}
		
		assert size < length;
		compact(false);
		assert length == keys.length && length == values.length;
		assert keys[length - 1] == null;
		final int index = binarySearch0(0, size, key);

		final int insert = -index - 1;
		assert 0 <= insert && insert < length;

		System.arraycopy(keys,   insert, keys,   insert + 1, length - insert - 1);
		System.arraycopy(values, insert, values, insert + 1, length - insert - 1);
		keys[insert] = key;
		values[insert] = value;
		assert maxIndex == size - 1;
		maxIndex = size;
		size++;
		

		
		assert invariant();
		return null;
	}

	@Override
	public V remove(Object o) {
		if (o == null) throw new NullPointerException();
		
		final int remove = binarySearch0(0, maxIndex + 1, o);
		if (remove < 0) return null;
		
		assert (remove < keys.length);
		assert (keys.length > 0);
		
		@SuppressWarnings("unchecked")
		V oldValue = (V) values[remove];
		
		assert keys[remove] != null;
		assert values[remove] != null;
		
		keys[remove] = null;
		values[remove] = null;
		
		if (remove == (size - 1)) {
			maxIndex--;
		} else {
			isCompact = false;
		}
		size--;
		
		assert invariant();
		
		if (compactEnabled) compact(true);
		return oldValue;
	}
	
	public void trimToSize() {
		compact(true);
	}
	
	private void compact(boolean shorten) {
		
		assert keys.length == values.length;
		final int length = keys.length;
		

		if (isCompact) {
			if (shorten) {
				Object[] newKeys   = shorten ? new Object[size] : keys;
				Object[] newValues = shorten ? new Object[size] : values;
				
				System.arraycopy(keys,   0, newKeys,    0, size);
				System.arraycopy(values, 0, newValues,  0, size);
				
				keys   = newKeys;
				values = newValues; 
			}
			return;
		}
		
		int k = 0;
		int i = 0;
		
		Object[] newKeys   = shorten ? new Object[size] : keys;
		Object[] newValues = shorten ? new Object[size] : values;

		
		while (true) {
			while (i < length && keys[i] == null) i++;
			if (i == length)  {
				this.keys = newKeys;
				this.values= newValues;
				maxIndex = size - 1;
				isCompact = true;
				assert invariant();
				return;
			}
			assert keys[i] != null;
			
			int j = i;
			while (j < length && keys[j] != null) j++;
			assert j == length || keys[j] == null;
			
			int blockLength = j - i;
			assert blockLength > 0;
			assert k <= i;
			System.arraycopy(keys,   i, newKeys,    k, blockLength); // TODO: does System.arrayCopy do the smart thing if keys == newKeys, and k == i?!?!?
			System.arraycopy(values, i, newValues,  k, blockLength);
			k += blockLength;
			
			i = j;
		}
	}
	
	@Override
	public void clear() {
		keys   = new Object[0];
		values = new Object[0];
		size = 0;
		isCompact = true;
		this.maxIndex = -1;
		
		assert invariant();
		return;
	}
	
	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	
	
	class ArrayIterator<T> implements Iterator<T> {
		private final Object[] array;
		private int i;
		
		public ArrayIterator(Object[] array) {
			this.array = array;
			this.i = 0;
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
			return (T) array[i++];
		}
	}

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
				return ArrayMap.this.remove(o) != null;
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
	
	
}
