/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.graph;

import java.util.Collection;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.EdgeSetFactory;

/**
 * This is the base class for all concrete graphs used in the JOANA project.
 * @author Martin Mohr
 */
public class AbstractJoanaGraph<V, E extends KnowsVertices<V>> implements DirectedGraph<V,E> {
	
	
	private final DirectedPseudograph<V,E> delegate;
	
	private boolean changed = true;
	private int hashCode;

	/**
	 * @param defaultEdgeFactory edge factory to use for e.g. adding new edges
	 * @see org.jgrapht.graph.DirectedPseudograph
	 */
	public AbstractJoanaGraph(EdgeFactory<V, E> edgeFactory) {
		this.delegate = new DirectedPseudograph<V,E>(edgeFactory);
	}

	/**
	 * @param class1
	 */
	public AbstractJoanaGraph(Class<E> edgeClass) {
		this.delegate = new DirectedPseudograph<V,E>(edgeClass);
		changed = true;
	}

	private void changed() {
		changed = true;
	}
	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#addEdge(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	public boolean addEdge(V arg0, V arg1, E arg2) {
		changed = true;
		return delegate.addEdge(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#addEdge(java.lang.Object, java.lang.Object)
	 */
	public E addEdge(V arg0, V arg1) {
		changed = true;
		return delegate.addEdge(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#addVertex(java.lang.Object)
	 */
	public boolean addVertex(V arg0) {
		changed = true;
		return delegate.addVertex(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#containsEdge(java.lang.Object)
	 */
	public boolean containsEdge(E arg0) {
		return delegate.containsEdge(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see org.jgrapht.graph.AbstractGraph#containsEdge(java.lang.Object, java.lang.Object)
	 */
	public boolean containsEdge(V arg0, V arg1) {
		return delegate.containsEdge(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#containsVertex(java.lang.Object)
	 */
	public boolean containsVertex(V arg0) {
		return delegate.containsVertex(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#degreeOf(java.lang.Object)
	 */
	public int degreeOf(V arg0) {
		return delegate.degreeOf(arg0);
	}

	/**
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#edgeSet()
	 */
	public Set<E> edgeSet() {
		return delegate.edgeSet();
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#edgesOf(java.lang.Object)
	 */
	public Set<E> edgesOf(V arg0) {
		return delegate.edgesOf(arg0);
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (!(obj instanceof AbstractJoanaGraph)) {
			return false;
		} else {
		    AbstractJoanaGraph<V,E> other = (AbstractJoanaGraph<V,E>) obj;
			DirectedPseudograph<V, E> otherDelegate = (DirectedPseudograph<V, E>) other.delegate;
			return delegate.equals(otherDelegate);
		}
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#getAllEdges(java.lang.Object, java.lang.Object)
	 */
	public Set<E> getAllEdges(V arg0, V arg1) {
		return delegate.getAllEdges(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#getEdge(java.lang.Object, java.lang.Object)
	 */
	public E getEdge(V arg0, V arg1) {
		return delegate.getEdge(arg0, arg1);
	}

	/**
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#getEdgeFactory()
	 */
	public EdgeFactory<V, E> getEdgeFactory() {
		final EdgeFactory<V, E> edgeFactory = delegate.getEdgeFactory();
		return new EdgeFactory<V, E>() {

			@Override
			public E createEdge(V sourceVertex, V targetVertex) {
				changed();
				return edgeFactory.createEdge(sourceVertex, targetVertex);
			}
		};
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#getEdgeSource(java.lang.Object)
	 */
	public V getEdgeSource(E arg0) {
		return delegate.getEdgeSource(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#getEdgeTarget(java.lang.Object)
	 */
	public V getEdgeTarget(E arg0) {
		return delegate.getEdgeTarget(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#getEdgeWeight(java.lang.Object)
	 */
	public double getEdgeWeight(E arg0) {
		return delegate.getEdgeWeight(arg0);
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if (changed) {
			hashCode = delegate.hashCode(); 
		}
		return hashCode;
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#inDegreeOf(java.lang.Object)
	 */
	public int inDegreeOf(V arg0) {
		return delegate.inDegreeOf(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#incomingEdgesOf(java.lang.Object)
	 */
	public Set<E> incomingEdgesOf(V arg0) {
		return delegate.incomingEdgesOf(arg0);
	}

	/**
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#isAllowingLoops()
	 */
	public boolean isAllowingLoops() {
		return delegate.isAllowingLoops();
	}

	/**
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#isAllowingMultipleEdges()
	 */
	public boolean isAllowingMultipleEdges() {
		return delegate.isAllowingMultipleEdges();
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#outDegreeOf(java.lang.Object)
	 */
	public int outDegreeOf(V arg0) {
		return delegate.outDegreeOf(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#outgoingEdgesOf(java.lang.Object)
	 */
	public Set<E> outgoingEdgesOf(V arg0) {
		return delegate.outgoingEdgesOf(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractGraph#removeAllEdges(java.util.Collection)
	 */
	public boolean removeAllEdges(Collection<? extends E> arg0) {
		changed = true;
		return delegate.removeAllEdges(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see org.jgrapht.graph.AbstractGraph#removeAllEdges(java.lang.Object, java.lang.Object)
	 */
	public Set<E> removeAllEdges(V arg0, V arg1) {
		changed = true;
		return delegate.removeAllEdges(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractGraph#removeAllVertices(java.util.Collection)
	 */
	public boolean removeAllVertices(Collection<? extends V> arg0) {
		changed = true;
		return delegate.removeAllVertices(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#removeEdge(java.lang.Object)
	 */
	public boolean removeEdge(E arg0) {
		changed = true;
		return delegate.removeEdge(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#removeEdge(java.lang.Object, java.lang.Object)
	 */
	public E removeEdge(V arg0, V arg1) {
		changed = true;
		return delegate.removeEdge(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#removeVertex(java.lang.Object)
	 */
	public boolean removeVertex(V arg0) {
		changed = true;
		return delegate.removeVertex(arg0);
	}

	/**
	 * @param arg0
	 * @see org.jgrapht.graph.AbstractBaseGraph#setEdgeSetFactory(org.jgrapht.graph.EdgeSetFactory)
	 */
	public void setEdgeSetFactory(EdgeSetFactory<V, E> arg0) {
		delegate.setEdgeSetFactory(arg0);
	}


	/**
	 * @return
	 * @see org.jgrapht.graph.AbstractGraph#toString()
	 */
	public String toString() {
		return delegate.toString();
	}

	/**
	 * @return
	 * @see org.jgrapht.graph.AbstractBaseGraph#vertexSet()
	 */
	public Set<V> vertexSet() {
		return delegate.vertexSet();
	}

}
