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
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * A {@link Map} implementation for {@link IntegerIdentifiable} keys.
 * 
 * This backed by a array, directly accessed by {@link IntegerIdentifiable#getId()},
 * hence this only makes sense for keys that are densely aligned around 0, i.e.: 
 * almost all keys have and id that contained in an interval [-leftMaxIndex, rightMaxIndex] for
 * some leftMaxIndex, rightMaxIndex >=0.
 * 
 * Also, this Map violates the {@link Map} contract whenever there exists keys k1, k2 s.t. 
 * k1.getId() == k2.getId(), but not: k1.equals(k2).
 */
public abstract class SimpleVectorBase<K, V>  extends AbstractMap<K, V> implements Map<K, V> {

	private final static int MAX_SIZE = Integer.MAX_VALUE / 4;

	private final static double GROWTH_FACTOR = 1.5;

	protected Object[] rightKeys;
	protected Object[] rightValues;
	
	protected Object[] leftKeys;
	protected Object[] leftValues;

	protected int rightMaxIndex;
	protected int leftMaxIndex;
	
	protected int size;

	public SimpleVectorBase(int initialLeftCapacity, int initialRightCapacity) {
		if (initialLeftCapacity < 0  || initialLeftCapacity  > MAX_SIZE) throw new IllegalArgumentException(); 
		if (initialRightCapacity < 0 || initialRightCapacity > MAX_SIZE) throw new IllegalArgumentException();

		this.rightMaxIndex = -1;
		this.leftMaxIndex  = -1;
		this.size = 0;
		this.rightKeys   = new Object[initialRightCapacity];
		this.rightValues = new Object[initialRightCapacity];
		this.leftKeys    = new Object[initialLeftCapacity];
		this.leftValues  = new Object[initialLeftCapacity];
	}
	
	public void trimToSize() {
		{
			Object[] oldRightKeys = rightKeys;
			rightKeys = new Object[rightMaxIndex + 1];
			System.arraycopy(oldRightKeys, 0, rightKeys, 0, rightKeys.length);
			oldRightKeys = null;
		}
		{
			Object[] oldLeftKeys = leftKeys;
			leftKeys = new Object[leftMaxIndex + 1];
			System.arraycopy(oldLeftKeys, 0, leftKeys, 0, leftKeys.length);
			oldLeftKeys = null;
		}
		{
			Object[] oldRightValues = rightValues;
			rightValues = new Object[rightMaxIndex + 1];
			System.arraycopy(oldRightValues, 0, rightValues, 0, rightValues.length);
			oldRightValues = null;
		}
		{
			Object[] oldLeftValues = leftValues;
			leftValues = new Object[leftMaxIndex + 1];
			System.arraycopy(oldLeftValues, 0, leftValues, 0, leftValues.length);
			oldLeftValues = null;
		}
	}
	
	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}
	
	protected abstract int getId(K k);

	@Override
	@SuppressWarnings("unchecked")
	public V get(Object o) {
		if (o == null) throw new NullPointerException();
		final int x = getId((K)o);

		if (0 <= x && x <= rightMaxIndex) {
			assert o.equals(rightKeys[x]);
			return (V) rightValues[x];
		}
		
		final int y = -x;
		assert y != 0;
		if (0 <= y && y <= leftMaxIndex) {
			assert o.equals(leftKeys[y]);
			return (V) leftValues[y];
		}
		return null;
	}
	
	@Override
	public boolean containsKey(Object o) {
		if (o == null) throw new NullPointerException();
		@SuppressWarnings("unchecked")
		final int x = getId((K)o);

		if (0 <= x && x <= rightMaxIndex && rightKeys[x] != null) {
			assert o.equals(rightKeys[x]);
			return true;
		}
		
		final int y = -x;
		if (0 <= y && y <= leftMaxIndex  && leftKeys[y] != null) {
			assert o.equals(leftKeys[y]);
			return true;
		}
		
		return false;
	}

	@Override
	public V put(K key, V value) {
		if (key == null || value == null) throw new NullPointerException();
		final int id = getId(key);
		if (id > MAX_SIZE || id < -MAX_SIZE) {
			throw new IllegalArgumentException("id is too big: " + id);
		}
		
		if (0 <= id) {
			final int x = id;
			ensureRightCapacity(x);
	
			@SuppressWarnings("unchecked")
			K currentKey = (K) rightKeys[x];
			@SuppressWarnings("unchecked")
			V oldValue   = (V) rightValues[x];
			
			assert (oldValue == null) == (currentKey == null);
			
			if (currentKey == null) {
				rightKeys[x] = key; 
				size++;
				rightMaxIndex = Math.max(rightMaxIndex, x);
			} else {
				assert key.equals(currentKey);
			}
			rightValues[x] = value;
			
			return oldValue;
		} else {
			final int y = -id;
			assert y != 0;
			
			ensureLeftCapacity(y);
			
			@SuppressWarnings("unchecked")
			K currentKey = (K) leftKeys[y];
			@SuppressWarnings("unchecked")
			V oldValue   = (V) leftValues[y];
			
			assert (oldValue == null) == (currentKey == null);
			
			if (currentKey == null) {
				leftKeys[y] = key; 
				size++;
				leftMaxIndex = Math.max(leftMaxIndex, y);
			} else {
				assert key.equals(currentKey);
			}

			leftValues[y] = value;
			
			return oldValue;
		}
	}

	@Override
	public V remove(Object o) {
		if (o == null) throw new NullPointerException();
		@SuppressWarnings("unchecked")
		final int x = getId((K)o);
		final int y = -x;
		
		if (0 <= x && x <= rightMaxIndex) {
			@SuppressWarnings("unchecked")
			K currentKey = (K) rightKeys[x];
			@SuppressWarnings("unchecked")
			V oldValue   = (V) rightValues[x];
			
			assert (oldValue == null) == (currentKey == null);
			
			rightValues[x] = null;
			rightKeys[x] = null;

			if (currentKey != null) {
				size--;
				if (rightMaxIndex == x) {
					int newMaxIndex = x - 1;
					while (newMaxIndex >= 0 && rightKeys[newMaxIndex] == null) newMaxIndex--;
					rightMaxIndex = newMaxIndex;
				}
			}

			return oldValue;
		} else if (0 <= y && y <= leftMaxIndex) {
			@SuppressWarnings("unchecked")
			K currentKey = (K) leftKeys[y];
			@SuppressWarnings("unchecked")
			V oldValue   = (V) leftValues[y];
			
			assert (oldValue == null) == (currentKey == null);
			
			leftValues[y] = null;
			leftKeys[y] = null;

			if (currentKey != null) {
				size--;
				if (leftMaxIndex == y) {
					int newMaxIndex = y - 1;
					while (newMaxIndex >= 0 && leftKeys[newMaxIndex] == null) newMaxIndex--;
					leftMaxIndex = newMaxIndex;
				}
			}

			return oldValue;
		}
		return null;

	}

	@Override
	public void clear() {
		rightKeys = new Object[1];
		rightValues = new Object[1];
		leftKeys = new Object[1];
		leftValues = new Object[1];
		size = 0;
		rightMaxIndex = -1;
		leftMaxIndex = -1;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	
	/**
	 * make sure we can store to a particular index
	 */
	private void ensureRightCapacity(int capacity) {
		assert rightKeys.length == rightValues.length;
		final int length = rightKeys.length;
		if (capacity >= length) {
			final int newLength = 1 + (int) (GROWTH_FACTOR * capacity);
			{
				Object[] oldKeys = rightKeys;
				rightKeys = new Object[newLength];
				System.arraycopy(oldKeys, 0, rightKeys, 0, length);
				oldKeys = null;
			}
			{			
				Object[] oldValues = rightValues;
				rightValues = new Object[newLength];
				System.arraycopy(oldValues, 0, rightValues, 0, length);
				oldValues = null;
			}
		}
	}
	
	/**
	 * make sure we can store to a particular index
	 */
	private void ensureLeftCapacity(int capacity) {
		assert leftKeys.length == leftValues.length;
		final int length = leftKeys.length;
		if (capacity >= length) {
			final int newLength = 1 + (int) (GROWTH_FACTOR * capacity);
			{
				Object[] oldKeys = leftKeys;
				leftKeys = new Object[newLength];
				System.arraycopy(oldKeys, 0, leftKeys, 0, length);
				oldKeys = null;
			}
			{			
				Object[] oldValues = leftValues;
				leftValues = new Object[newLength];
				System.arraycopy(oldValues, 0, leftValues, 0, length);
				oldValues = null;
			}
		}
	}

	
	class LeftRightIterator<T> implements Iterator<T> {
		private int x = 0;
		private int y = 0;
		
		private final Object[] left;
		private final Object[] right;
		
		LeftRightIterator(Object[] left, Object[] right) {
			assert left  == leftKeys  || left  == leftValues;
			assert right == rightKeys || right == rightValues;
			this.left = left;
			this.right = right;
		}
		
		@Override
		public boolean hasNext() {
			return y <= leftMaxIndex || x <= rightMaxIndex;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T next() {
			if (y <= leftMaxIndex) {
				assert y < leftKeys.length;
				while (left[y] == null) {
					y++;
					assert y < leftKeys.length;
				}
				return (T) left[y++];
			}
			if (x <= rightMaxIndex) {
				assert x < right.length;
				while (right[x] == null) {
					x++;
					assert x < right.length;
				}
				return (T) right[x++];
			}

			throw new NoSuchElementException();
		}
	}

	@Override
	public Set<K> keySet() {
		return KEY_SET;
	}

	private final Set<K> KEY_SET = new AbstractSet<K>() {
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
		public Iterator<K> iterator() {
			return new LeftRightIterator<K>(leftKeys, rightKeys);
		}

		@Override
		public boolean add(K e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			return SimpleVectorBase.this.remove(o) != null;
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	};
	

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
				return new LeftRightIterator<V>(leftValues, rightValues);
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
		return ENTRY_SET;
	}

	private final Set<Entry<K, V>> ENTRY_SET = new AbstractSet<Entry<K, V>>() {
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
		public Iterator<Entry<K, V>> iterator() {
			return new Iterator<Map.Entry<K,V>>() {
				final Iterator<K> keyIterator = new LeftRightIterator<K>(leftKeys, rightKeys);

				@Override
				public boolean hasNext() {
					return keyIterator.hasNext();
				}

				@Override
				public Entry<K, V> next() {
					final K key = keyIterator.next();
					final int id = getId(key);
					return new Entry<K,V>() {
						public K getKey() {
							return key;
						};
						
						@Override
						public V getValue() {
							if (id >= 0) {
								final int x = id;
								@SuppressWarnings("unchecked")
								final V value = (V) rightValues[x]; 
								return value;
							} else {
								final int y = -id;
								assert y != 0;
								@SuppressWarnings("unchecked")
								final V value = (V) leftValues[y]; 
								return value;
							}
							
						}
						
						@Override
						public V setValue(V value) {
							throw new UnsupportedOperationException();
						}
					};
				}
				
			};
					
		}

		@Override
		public boolean add(Entry<K, V> e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			return SimpleVectorBase.this.remove(o) != null;
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	};
	
	
	@Override
	public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		if (key == null) throw new NullPointerException();
		final int id = getId(key);
		if (id > MAX_SIZE || id < -MAX_SIZE) {
			throw new IllegalArgumentException("id is too big: " + id);
		}
		
		if (0 <= id) {
			final int x = id;
			
			if (x < rightKeys.length) {
				@SuppressWarnings("unchecked")
				K currentKey = (K) rightKeys[x];
				@SuppressWarnings("unchecked")
				V oldValue   = (V) rightValues[x];
				
				assert (oldValue == null) == (currentKey == null);
				assert currentKey == null || key.equals(currentKey);

				final V value = remappingFunction.apply(key, oldValue);
				
				if (currentKey == null && value != null) {
					rightKeys[x] = key;
					size++;
					rightMaxIndex = Math.max(rightMaxIndex, x);
				} else if (currentKey != null && value == null){
					size--;
					if (rightMaxIndex == x) {
						int newMaxIndex = x - 1;
						while (newMaxIndex >= 0 && rightKeys[newMaxIndex] == null) newMaxIndex--;
						rightMaxIndex = newMaxIndex;
					}
				}
				rightValues[x] = value;
				
				return value;
			} else {
				final V value = remappingFunction.apply(key, null);
				if (value != null) {
					ensureRightCapacity(x);
					rightKeys[x] = key;
					rightValues[x] = value;
					size++;
					assert Math.max(rightMaxIndex, x) == x;
					rightMaxIndex = x;
				}
				return value;
			}

		} else {
			final int y = -id;
			assert y != 0;
			
			if (y < leftKeys.length) {
				@SuppressWarnings("unchecked")
				K currentKey = (K) leftKeys[y];
				@SuppressWarnings("unchecked")
				V oldValue   = (V) leftValues[y];
				
				assert (oldValue == null) == (currentKey == null);
				assert currentKey == null || key.equals(currentKey);
				
				final V value = remappingFunction.apply(key, oldValue);
				
				if (currentKey == null && value != null) {
					leftKeys[y] = key;
					size++;
					leftMaxIndex = Math.max(leftMaxIndex, y);
				} else if (currentKey != null && value == null){
					size--;
					if (leftMaxIndex == y) {
						int newMaxIndex = y - 1;
						while (newMaxIndex >= 0 && leftKeys[newMaxIndex] == null) newMaxIndex--;
						leftMaxIndex = newMaxIndex;
					}
				}
				leftValues[y] = value;
				
				return value;
			} else {
				final V value = remappingFunction.apply(key, null);
				if (value != null) {
					ensureRightCapacity(y);
					leftKeys[y] = key;
					leftValues[y] = value;
					size++;
					assert Math.max(leftMaxIndex, y) == y;
					leftMaxIndex = y;
				}
				return value;
			}
		}
	}
}
