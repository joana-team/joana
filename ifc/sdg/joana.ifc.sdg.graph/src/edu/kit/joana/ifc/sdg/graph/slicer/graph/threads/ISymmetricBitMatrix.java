/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.threads;

import com.ibm.wala.util.intset.IntIterator;

/**
 * TODO: @author Add your name here.
 */
public interface ISymmetricBitMatrix<T> extends IBitMatrix<T> {
    public IntIterator onColAsymemtric(int j);
}
