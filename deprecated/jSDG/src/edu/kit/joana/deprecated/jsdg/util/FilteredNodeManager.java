/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.util;

import java.util.Iterator;

import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.intset.IntSet;

/**
 * An node manager that filters node from a given graph that are not contained
 * in a given set of nodes.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
@SuppressWarnings("deprecation")
public class FilteredNodeManager<T> implements NumberedNodeManager<T> {

	private final NumberedNodeManager<T> delegate;
	private final IntSet ok;

	/**
	 * An node manager that filters node from a given graph that are not contained
	 * in a given set of nodes.
	 * @param delegate The graph the nodes are filtered from.
	 * @param nodesOk The nodes that are not filtered.
	 */
	public FilteredNodeManager(NumberedNodeManager<T> delegate, IntSet nodesOk) {
		if (delegate == null) {
			throw new IllegalArgumentException();
		} else if (nodesOk == null) {
			throw new IllegalArgumentException();
		}

		this.delegate = delegate;
		this.ok = nodesOk;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
	 */
	public void addNode(T n) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object)
	 */
	public boolean containsNode(T N) {
		int num = delegate.getNumber(N);
		return (num >= 0 && ok.contains(num));
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
	 */
	public int getNumberOfNodes() {
		return ok.size();
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#iterator()
	 */
	public Iterator<T> iterator() {
		FilterIterator<T> it = new FilterIterator<T>(delegate.iterator(), new Predicate<T>() {
			@SuppressWarnings("unchecked")
			public boolean test(T t) {
				int num = delegate.getNumber(t);
				return ok.contains(num);
			}});

		return it;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object)
	 */
	public void removeNode(T n) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NumberedNodeManager#getMaxNumber()
	 */
	@Override
	public int getMaxNumber() {
		return delegate.getMaxNumber();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NumberedNodeManager#getNode(int)
	 */
	@Override
	public T getNode(int number) {
		return (ok.contains(number) ? delegate.getNode(number) : null);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NumberedNodeManager#getNumber(java.lang.Object)
	 */
	@Override
	public int getNumber(T N) {
		int number = delegate.getNumber(N);

		return (ok.contains(number) ? number : -1);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NumberedNodeManager#iterateNodes(com.ibm.wala.util.intset.IntSet)
	 */
	@Override
	public Iterator<T> iterateNodes(IntSet s) {
		IntSet okNodes = ok.intersection(s);
		return delegate.iterateNodes(okNodes);
	}

}
