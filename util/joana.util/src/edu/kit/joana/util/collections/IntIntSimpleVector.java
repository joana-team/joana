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
 * A {@link Map} implementation for {@link Integer} keys and values.
 * 
 * This backed by a array, directly accessed by {@link IntegerIdentifiable#getId()},
 * hence this only makes sense for keys that are densely aligned around 0, i.e.: 
 * almost all keys have and id that contained in an interval [-leftMaxIndex, rightMaxIndex] for
 * some leftMaxIndex, rightMaxIndex >=0.
 * 
 */
public class IntIntSimpleVector extends AbstractMap<Integer, Integer> implements Map<Integer, Integer> {

	public static final int NULL = Integer.MIN_VALUE;
	private final static int MAX_SIZE = Integer.MAX_VALUE / 4;

	private final static double GROWTH_FACTOR = 1.5;

	protected int[] rightValues;
	
	protected int[] leftValues;

	protected int rightMaxIndex;
	protected int leftMaxIndex;
	
	protected int size;

	public IntIntSimpleVector(int initialLeftCapacity, int initialRightCapacity) {
		if (initialLeftCapacity < 0  || initialLeftCapacity  > MAX_SIZE) throw new IllegalArgumentException(); 
		if (initialRightCapacity < 0 || initialRightCapacity > MAX_SIZE) throw new IllegalArgumentException();

		this.rightMaxIndex = -1;
		this.leftMaxIndex  = -1;
		this.size = 0;
		this.rightValues = new int[initialRightCapacity];
		this.leftValues  = new int[initialLeftCapacity];
		for (int i = 0; i < leftValues.length; i++) {
			leftValues[i] = NULL;
		}
		for (int i = 0; i < rightValues.length; i++) {
			rightValues[i] = NULL;
		}
	}
	
	public void trimToSize() {
		{
			int[] oldRightValues = rightValues;
			rightValues = new int[rightMaxIndex + 1];
			System.arraycopy(oldRightValues, 0, rightValues, 0, rightValues.length);
			oldRightValues = null;
		}
		{
			int[] oldLeftValues = leftValues;
			leftValues = new int[leftMaxIndex + 1];
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
	
	@Override
	public Integer get(Object o) {
		if (o == null) throw new NullPointerException();
		if (!(o instanceof Integer)) return null;
		final int x = (int) o;

		if (0 <= x && x <= rightMaxIndex) {
			return rightValues[x];
		}
		
		final int y = -x;
		assert y != 0;
		if (0 <= y && y <= leftMaxIndex) {
			return leftValues[y];
		}
		return null;
	}
	
	@Override
	public boolean containsKey(Object o) {
		if (o == null) throw new NullPointerException();
		if (!(o instanceof Integer)) return false;
		final int x = (int) o;

		if (0 <= x && x <= rightMaxIndex && rightValues[x] != NULL) {
			return true;
		}
		
		final int y = -x;
		if (0 <= y && y <= leftMaxIndex  && leftValues[y] != NULL) {
			return true;
		}
		
		return false;
	}

	@Override
	public Integer put(Integer key, Integer value) {
		if (key == null || value == null || value == NULL) throw new NullPointerException();
		final int id = key;
		if (id > MAX_SIZE || id < -MAX_SIZE) {
			throw new IllegalArgumentException("id is too big: " + id);
		}
		
		if (0 <= id) {
			final int x = id;
			ensureRightCapacity(x);

			int oldValue   = rightValues[x];
			
			
			if (oldValue == NULL) {
				size++;
				rightMaxIndex = Math.max(rightMaxIndex, x);
			}
			rightValues[x] = value;
			
			return oldValue == NULL ? null : oldValue;
		} else {
			final int y = -id;
			assert y != 0;
			
			ensureLeftCapacity(y);
			
			int oldValue   = leftValues[y];
			
			if (oldValue == NULL) {
				size++;
				leftMaxIndex = Math.max(leftMaxIndex, y);
			}

			leftValues[y] = value;
			
			return oldValue == NULL ? null : oldValue;
		}
	}

	@Override
	public Integer remove(Object o) {
		if (o == null) throw new NullPointerException();
		if (!(o instanceof Integer)) return null;
		final int x = (int) o;
		final int y = -x;
		
		if (0 <= x && x <= rightMaxIndex) {
			int oldValue   = rightValues[x];
			
			rightValues[x] = NULL;

			if (oldValue != NULL) {
				size--;
				if (rightMaxIndex == x) {
					int newMaxIndex = x - 1;
					while (newMaxIndex >= 0 && rightValues[newMaxIndex] == NULL) newMaxIndex--;
					rightMaxIndex = newMaxIndex;
				}
			}

			return oldValue;
		} else if (0 <= y && y <= leftMaxIndex) {
			int oldValue   = leftValues[y];
			
			leftValues[y] = NULL;

			if (oldValue != NULL) {
				size--;
				if (leftMaxIndex == y) {
					int newMaxIndex = y - 1;
					while (newMaxIndex >= 0 && leftValues[newMaxIndex] == NULL) newMaxIndex--;
					leftMaxIndex = newMaxIndex;
				}
			}

			return oldValue;
		}
		return null;

	}

	@Override
	public void clear() {
		rightValues = new int[1];
		leftValues = new int[1];
		rightValues[0] = NULL;
		leftValues[0] = NULL;
		size = 0;
		rightMaxIndex = -1;
		leftMaxIndex = -1;
	}

	@Override
	public void putAll(Map<? extends Integer, ? extends Integer> m) {
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
		final int length = rightValues.length;
		if (capacity >= length) {
			final int newLength = 1 + (int) (GROWTH_FACTOR * capacity);
			{			
				int[] oldValues = rightValues;
				rightValues = new int[newLength];
				System.arraycopy(oldValues, 0, rightValues, 0, length);
				for (int i = length; i < newLength; i ++) {
					rightValues[i] = NULL;
				}
				oldValues = null;
			}
		}
	}
	
	/**
	 * make sure we can store to a particular index
	 */
	private void ensureLeftCapacity(int capacity) {
		final int length = leftValues.length;
		if (capacity >= length) {
			final int newLength = 1 + (int) (GROWTH_FACTOR * capacity);
			{			
				int[] oldValues = leftValues;
				leftValues = new int[newLength];
				System.arraycopy(oldValues, 0, leftValues, 0, length);
				for (int i = length; i < newLength; i ++) {
					leftValues[i] = NULL;
				}
				oldValues = null;
			}
		}
	}

	
	abstract class LeftRightIterator implements Iterator<Integer> {
		private int x = 0;
		private int y = 0;
		
		private final int[] left;
		private final int[] right;
		
		LeftRightIterator(int[] left, int[] right) {
			assert left  == leftValues;
			assert right == rightValues;
			this.left = left;
			this.right = right;
		}
		
		@Override
		public boolean hasNext() {
			return y <= leftMaxIndex || x <= rightMaxIndex;
		}

		@Override
		public Integer next() {
			if (y <= leftMaxIndex) {
				assert y < left.length;
				while (left[y] == NULL) {
					y++;
					assert y < leftValues.length;
				}
				//return left[y++];
				return what(left, y++);
			}
			if (x <= rightMaxIndex) {
				assert x < right.length;
				while (right[x] == NULL) {
					x++;
					assert x < right.length;
				}
				//return right[x++];
				return what(right, x++);
			}

			throw new NoSuchElementException();
		}
		
		abstract int what(int[] array, int index);
	}

	@Override
	public Set<Integer> keySet() {
		return KEY_SET;
	}

	private final Set<Integer> KEY_SET = new AbstractSet<Integer>() {
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
		public Iterator<Integer> iterator() {
			return new LeftRightIterator(leftValues, rightValues) {
				int what(int[] array, int index) {
					return index;
				}
			};
		}

		@Override
		public boolean add(Integer e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			return IntIntSimpleVector.this.remove(o) != null;
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	};
	

	@Override
	public Collection<Integer> values() {
		return new AbstractCollection<Integer>() {

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
			public Iterator<Integer> iterator() {
				return new LeftRightIterator(leftValues, rightValues) {
					@Override
					int what(int[] array, int index) {
						return array[index];
					}
				};
			}

			@Override
			public boolean add(Integer e) {
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
	public Set<Entry<Integer, Integer>> entrySet() {
		return ENTRY_SET;
	}

	private final Set<Entry<Integer, Integer>> ENTRY_SET = new AbstractSet<Entry<Integer, Integer>>() {
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
		public Iterator<Entry<Integer, Integer>> iterator() {
			return new Iterator<Map.Entry<Integer,Integer>>() {
				final Iterator<Integer> keyIterator = new LeftRightIterator(leftValues, rightValues) {
					int what(int[] array, int index) {
						return index;
					};
				};

				@Override
				public boolean hasNext() {
					return keyIterator.hasNext();
				}

				@Override
				public Entry<Integer, Integer> next() {
					final Integer key = keyIterator.next();
					final int id = key;
					return new Entry<Integer, Integer>() {
						public Integer getKey() {
							return key;
						};
						
						@Override
						public Integer getValue() {
							if (id >= 0) {
								final int x = id;
								final int value = rightValues[x]; 
								return value;
							} else {
								final int y = -id;
								assert y != 0;
								final int value = leftValues[y]; 
								return value;
							}
							
						}
						
						@Override
						public Integer setValue(Integer value) {
							throw new UnsupportedOperationException();
						}
					};
				}
				
			};
					
		}

		@Override
		public boolean add(Entry<Integer, Integer> e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			return IntIntSimpleVector.this.remove(o) != null;
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	};
	
	
	@Override
	public Integer compute(Integer key, BiFunction<? super Integer, ? super Integer, ? extends Integer> remappingFunction) {
		if (key == null) throw new NullPointerException();
		final int id = (int) key;
		if (id > MAX_SIZE || id < -MAX_SIZE) {
			throw new IllegalArgumentException("id is too big: " + id);
		}
		
		if (0 <= id) {
			final int x = id;
			
			if (x < rightValues.length) {
				int oldValue   = rightValues[x];
				
				final Integer value = remappingFunction.apply(key, oldValue);
				
				if (oldValue == NULL && value != NULL && value != null) {
					size++;
					rightMaxIndex = Math.max(rightMaxIndex, x);
				} else if (oldValue != NULL && (value == null || value == NULL)){
					size--;
					if (rightMaxIndex == x) {
						int newMaxIndex = x - 1;
						while (newMaxIndex >= 0 && rightValues[newMaxIndex] == NULL) newMaxIndex--;
						rightMaxIndex = newMaxIndex;
					}
				}
				rightValues[x] = value;
				
				return value;
			} else {
				final Integer value = remappingFunction.apply(key, null);
				if (value != null && value != NULL) {
					ensureRightCapacity(x);
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
			
			if (y < leftValues.length) {
				Integer oldValue   = leftValues[y];
				
				final Integer value = remappingFunction.apply(key, oldValue);
				
				if (oldValue == NULL && value != null && value != NULL) {
					size++;
					leftMaxIndex = Math.max(leftMaxIndex, y);
				} else if (oldValue != null && (value == null || value == NULL)){
					size--;
					if (leftMaxIndex == y) {
						int newMaxIndex = y - 1;
						while (newMaxIndex >= 0 && leftValues[newMaxIndex] == NULL) newMaxIndex--;
						leftMaxIndex = newMaxIndex;
					}
				}
				leftValues[y] = value;
				
				return value;
			} else {
				final Integer value = remappingFunction.apply(key, null);
				if (value != null && value != NULL) {
					ensureRightCapacity(y);
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
