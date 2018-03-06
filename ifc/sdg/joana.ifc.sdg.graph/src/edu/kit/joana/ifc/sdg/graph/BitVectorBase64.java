/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package edu.kit.joana.ifc.sdg.graph;

import java.io.Serializable;

import com.ibm.wala.util.intset.IntIterator;

/**
 * Abstract base class for implementations of bitvectors
 */
@SuppressWarnings("rawtypes")
abstract public class BitVectorBase64<T extends BitVectorBase64> implements Cloneable, Serializable {

	private static final long serialVersionUID = 6616478949492760615L;

	protected final static int LOG_BITS_PER_UNIT = 6;

	protected final static int BITS_PER_UNIT = 64;

	protected final static long MASK = 0xffffffffffffffffL;

	protected final static long LOW_MASK = 0x3f;

	protected long bits[];

	public abstract void set(int bit);

	public abstract void clear(int bit);

	public abstract boolean get(int bit);

	public abstract int length();

	public abstract void and(T other);

	public abstract void andNot(T other);

	public abstract void or(T other);

	public abstract void xor(T other);

	public abstract boolean sameBits(T other);

	public abstract boolean isSubset(T other);

	public abstract boolean intersectionEmpty(T other);

	/**
	 * Convert bitIndex to a subscript into the bits[] array.
	 */
	public static int subscript(int bitIndex) {
		return bitIndex >> LOG_BITS_PER_UNIT;
	}

	/**
	 * Clears all bits.
	 */
	public final void clearAll() {
		for (int i = 0; i < bits.length; i++) {
			bits[i] = 0;
		}
	}

	@Override
	public int hashCode() {
		int h = 1234;
		for (int i = bits.length - 1; i >= 0;) {
			h ^= bits[i] * (i + 1);
			i--;
		}
		return h;
	}

	/**
	 * How many bits are set?
	 */
	public final int populationCount() {
		int count = 0;
		for (int i = 0; i < bits.length; i++) {
			count += Bits.populationCount(bits[i]);
		}
		return count;
	}

	public boolean isZero() {
		int setLength = bits.length;
		for (int i = setLength - 1; i >= 0;) {
			if (bits[i] != 0)
				return false;
			i--;
		}
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object clone() {
		BitVectorBase64<T> result = null;
		try {
			result = (BitVectorBase64<T>) super.clone();
		} catch (CloneNotSupportedException e) {
			// this shouldn't happen, since we are Cloneable
			throw new InternalError();
		}
		result.bits = new long[bits.length];
		System.arraycopy(bits, 0, result.bits, 0, result.bits.length);
		return result;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		boolean needSeparator = false;
		buffer.append('{');
		int limit = length();
		for (int i = 0; i < limit; i++) {
			if (get(i)) {
				if (needSeparator) {
					buffer.append(", ");
				} else {
					needSeparator = true;
				}
				buffer.append(i);
			}
		}
		buffer.append('}');
		return buffer.toString();
	}

	/*
	 * @see com.ibm.wala.util.intset.IntSet#contains(int)
	 */
	public boolean contains(int i) {
		return get(i);
	}


	public int max() {
		int lastWord = bits.length - 1;

		while (lastWord >= 0 && bits[lastWord] == 0)
			lastWord--;

		if (lastWord < 0)
			return -1;

		int count = lastWord << LOG_BITS_PER_UNIT;
		

		long top = bits[lastWord];
		
		assert top != 0;
		return count + BITS_PER_UNIT - Long.numberOfLeadingZeros(top) - 1;
	}

	/**
	 * @return min j >= start s.t get(j)
	 */
	public int nextSetBit(int start) {
		if (start < 0) {
			throw new IllegalArgumentException("illegal start: " + start);
		}
		int word = subscript(start);
		long bitset = bits[word];
		if (bitset != 0) {
			int relevantBit = (start & (int)LOW_MASK);
			long t = bitset >>> relevantBit;
			if (t != 0) {
				int nextSetBit = Long.numberOfTrailingZeros(t);
				return start + nextSetBit;
			}
		}
		
		word++;
		start = word << LOG_BITS_PER_UNIT;
		
		while (word < bits.length) {
			bitset = bits[word];
			if (bitset != 0) {
				int nextSetBit = Long.numberOfTrailingZeros(bitset);
				return start + nextSetBit;
			}
			word++;
			start = word << LOG_BITS_PER_UNIT;
		}

		return -1;
	}
	
	/**
	 * via: https://lemire.me/blog/2018/02/21/iterating-over-set-bits-quickly/
	 * @return
	 */
	public IntIterator intIterator() {
		assert bits.length > 0;
		return new IntIterator() {
			int k = 0;
			long bitset = bits[k];
			
			@Override
			public int next() {
				assert bitset != 0;
				final long bs = bitset;
				final long t = Long.lowestOneBit(bs);
				final long r = Long.numberOfTrailingZeros(bs);
				final long result = (k << LOG_BITS_PER_UNIT) + r;
				bitset ^= t;
				assert result <= Integer.MAX_VALUE;
				return (int) result;
			}
			
			@Override
			public boolean hasNext() {
				if (bitset != 0) return true;
				while (++k < bits.length) {
					long bitset = bits[k];
					if (bitset != 0) {
						this.bitset = bitset;
						return true;
					}
				}
				return false;
			}
		};
	}

	/**
	 * Copies the values of the bits in the specified set into this set.
	 * 
	 * @param set
	 *            the bit set to copy the bits from
	 * @throws IllegalArgumentException
	 *             if set is null
	 */
	public void copyBits(BitVectorBase64 set) {
		if (set == null) {
			throw new IllegalArgumentException("set is null");
		}
		int setLength = set.bits.length;
		bits = new long[setLength];
		for (int i = setLength - 1; i >= 0;) {
			bits[i] = set.bits[i];
			i--;
		}
	}
	
	public static void main(String[] args) {
		BitVector64 bv = new BitVector64();
		System.out.println(bv.max());
		bv.set(0);
		System.out.println(bv.max());
		bv.set(3);
		System.out.println(bv.max());
		bv.set(5);
		System.out.println(bv.max());
		bv.set(31);
		System.out.println(bv.max());
		bv.set(32);
		System.out.println(bv.max());
		bv.set(33);
		System.out.println(bv.max());
		bv.set(63);
		System.out.println(bv.max());
		bv.set(64);
		System.out.println(bv.max());
		bv.set(65);
		System.out.println(bv.max());
		bv.set(18932);
		System.out.println(bv.max());
		bv.set(53223);
		System.out.println(bv.max());
		
		IntIterator it = bv.intIterator();
		while (it.hasNext()) {
			int next = it.next();
			System.out.println(next);
			System.out.println(bv.get(next));
		}
		
		int nextBit = -1;
		while ((nextBit = bv.nextSetBit(nextBit + 1)) != -1) {
			System.out.println(nextBit);
			System.out.println(bv.get(nextBit));
		}
		
		
	}
}
