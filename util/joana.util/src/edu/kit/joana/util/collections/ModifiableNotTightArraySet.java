/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.collections;

import java.lang.reflect.Array;
import java.util.Set;

import javax.imageio.metadata.IIOInvalidTreeException;

/**
 * TODO: @author Add your name here.
 */
public final class ModifiableNotTightArraySet<K> extends NotTightArraySet<K> {
	
	private final static double GROWTH_FACTOR = 1.5;
	
	private final Class<? super K> clazz;
	
	public ModifiableNotTightArraySet(int capacity, Class<? super K> clazz) {
		super(capacity);
		this.clazz = clazz;
	}
	
	private ModifiableNotTightArraySet(K[] entries, Class<? super K> clazz) {
		super(entries);
		this.clazz = clazz;
	}
	
	public ModifiableNotTightArraySet(Set<K> other, Class<? super K> clazz) {
		super(other);
		this.clazz = clazz;
	}

	@Override
	public boolean add(K key) {
		if (key == null) throw new NullPointerException();
		final int length = keys.length;
		
		{
			final int index = binarySearch0(0, maxIndex + 1, key);
			if (index >= 0)  {
				@SuppressWarnings("unchecked")
				final K oldKey = (K) keys[index];
				assert oldKey != null;
				assert oldKey.equals(key);
				
				return false;
			}
	
			final int insert = -index - 1;
			assert insert >= 0;
	
			if (insert != length && keys[insert] == null) {
				keys[insert] = key;
				maxIndex = Math.max(maxIndex, insert);
				size++;
				
				assert invariant();
				return true;
			}
	
			if (insert == length && size < length) { // maybe do not do this if |length - size| is very small?
				compact(false);
				assert length == keys.length;
				assert (keys[size] == null);
				keys[size] = key;
				assert maxIndex == size - 1;
				maxIndex = size;
				size++;
				assert invariant();
				return true;
			}
			
			if (size == length) { // maybe even do this a little sooner?
				final int newLength = (int) (GROWTH_FACTOR * (length + 1));
				
				@SuppressWarnings("unchecked")
				K[] newKeys   = (K[])  Array.newInstance(clazz, newLength);
				if (insert > 0) {
					System.arraycopy(keys,   0, newKeys,   0, insert);
				}
				if (insert < length) {
					System.arraycopy(keys,   insert, newKeys,   insert + 1, length - insert);
				}
				newKeys[insert] = key;

				this.keys   = newKeys;
				maxIndex = maxIndex + 1;
				size++;
				
				isCompact = true;
				
				assert invariant();
				return true;
			}
		}
		
		assert size < length;
		compact(false);
		assert length == keys.length;
		assert keys[length - 1] == null;
		final int index = binarySearch0(0, size, key);

		final int insert = -index - 1;
		assert 0 <= insert && insert < length;

		System.arraycopy(keys,   insert, keys,   insert + 1, length - insert - 1);
		keys[insert] = key;
		assert maxIndex == size - 1;
		maxIndex = size;
		size++;
		

		
		assert invariant();
		return true;
	}

	@Override
	public boolean remove(Object o) {
		assert invariant();
		if (o == null) throw new NullPointerException();
		
		final int remove = binarySearch0(0, maxIndex + 1, o);
		if (remove < 0) return false;
		
		assert (remove < keys.length);
		assert (keys.length > 0);
		
		assert keys[remove] != null;
		
		keys[remove] = null;
		
		if (remove == maxIndex) {
			maxIndex--;
			for (int i = maxIndex; i > 0 && keys[i] == null; i--) {
				maxIndex--;
			}
		} else {
			isCompact = false;
		}
		size--;
		
		assert invariant();
		
		if (compactEnabled) compact(true);
		return true;
	}
	
	public void trimToSize() {
		compact(true);
	}
	
	private void compact(boolean shorten) {
		assert invariant();
		final int length = keys.length;
		

		if (isCompact) {
			if (shorten) {
				Object[] newKeys   = shorten ? new Object[size] : keys;
				
				System.arraycopy(keys,   0, newKeys,    0, size);
				
				keys   = newKeys;
			}
			return;
		}
		
		
		Object[] newKeys   = shorten ? new Object[size] : keys;
		
		/*
		int k = 0;
		int i = 0;

		while (true) {
			while (i < length && keys[i] == null) i++;
			if (i == length)  {
				this.keys = newKeys;
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
			k += blockLength;
			
			i = j;
		}
		*/
		int i = 0;
		int j = 0;
		Object k = null;
		while (i < length) {
			while (i < length && (k = keys[i++]) != null) {
				newKeys[j++] = k;
			}
			while (i < length && (k = keys[i]) == null) i++; 
		}
		
		maxIndex = j-1;
		if (keys == newKeys) {
			for (; j < length; j++) {
				keys[j] = null;
			}
		}
		this.keys = newKeys;
		
		isCompact = true;
		assert invariant();

	}
	
	@Override
	public void clear() {
		keys   = new Object[0];
		size = 0;
		isCompact = true;
		this.maxIndex = -1;
		
		assert invariant();
		return;
	}

	
	
	@SuppressWarnings("unchecked")
	public static <K> ModifiableNotTightArraySet<K> own(Object[] elements, Class<? super K> clazz) {
		if (elements == null) return new ModifiableNotTightArraySet<>((K[]) empty, clazz);
		return new ModifiableNotTightArraySet<>((K[])elements, clazz);
	}
}
