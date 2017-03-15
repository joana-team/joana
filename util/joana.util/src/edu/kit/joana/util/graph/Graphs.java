/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.graph;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.DirectedGraph;

/**
 * TODO: @author Add your name here.
 */
public class Graphs {
	
	public static <V, E extends KnowsVertices<V>> Set<V> getSuccNodes(DirectedGraph<V, E> graph, V node) {
		final Set<E> edges = graph.outgoingEdgesOf(node);
		final HashSet<V> successors = new HashSet<>(edges.size());
		for (E e : edges) {
			successors.add(e.getTarget());
		}
		return successors;
	}
	public static <V, E extends KnowsVertices<V>> int getSuccNodeCount(DirectedGraph<V, E> graph, V node) {
		return getSuccNodes(graph, node).size();
	}

}
