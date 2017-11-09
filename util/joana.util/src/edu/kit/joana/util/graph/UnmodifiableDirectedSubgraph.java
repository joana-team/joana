/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.graph;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.UnmodifiableDirectedGraph;

import com.google.common.collect.Iterators;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class UnmodifiableDirectedSubgraph<V, E> extends UnmodifiableDirectedGraph<V, E>  {
	private static final long serialVersionUID = -1836252856735425394L;

	private final boolean keepIsCheap;
	private final Predicate<E> keep;
	private final com.google.common.base.Predicate<E> keepGoogle;
	
	/**
	 * 
	 */
	public UnmodifiableDirectedSubgraph(DirectedGraph<V,E> g, Predicate<E> keep, boolean keepIsCheap ) {
		super(g);
		this.keep = keep;
		this.keepIsCheap = keepIsCheap;
		this.keepGoogle = new com.google.common.base.Predicate<E>() {
			public final boolean apply(E e) { return keep.test(e); }
		};
	}
	
	@Override
	public boolean containsEdge(E e) {
		if (keepIsCheap) {
			return keep.test(e) && super.containsEdge(e);
		} else {
			return super.containsEdge(e) && keep.test(e);
		}
	}
	
	@Override
	public boolean containsEdge(V sourceVertex, V targetVertex) {
		return super.getAllEdges(sourceVertex, targetVertex).stream().anyMatch(keep);
	}
	
	
	@Override
	public Set<E> incomingEdgesOf(V vertex) {
		return new SubSet(super.incomingEdgesOf(vertex));
	}
	@Override
	public Set<E> outgoingEdgesOf(V vertex) {
		return new SubSet(super.outgoingEdgesOf(vertex));
	}

	
	@Override
	public Set<E> edgeSet() {
		return new SubSet(super.edgeSet());
	}
	
	@Override
	public Set<E> edgesOf(V vertex) {
		return new SubSet(super.edgesOf(vertex));
	}
	
	@Override
	public Set<E> getAllEdges(V sourceVertex, V targetVertex) {
		return new SubSet(super.getAllEdges(sourceVertex, targetVertex));
	}
	
	@Override
	public E getEdge(V sourceVertex, V targetVertex) {
		return super.getAllEdges(sourceVertex, targetVertex).stream().filter(keep).findFirst().orElse(null);
	}
	
	
	@Override
	public int inDegreeOf(V vertex) {
		return incomingEdgesOf(vertex).size();
	}
	
	@Override
	public int outDegreeOf(V vertex) {
		return outgoingEdgesOf(vertex).size();
	}
	
	
	
	
	class SubSet extends AbstractSet<E> {
		private Set<E> superSet;
		private int size = -1;
		public SubSet(Set<E> superSet) {
			this.superSet = superSet;
		}
		
		@Override
		public Iterator<E> iterator() {
			return Iterators.filter(superSet.iterator(), keepGoogle);
		}
		
		@Override
		public int size() {
			if (size == -1) {
				this.size = (int) superSet.stream().filter(keep).count();
			}
			return size;
		}
		
	}

}
