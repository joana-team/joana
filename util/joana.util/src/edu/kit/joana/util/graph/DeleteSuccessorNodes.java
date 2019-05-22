/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.graph;

import java.util.Collections;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.UnmodifiableGraph;

import edu.kit.joana.util.collections.ModifiableNotTightArraySet;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
@SuppressWarnings("serial")
public class DeleteSuccessorNodes<V, E> extends UnmodifiableGraph<V, E> implements DirectedGraph<V, E> {

	private final DirectedGraph<V, E> graph;
	private final Set<V> ms;
	private final Class<E> classE;
	public DeleteSuccessorNodes(DirectedGraph<V, E> graph, Set<V> ms, Class<E> classE) {
		super(graph);
		this.graph = graph;
		this.ms = ms;
		this.classE = classE;
	}
	
	@Override
	public boolean containsEdge(E e) {
		if (ms.contains(graph.getEdgeSource(e))) return false;
		return super.containsEdge(e);
	}
	
	@Override
	public boolean containsEdge(V sourceVertex, V targetVertex) {
		if (ms.contains(sourceVertex)) return false;
		return super.containsEdge(sourceVertex, targetVertex);
	}
	
	@Override
	public Set<E> edgeSet() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Set<E> edgesOf(V vertex) {
		if (ms.contains(vertex)) return incomingEdgesOf(vertex);
		return super.edgesOf(vertex);
	}
	
	@Override
	public Set<E> getAllEdges(V sourceVertex, V targetVertex) {
		if (ms.contains(sourceVertex)) return Collections.emptySet();
		return super.getAllEdges(sourceVertex, targetVertex);
	}
	
	@Override
	public E getEdge(V sourceVertex, V targetVertex) {
		if (ms.contains(sourceVertex)) return null;
		return super.getEdge(sourceVertex, targetVertex);
	}
	
	@Override
	public Set<E> incomingEdgesOf(V vertex) {
		final Set<E> inc = super.incomingEdgesOf(vertex);
		final ModifiableNotTightArraySet<E> result = new ModifiableNotTightArraySet<>(inc, classE);
		result.removeIf(e -> ms.contains(graph.getEdgeSource(e)));
		return result;
	}
	
	@Override
	public int inDegreeOf(V vertex) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int outDegreeOf(V vertex) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Set<E> outgoingEdgesOf(V vertex) {
		if (ms.contains(vertex)) return Collections.emptySet();
		return super.outgoingEdgesOf(vertex);
	}
	
	@Override
	public int degreeOf(V vertex) {
		throw new UnsupportedOperationException();
	}
	
}
