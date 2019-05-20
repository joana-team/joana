/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

@FunctionalInterface
public interface NodeEvaluator {
	/**
	 * Evaluates the passed node
	 * 
	 * @param node passed node
	 * @return true if the result of the evaluate was the different
	 * compared to the last evaluation
	 */
	public boolean evaluate(SDGNode node);
}
