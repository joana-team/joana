/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.collections;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Set;
import java.util.Map.Entry;

/**
 * TODO: @author Add your name here.
 */
public final class ModifiableNotTightArraySet<K> extends NotTightArraySet<K> implements Disowning<K>{
	
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
	
	@SuppressWarnings("unchecked")
	public ModifiableNotTightArraySet(Set<K> other, Class<? super K> clazz) {
		this.clazz = clazz;
		this.size = other.size();
		this.keys = other.toArray((K[])Array.newInstance(clazz, size));
		Arrays.sort(keys, ENTRY_COMPARATOR);
	}
	
	protected void incSize() {
		if (size != SIZE_UNKNOWN) size++;
	}

	protected void decSize() {
		if (size != SIZE_UNKNOWN) size--;
	}

	
	@Override
	public boolean add(K key) {
		if (key == null) throw new NullPointerException();
		final int length = keys.length;
		
			final int index = binarySearch0(0, keys.length, key);
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
				
				assert invariant();
				return true;
			}
	
			
//			if (insert == length && size < length) { // maybe do not do this if |length - size| is very small?
//				compact(false);
//				assert length == keys.length;
//				assert (keys[size] == null);
//				keys[size] = key;
//				assert maxIndex == size - 1;
//				maxIndex = size;
//				size++;
//				assert invariant();
//				return true;
//			}
			
			if (length == 0 || keys[length -1] != null) { // maybe even do this a little sooner?
				final int newLength = (int) (GROWTH_FACTOR * (length + 3));
				
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
				incSize();
				
				assert invariant();
				return true;
			}
		

		assert 0 <= insert && insert < keys.length;

		System.arraycopy(keys,   insert, keys,   insert + 1, length - insert - 1);
		keys[insert] = key;
		incSize();
		

		
		assert invariant();
		return true;
	}

	@Override
	public boolean remove(Object o) {
		assert invariant();
		if (o == null) throw new NullPointerException();
		
		final int remove = binarySearch0(0, keys.length, o);
		if (remove < 0) return false;
		
		assert (remove < keys.length);
		assert (keys.length > 0);
		
		assert keys[remove] != null;
		
		keys[remove] = null;
		
		decSize();
		
		assert invariant();
		
		return true;
	}
	
	
	@Override
	public void clear() {
		keys   = new Object[0];
		size = 0;
		assert invariant();
		return;
	}

	
	
	@SuppressWarnings("unchecked")
	public static <K> ModifiableNotTightArraySet<K> own(Object[] elements, Class<? super K> clazz) {
		if (elements == null) return new ModifiableNotTightArraySet<>((K[]) empty, clazz);
		return new ModifiableNotTightArraySet<>((K[])elements, clazz);
	}
}
