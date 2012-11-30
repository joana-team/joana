/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.sdg;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * An SDGNodePredicate decides by some magic criterion, whether a given sdg node is 'interesting'. This is needed for
 * the construction of a reduced control-flow graph. The ReducedCFGBuilder can be configured using a custom
 * SDGNodePredicate to support different reduced views of a control-flow graph.
 * @author Martin Mohr
 *
 */
public interface SDGNodePredicate {
	/**
	 * Returns whether the given node is 'interesting' enough to show in a reduced control-flow graph
	 * @param node node to test
	 * @return {@code true} if node is 'interesting', {@code false} if not
	 */
	public boolean isInteresting(SDGNode node);
}
