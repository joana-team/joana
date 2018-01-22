/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.util;

import java.util.Iterator;
import java.util.function.Predicate;

import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.SparseIntSet;

/**
 * An edge manager that filters edges from a given graph that do not
 * connect nodes from a given set of nodes.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class FilteredEdgeManager<T> implements NumberedEdgeManager<T> {

	private static final IntSet EMPTY = new SparseIntSet();

	private final NumberedGraph<T> delegate;
	private final IntSet nodesOk;

	/**
	 * An edge manager that filters edges from a given graph that do not
	 * connect nodes from a given set of nodes.
	 * @param delegate The graph the edges are filtered from.
	 * @param nodesOk The nodes that whose edges are not filtered.
	 */
	public FilteredEdgeManager(NumberedGraph<T> delegate, IntSet nodesOk) {
		if (delegate == null) {
			throw new IllegalArgumentException();
		} else if (nodesOk == null) {
			throw new IllegalArgumentException();
		}

		this.delegate = delegate;
		this.nodesOk = nodesOk;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#addEdge(java.lang.Object, java.lang.Object)
	 */
	public void addEdge(T src, T dst) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(java.lang.Object)
	 */
	public int getPredNodeCount(T N) {
		int count = 0;

		for (Iterator<?> it = getPredNodes(N); it.hasNext();) {
			count++;
			it.next();
		}

		return count;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(java.lang.Object)
	 */
	public Iterator<T> getPredNodes(T N) {
		int num = delegate.getNumber(N);
		if (!nodesOk.contains(num)) {
			return new Iterator<T>() {

				@Override
				public boolean hasNext() {
					return false;
				}

				@Override
				public T next() {
					return null;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		Iterator<T> it = new FilterIterator<T>(delegate.getPredNodes(N), new Predicate<T>() {

			public boolean test(T o) {
				int num = delegate.getNumber(o);
				return nodesOk.contains(num);
			}});

		return it;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(java.lang.Object)
	 */
	public int getSuccNodeCount(T N) {
		int count = 0;

		for (Iterator<?> it = getSuccNodes(N); it.hasNext();) {
			count++;
			it.next();
		}

		return count;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object)
	 */
	public Iterator<T> getSuccNodes(T N) {
		int num = delegate.getNumber(N);
		if (!nodesOk.contains(num)) {
			return new Iterator<T>() {

				@Override
				public boolean hasNext() {
					return false;
				}

				@Override
				public T next() {
					return null;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		Iterator<T> it = new FilterIterator<T>(delegate.getSuccNodes(N), new Predicate<T>() {

			public boolean test(T t) {
				int num = delegate.getNumber(t);
				return nodesOk.contains(num);
			}});

		return it;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#hasEdge(java.lang.Object, java.lang.Object)
	 */
	public boolean hasEdge(T src, T dst) {
		int sNum = delegate.getNumber(src);
		int dNum = delegate.getNumber(dst);
		return nodesOk.contains(sNum) && nodesOk.contains(dNum) && delegate.hasEdge(src, dst);
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(java.lang.Object)
	 */
	public void removeAllIncidentEdges(T node)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#removeEdge(java.lang.Object, java.lang.Object)
	 */
	public void removeEdge(T src, T dst)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#removeIncomingEdges(java.lang.Object)
	 */
	public void removeIncomingEdges(T node)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#removeOutgoingEdges(java.lang.Object)
	 */
	public void removeOutgoingEdges(T node)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NumberedEdgeManager#getPredNodeNumbers(java.lang.Object)
	 */
	@Override
	public IntSet getPredNodeNumbers(T node) {
		int number = delegate.getNumber(node);
		if (nodesOk.contains(number)) {
			IntSet preds = delegate.getPredNodeNumbers(node);
			return nodesOk.intersection(preds);
		} else {
			return EMPTY;
		}
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NumberedEdgeManager#getSuccNodeNumbers(java.lang.Object)
	 */
	@Override
	public IntSet getSuccNodeNumbers(T node) {
		int number = delegate.getNumber(node);
		if (nodesOk.contains(number)) {
			IntSet succs = delegate.getSuccNodeNumbers(node);
			return nodesOk.intersection(succs);
		} else {
			return EMPTY;
		}
	}

}
