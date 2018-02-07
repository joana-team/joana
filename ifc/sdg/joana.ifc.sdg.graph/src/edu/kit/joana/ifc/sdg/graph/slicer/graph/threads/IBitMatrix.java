/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.threads;

import java.util.Arrays;

/**
 * TODO: @author Add your name here.
 */
public interface IBitMatrix<T> {

	/**
	 * @param i row offset
	 * @param j column offset
	 * @return value of given bit in matrix
	 */
	boolean get(int i, int j);
	
	int getDimension();
	
    public static <T> boolean equals(IBitMatrix<T> a, IBitMatrix<T> b) {
    	if (a.getDimension() != b.getDimension()) return false;
    	for (int i = 0; i < a.getDimension(); i++) {
    		for (int j = 0; j < a.getDimension(); j++) {
    			if (a.get(i,j) != b.get(i,j)) {
    				return false;
    			}
    		}
    	}
    	return true;
    }
    
    public static <T> boolean leq(IBitMatrix<T> a, IBitMatrix<T> b) {
    	if (a.getDimension() != b.getDimension()) return false;
    	for (int i = 0; i < a.getDimension(); i++) {
    		for (int j = 0; j < a.getDimension(); j++) {
    			if (a.get(i,j) && !b.get(i,j)) {
    				return false;
    			}
    		}
    	}
    	return true;
    }

}