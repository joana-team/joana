/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.threads;

/**
 * TODO: @author Add your name here.
 */
public interface IMutableBitMatrix<T> extends IBitMatrix<T> {
	
	void set(int i, int j);

}
