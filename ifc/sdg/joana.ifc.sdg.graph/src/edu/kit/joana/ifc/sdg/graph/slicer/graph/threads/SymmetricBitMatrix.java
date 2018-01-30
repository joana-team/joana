/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.threads;

import com.ibm.wala.util.intset.IntIterator;

/**
 * <p>Represents a square matrix of bits. In function arguments below, i is the row position
 * and j the column position of a bit. The top left bit corresponds to i = 0 and j = 0.</p>
 *
 * <p>Internally the bits are represented in a compact 1-D array of 32-bit ints. The
 * ordering of bits is column-major; that is the bits in this array correspond to
 * j=0 and i=0..dimension-1 first, then j=1 and i=0..dimension-1, etc.</p>
 *
 * <p>Within each int, less-significant bits correspond to lower values of i and higher rows.
 * That is, the top-left bit is the least significant bit of the first int.</p>
 *
 * <p>This class is a convenient wrapper around this representation, but also exposes the internal
 * array for efficient access and manipulation.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class SymmetricBitMatrix {

    private final int dimension;
    private final int[] bits;
    
    protected final static int LOW_MASK = 0x1f;
    protected final static int BITS_PER_UNIT = 32;
    protected final static int LOG_BITS_PER_UNIT = 5;

    public SymmetricBitMatrix(int dimension) {
        if (dimension < 1) {
            throw new IllegalArgumentException("dimension must be at least 1");
        }
        this.dimension = dimension;
        final long n = (long) dimension;
        assert (n * (n + 1)) % 2 == 0;
        final long numBits = (n * (n + 1)) / 2; 
        long arraySize = numBits >> LOG_BITS_PER_UNIT; // one int per 32 bits
        if ((numBits & LOW_MASK) != 0) { // plus one more if there are leftovers
            arraySize++;
        }
        assert arraySize > 0;
        if ((int) arraySize > Integer.MAX_VALUE) {
        	throw new IllegalArgumentException("dimension is too large");
        }
        bits = new int[(int)arraySize];
    }

    /**
     * @param i row offset
     * @param j column offset
     * @return value of given bit in matrix
     */
    public boolean get(int i, int j) {
        long m = (long) ((i <= j) ? i : j);
        long n = (long) ((i <= j) ? j : i);
        assert (n * (n + 1)) % 2 == 0;
        long offset = m + (n * (n + 1))/2;
        int index = (int) (offset >> LOG_BITS_PER_UNIT);
        return ((bits[index] >>> (offset & LOW_MASK)) & 0x01) != 0;
    }

    /**
     * <p>Sets the given bit to true.</p>
     *
     * @param i row offset
     * @param j column offset
     */
    public void set(int i, int j) {
        long m = (long) ((i <= j) ? i : j);
        long n = (long) ((i <= j) ? j : i);
        assert (n * (n + 1)) % 2 == 0;
        long offset = m + (n * (n + 1))/2;
        int index = (int) (offset >> LOG_BITS_PER_UNIT);
        bits[index] |= 1 << (offset & LOW_MASK);
    }

    /**
     * <p>Flips the given bit.</p>
     *
     * @param i row offset
     * @param j column offset
     */
    public void clear(int i, int j) {
        long m = (long) ((i <= j) ? i : j);
        long n = (long) ((i <= j) ? j : i);
        assert (n * (n + 1)) % 2 == 0;
        long offset = m + (n * (n + 1))/2;
        int index = (int) (offset >> LOG_BITS_PER_UNIT);
        bits[index] &= ~(1 << (offset & LOW_MASK));
    }

    public String toString() {
        StringBuffer result = new StringBuffer(dimension * (dimension + 1));
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                result.append(get(i, j) ? '1' : '0');
            }
            result.append('\n');
        }
        return result.toString();
    }
    
    // TODO: presumably, the array accesses in this iteration scheme can be strength-reduced
    public IntIterator onCol(int j) {
        return new IntIterator() {
            int n = j;
            int m = 0;
            boolean inColMode = m < n;

            @Override
            public int next() {
            	findNext();
            	return (inColMode) ? m++ : n++;
            }

            @Override
            public boolean hasNext() {
            	return findNext();
            }
            
            boolean findNext() {
            	boolean found = false;
            	while (inColMode && m < n         && !(found = get(m, n))) m++;
            	if (found) return found;
            	inColMode = false;
            	while (n < dimension && !(found = get(m, n))) {
            		n++;
            	}
            	return found;
            }
        };
    }
    
    public IntIterator onColAsymemtric(int j) {
        return new IntIterator() {
            final int n = j;
            final long offset0 = (n * (n + 1))/2;
            long offset = offset0;

            @Override
            public int next() {
            	findNext();
            	return (int) ((offset++) - offset0);
            }

            @Override
            public boolean hasNext() {
            	return findNext();
            }
            
            boolean findNext() {
            	offset = nextSetBit(offset);
            	final boolean found = !(offset == -1 || offset > offset0 + n); 
            	return found;
            }
        };
    }
    
    // taken from com.ibm.wala.util.intset.BitVectorBase
    public long nextSetBit(long start) {
        if (start < 0) {
          throw new IllegalArgumentException("illegal start: " + start);
        }
        int word = (int) (start >> LOG_BITS_PER_UNIT);
        if (word >= bits.length) {
          return -1;
        }
        int shift = (int) (start & LOW_MASK);
        int w = bits[word] >> shift;
        if (w != 0) {
          return start + Long.numberOfTrailingZeros(w);
        }
        start = (start + BITS_PER_UNIT) & ~LOW_MASK;
        word++;
        while (word < bits.length) {
          if (bits[word] != 0) {
            return start + Long.numberOfTrailingZeros(bits[word]);
          } else {
            start += BITS_PER_UNIT;
          }

          word++;
        }

        return -1;
      }
    
    
    
}
