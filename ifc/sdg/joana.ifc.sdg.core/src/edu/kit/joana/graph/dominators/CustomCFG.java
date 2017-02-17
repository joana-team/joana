package edu.kit.joana.graph.dominators;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.DefaultDirectedGraph;

public class CustomCFG<V, E> implements AbstractCFG<V, E> {

	private V root;
	private final DirectedGraph<V, E> graph;
	private final Map<E, E> call2ret = new HashMap<E, E>();
	private final Map<E, E> ret2call = new HashMap<E, E>();
	private final Set<E> joins = new HashSet<E>();

	public DirectedGraph<V,E> getUnderlyingGraph() {
		return graph;
	}

	public CustomCFG(Class<? extends E> clazz) {
		graph = new DefaultDirectedGraph<V, E>(clazz);
	}

	public CustomCFG(EdgeFactory<V, E> factory) {
		graph = new DefaultDirectedGraph<V, E>(factory);
	}

	public E addNormalEdge(V v1, V v2) {
		graph.addVertex(v1);
		graph.addVertex(v2);
		return graph.addEdge(v1, v2);
	}

	public void addCall(V callNode, V entryNode, V retNode, V callRetNode) {
		E call = addNormalEdge(callNode, entryNode);
		E ret = addNormalEdge(retNode, callRetNode);
		call2ret.put(call, ret);
		ret2call.put(ret, call);
	}

	public void addJoin(V exitSpawned, V joinNode) {
		E join = addNormalEdge(exitSpawned, joinNode);
		joins.add(join);
	}
	public void setRoot(V newRoot) {
		this.root = newRoot;
	}

	@Override
	public V getRoot() {
		return root;
	}

	@Override
	public Collection<? extends V> vertexSet() {
		return graph.vertexSet();
	}

	@Override
	public Collection<? extends E> inc(V v) {
		return graph.incomingEdgesOf(v);
	}

	@Override
	public Collection<? extends E> out(V v) {
		return graph.outgoingEdgesOf(v);
	}

	@Override
	public V getSource(E e) {
		assert e != null;
		return graph.getEdgeSource(e);
	}

	@Override
	public V getTarget(E e) {
		return graph.getEdgeTarget(e);
	}

	@Override
	public boolean isCallEdge(E e) {
		return call2ret.containsKey(e);
	}

	@Override
	public boolean isReturnEdge(E e) {
		return ret2call.containsKey(e);
	}

	@Override
	public E mapCallToReturn(E call) {
		assert call2ret.containsKey(call);
		return call2ret.get(call);
	}

	@Override
	public E mapReturnToCall(E ret) {
		assert ret2call.containsKey(ret);
		return ret2call.get(ret);
	}

	@Override
	public boolean isJoinEdge(E e) {
		return joins.contains(e);
	}

}
