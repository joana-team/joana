/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Creator:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003-2009, by Barak Naveh and Contributors.
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
/* -------------------------
 * TarjanStrongConnectivityInspector.java
 * -------------------------
 * (C) Copyright 2013-2017, by Sarah Komla-Ebri, Martin Hecker and Contributors
 *
 * Original Author:  Sarah Komla-Ebri, Martin Hecker
 *
 *
 * Changes
 * -------
 * 12-May-2017 : Initial version.
 *
 */
package edu.kit.joana.util.graph;

import java.util.*;

import org.jgrapht.*;
import org.jgrapht.graph.*;

/**
 * Allows obtaining the strongly connected components of a directed graph. The implemented algorithm follows Tarjan's
 * algorithm presented in [1]. 
 * The running time is order of O(|V|+|E|)
 * 
 * [1] <a href="http://dx.doi.org/10.1137/0201010">Depth-First Search and Linear Graph Algorithms</a>, Robert Tarjan,
 * 1972
 * 
 *
 * @author Martin Hecker, based on {@link KosarajuStrongConnectivityInspector} by Sarah Komla-Ebri
 * @since May, 2017
 */

public class TarjanStrongConnectivityInspector<V, E> {
	// the graph to compute the strongly connected sets
	private final DirectedGraph<V, E> graph;

	// stores the vertices
	private Deque<VertexNumber<V>> stack;

	// the result of the computation, cached for future calls
	private List<Set<V>> stronglyConnectedSets;

	// the result of the computation, cached for future calls
	private List<DirectedSubgraph<V, E>> stronglyConnectedSubgraphs;

	// maps vertices to their VertexNumber object
	private Map<V, VertexNumber<V>> vertexToVertexNumber;

	private int index;

	/**
	 * The constructor of GabowStrongConnectivityInspector class.
	 *
	 * @param directedGraph the graph to inspect
	 *
	 * @throws IllegalArgumentException
	 */
	public TarjanStrongConnectivityInspector(DirectedGraph<V, E> directedGraph) {
		if (directedGraph == null) {
			throw new IllegalArgumentException("null not allowed for graph!");
		}

		graph = directedGraph;
		vertexToVertexNumber = null;

		stronglyConnectedSets = null;
	}

	/**
	 * Returns the graph inspected
	 *
	 * @return the graph inspected
	 */
	public DirectedGraph<V, E> getGraph() {
		return graph;
	}

	/**
	 * 
	 * @return The mapping from vertices v to their {@link VertexNumber}, which gives access to v's dfs number, and v's
	 *         index in the list of the graphs {@link StrongConnectivityInspector#stronglyConnectedSets()}.
	 *         
	 * Will trigger a SCC inspection (via {@link TarjanStrongConnectivityInspector#stronglyConnectedSets()}) if necessary.
	 */
	public Map<V, VertexNumber<V>> getVertexToVertexNumber() {
		if (vertexToVertexNumber == null) {
			stronglyConnectedSets();
		}
		assert vertexToVertexNumber != null;
		return Collections.unmodifiableMap(vertexToVertexNumber);
	}

	/**
	 * Returns true if the graph instance is strongly connected.
	 *
	 * @return true if the graph is strongly connected, false otherwise
	 * 
	 * Will trigger a SCC inspection (via {@link TarjanStrongConnectivityInspector#stronglyConnectedSets()}) if necessary.
	 */
	public boolean isStronglyConnected() {
		return stronglyConnectedSets().size() == 1;
	}

	/**
	 * Computes a {@link List} of {@link Set}s, where each set contains vertices which together form a strongly
	 * connected component within the given graph.
	 * 
	 * The list is ordered in which the SCCs are identifies, hence yielding a reverse topological ordering of the DAG
	 * formed by the strongly connected components.
	 *
	 * @return <code>List</code> of <code>Set</code> s containing the strongly connected components
	 */
	public List<Set<V>> stronglyConnectedSets() {
		if (stronglyConnectedSets == null) {
			stronglyConnectedSets = new Vector<Set<V>>();

			// create VertexData objects for all vertices, store them
			initialize();

			// perform DFS
			for (VertexNumber<V> data : vertexToVertexNumber.values()) {
				if (data.number == VertexNumber.UNDEFINED) {
					dfsVisit(graph, data);
				}
			}

			stack = null;
		}

		return stronglyConnectedSets;
	}

	/**
	 * <p>
	 * Computes a list of {@link DirectedSubgraph}s of the given graph. Each subgraph will represent a strongly
	 * connected component and will contain all vertices of that component. The subgraph will have an edge (u,v) iff u
	 * and v are contained in the strongly connected component.
	 * </p>
	 *
	 * <p>
	 * NOTE: Calling this method will first execute {@link TarjanStrongConnectivityInspector#stronglyConnectedSets()}. If
	 * you don't need subgraphs, use that method.
	 * </p>
	 *
	 * @return a list of subgraphs representing the strongly connected components
	 */
	public List<DirectedSubgraph<V, E>> stronglyConnectedSubgraphs() {
		if (stronglyConnectedSubgraphs == null) {
			List<Set<V>> sets = stronglyConnectedSets();
			stronglyConnectedSubgraphs = new Vector<DirectedSubgraph<V, E>>(sets.size());

			for (Set<V> set : sets) {
				stronglyConnectedSubgraphs.add(new DirectedSubgraph<V, E>(graph, set, null));
			}
		}

		return stronglyConnectedSubgraphs;
	}

	/*
	 * Creates a VertexNumber object for every vertex in the graph and stores them in a HashMap. Also, initializes index
	 * and stack.
	 */
	private void initialize() {
		final int c = graph.vertexSet().size();
		vertexToVertexNumber = new HashMap<V, VertexNumber<V>>(c);
		index = 0;

		for (V vertex : graph.vertexSet()) {
			vertexToVertexNumber.put(vertex, new VertexNumber<V>(vertex));
		}

		stack = new LinkedList<>();
	}

	/*
	 * The subroutine of DFS.
	 */
	private void dfsVisit(DirectedGraph<V, E> visitedGraph, VertexNumber<V> v) {

		v.number = index;
		v.lowlink = index;
		index++;

		stack.push(v);
		v.onStack = true;
		for (E edge : visitedGraph.outgoingEdgesOf(v.vertex)) {
			VertexNumber<V> w;
			w = vertexToVertexNumber.get(visitedGraph.getEdgeTarget(edge));

			if (w.number == VertexNumber.UNDEFINED) { // (v,w) is a tree arc
				dfsVisit(graph, w);
				v.lowlink = Math.min(v.lowlink, w.lowlink);
			} else if (w.number < v.number) { // (v,w) is a frond cross-link
				if (w.onStack)
					v.lowlink = Math.min(v.lowlink, w.number);
			}
		}

		if (v.lowlink == v.number) { // v is the root of a scc
			final Set<V> scc = new HashSet<V>();
			VertexNumber<V> w = stack.peek();
			while (w != null && w.number >= v.number) {
				stack.pop();
				w.onStack = false;
				w.sccNumber = stronglyConnectedSets.size();
				scc.add(w.vertex);
				w = stack.peek();
			}
			stronglyConnectedSets.add(scc);
		}
	}

	public static final class VertexNumber<V> {
		static final int UNDEFINED = -1;
		V vertex;
		int number = UNDEFINED;
		int lowlink = UNDEFINED;
		int sccNumber = UNDEFINED;
		boolean onStack = false;

		private VertexNumber(V vertex) {
			this.vertex = vertex;
		}

		public V getVertex() {
			return vertex;
		}

		public int getNumber() {
			return number;
		}

		public int getSccNumber() {
			return sccNumber;
		}

		@Override
		public String toString() {
			return "{ " + vertex + " ; " + number + " ; " + sccNumber + "}";
		}
	}
}
