/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.util;

import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.intset.IntSet;

/**
 * A sub graph can be used to create a view of a given graph that only includes
 * a set of predefined nodes.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class SubGraph<T> extends AbstractNumberedGraph<T> {

	private final NumberedEdgeManager<T> edges;
	private final NumberedNodeManager<T> nodes;


	/**
	 * Creates a new sub graph from a given graph that only includes nodes from a
	 * given set of nodes.
	 * @param delegate The graph a subgraph is created from.
	 * @param nodesOk The set of nodes that should be included in the subgraph.
	 * May not contain nodes that are not part of the graph <tt>delegate</tt>.
	 */
	public SubGraph(NumberedGraph<T> delegate, IntSet nodesOk) {
		if (delegate == null) {
			throw new IllegalArgumentException();
		} else if (nodesOk == null) {
			throw new IllegalArgumentException();
		}

		nodes = new FilteredNodeManager<T>(delegate, nodesOk);
		edges = new FilteredEdgeManager<T>(delegate, nodesOk);
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.AbstractGraph#getEdgeManager()
	 */
	@Override
	protected NumberedEdgeManager<T> getEdgeManager() {
		return edges;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.AbstractGraph#getNodeManager()
	 */
	@Override
	protected NumberedNodeManager<T> getNodeManager() {
		return nodes;
	}

}
