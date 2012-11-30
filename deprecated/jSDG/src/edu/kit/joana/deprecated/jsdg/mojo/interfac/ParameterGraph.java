/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.mojo.interfac;

import com.ibm.wala.util.graph.impl.SparseNumberedGraph;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ParameterGraph extends SparseNumberedGraph<GraphNode> {

	private final GraphNode root;

	public ParameterGraph(GraphNode root) {
		this.root = root;
		addNode(root);
	}

	public GraphNode getRoot() {
		return root;
	}

	public String toString() {
		return "#(" + root + ")";
	}

}
