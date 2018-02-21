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
//TODO: replace uses of BitMatrix with SummetricBitMatrix wherever appropriate
public final class BitMatrix {

    private final int dimension;
    private final int[] bits;

    public BitMatrix(int dimension) {
        if (dimension < 1) {
            throw new IllegalArgumentException("dimension must be at least 1");
        }
        this.dimension = dimension;
        long numBits = ((long)dimension) * ((long)dimension);
        long arraySize = numBits >> 5; // one int per 32 bits
        if ((numBits & 0x1F) != 0) { // plus one more if there are leftovers
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
        long offset = ((long)i) + ((long)dimension) * ((long)j);
        int index = (int) (offset >> 5);
        return ((bits[index] >>> (offset & 0x1F)) & 0x01) != 0;
    }

    /**
     * <p>Sets the given bit to true.</p>
     *
     * @param i row offset
     * @param j column offset
     */
    public void set(int i, int j) {
        long offset = ((long)i) + ((long)dimension) * ((long)j);
        int index = (int) (offset >> 5);
        bits[index] |= 1 << (offset & 0x1F);
    }

    /**
     * <p>Flips the given bit.</p>
     *
     * @param i row offset
     * @param j column offset
     */
    public void clear(int i, int j) {
        long offset = ((long)i) + ((long)dimension) * ((long)j);
        int index = (int) (offset >> 5);
        bits[index] &= ~(1 << (offset & 0x1F));
    }

    /**
     * <p>Sets a square region of the bit matrix to true.</p>
     *
     * @param topI row offset of region's top-left corner (inclusive)
     * @param leftJ column offset of region's top-left corner (inclusive)
     * @param height height of region
     * @param width width of region
     */
    public void setRegion(int topI, int leftJ, int height, int width) {
        if (topI < 0 || leftJ < 0) {
            throw new IllegalArgumentException("topI and leftJ must be nonnegative");
        }
        if (height < 1 || width < 1) {
            throw new IllegalArgumentException("height and width must be at least 1");
        }
        int maxJ = leftJ + width;
        int maxI = topI + height;
        if (maxI > dimension || maxJ > dimension) {
            throw new IllegalArgumentException(
                    "topI + height and leftJ + width must be <= matrix dimension");
        }
        for (int j = leftJ; j < maxJ; j++) {
            long jOffset = dimension * j;
            for (int i = topI; i < maxI; i++) {
                long offset = i + jOffset;
                int index = (int) (offset >> 5);
                bits[index] |= 1 << (offset & 0x1F);
            }
        }
    }

    /**
     * @return row/column dimension of this matrix
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * @return array of ints holding internal representation of this matrix's bits
     */
    public int[] getBits() {
        return bits;
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

    public static void main(String[] args) {
    	BitMatrix m = new BitMatrix(4);
    	m.set(1, 1);
    	System.out.println(m);
    	m.set(1, 1);
    	System.out.println(m);
    }
}
