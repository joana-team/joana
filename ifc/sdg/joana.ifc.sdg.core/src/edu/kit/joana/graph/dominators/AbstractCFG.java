package edu.kit.joana.graph.dominators;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.DirectedGraph;

public interface AbstractCFG<V,E> {
	DirectedGraph<V,E> getUnderlyingGraph();
	V getRoot();
	Collection<? extends V> vertexSet();
	Collection<? extends E> inc(V v);
	Collection<? extends E> out(V v);
	V getSource(E e);
	V getTarget(E e);
	boolean isCallEdge(E e);
	boolean isReturnEdge(E e);
	boolean isJoinEdge(E e);
	E mapCallToReturn(E call);
	E mapReturnToCall(E ret);
	default Collection<? extends V> ahead(E e) {
		Set<V> ret = new HashSet<V>();
		ret.add(getSource(e));
		if (isReturnEdge(e)) {
			ret.add(getSource(mapReturnToCall(e)));
		}
		assert !isReturnEdge(e) && ret.size() == 1 || ret.size() == 2;
		return ret;
	}
	default Collection<? extends V> atail(E e) {
		Set<V> ret = new HashSet<V>();
		ret.add(getTarget(e));
		if (isCallEdge(e)) {
			ret.add(getTarget(mapCallToReturn(e)));
		}
		assert !isCallEdge(e) && ret.size() == 1 || ret.size() == 2;
		return ret;
	}
}
