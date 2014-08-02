/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

/**
 * callback interface to tell the world about some news about SDG construction
 * @author Martin Mohr
 */
public interface ConstructionNotifier {
	/** 
	 * this method is called right before SDG construction has started.
	 */
	void sdgStarted();
	/** 
	 * this method is called right after SDG construction has finished.
	 */
	void sdgFinished();
	/** 
	 * this method is called right after SDG construction has finished. The parameters passed are
	 * the number of nodes in the call graph of the analysed program, before and after pruning
	 */
	void numberOfCGNodes(int numberUnpruned, int numberPruned);
	/**
	 * this method is called if control dependencies are to be stripped, right before that procedure.
	 */
	void stripControlDepsStarted();
	/**
	 * this method is called if control dependencies are to be stripped, right after that procedure.
	 */
	void stripControlDepsFinished();
}
