/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.dataflow;

import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.wala.core.params.objgraph.dataflow.ModRefControlFlowGraph.Node;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public interface ModRefProvider {

	/**
	 * This has to be a definitely mod (must-alias) as we use it for killing definitions. Currently the nodes
	 * only kill themselves, so this will mostly return a bitvector with a single element set.
	 */
	IntSet getMustMod(Node node);

	/**
	 * This returns the set of all nodes in inNodes (via: mapping) that define values that the given node may read. Thus they are may-aliasing.
	 */
	IntSet getMayRef(Node node, IntSet inNodes, final OrdinalSetMapping<Node> mapping);

}
