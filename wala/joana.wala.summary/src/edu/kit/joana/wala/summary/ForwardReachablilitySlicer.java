/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.jgrapht.DirectedGraph;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ForwardReachablilitySlicer<V, E> {

	private final DirectedGraph<V, E> graph;

	private ForwardReachablilitySlicer(DirectedGraph<V, E> graph) {
		this.graph = graph;
	}

	public static <V,E> Set<V> slice(DirectedGraph<V, E> graph, V node) {
		if (node == null) {
			throw new IllegalArgumentException();
		}

		Set<V> criteria = new HashSet<V>(1);
		criteria.add(node);

		return slice(graph, criteria);
	}

	public static <V,E> Set<V> slice(DirectedGraph<V, E> graph, Set<V> nodes) {
		if (graph == null) {
			throw new IllegalArgumentException();
		} else if (nodes == null) {
			throw new IllegalArgumentException();
		}

		ForwardReachablilitySlicer<V, E> slicer = new ForwardReachablilitySlicer<V, E>(graph);

		return slicer.slice(nodes);
	}

	public Set<V> slice(Set<V> nodes) {
		Set<V> reachable = new HashSet<V>();
		reachable.addAll(nodes);

    	LinkedList<V> worklist = new LinkedList<V>();
    	worklist.addAll(nodes);

    	while (!worklist.isEmpty()) {
    		V w = worklist.poll();

    		for (E e : graph.outgoingEdgesOf(w)) {
    			V v = graph.getEdgeTarget(e);

    			if (reachable.add(v)) {
    				worklist.addFirst(v);
    			}
    		}
    	}


		return reachable;
	}

}
