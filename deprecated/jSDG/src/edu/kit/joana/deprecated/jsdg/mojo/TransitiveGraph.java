/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.mojo;

import java.util.Iterator;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphReachability;
import com.ibm.wala.util.graph.impl.GraphInverter;

/**
 * A wrapper that computes the transitive hull of a given graph.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
@SuppressWarnings("deprecation")
public class TransitiveGraph<E> implements Graph<E> {

	private final Graph<E> g;
	private GraphReachability<E,E> reach = null;
	private GraphReachability<E,E> reachInvers = null;
	public boolean changed = true;

	public TransitiveGraph(Graph<E> g) {
		this.g = g;
	}

	private void recomputeIfNeeded() {
		if (changed) {
			reach = new GraphReachability<E,E>(g, new Predicate<E>() {

				@Override
				public boolean test(E o) {
					return true;
				}
			});

			try {
				reach.solve(null);
			} catch (CancelException e) {
				e.printStackTrace();
			}

			Graph<E> inverted = GraphInverter.invert(g);
			reachInvers = new GraphReachability<E,E>(inverted, new Predicate<E>() {

				@Override
				public boolean test(E o) {
					return true;
				}
			});

			try {
				reachInvers.solve(null);
			} catch (CancelException e) {
				e.printStackTrace();
			}

			changed = false;
		}
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.Graph#removeNodeAndEdges(java.lang.Object)
	 */
	@Override
	public void removeNodeAndEdges(E n) throws UnsupportedOperationException {
		g.removeNodeAndEdges(n);
		changed = true;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
	 */
	@Override
	public void addNode(E n) {
		g.addNode(n);
		changed = true;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object)
	 */
	@Override
	public boolean containsNode(E n) {
		return g.containsNode(n);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
	 */
	@Override
	public int getNumberOfNodes() {
		return g.getNumberOfNodes();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#iterator()
	 */
	@Override
	public Iterator<E> iterator() {
		return g.iterator();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object)
	 */
	@Override
	public void removeNode(E n) {
		g.removeNode(n);
		changed = true;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#addEdge(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void addEdge(E src, E dst) {
		g.addEdge(src, dst);
		changed = true;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(java.lang.Object)
	 */
	@Override
	public int getPredNodeCount(E n) {
		recomputeIfNeeded();
		return reachInvers.getReachableSet(n).size();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(java.lang.Object)
	 */
	@Override
	public Iterator<E> getPredNodes(E n) {
		recomputeIfNeeded();
		return reachInvers.getReachableSet(n).iterator();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(java.lang.Object)
	 */
	@Override
	public int getSuccNodeCount(E N) {
		recomputeIfNeeded();
		return reach.getReachableSet(N).size();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object)
	 */
	@Override
	public Iterator<E> getSuccNodes(E n) {
		recomputeIfNeeded();
		return reach.getReachableSet(n).iterator();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#hasEdge(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean hasEdge(E src, E dst) {
		return reach.getReachableSet(src).contains(dst);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(java.lang.Object)
	 */
	@Override
	public void removeAllIncidentEdges(E node)
			throws UnsupportedOperationException {
		g.removeAllIncidentEdges(node);
		changed = true;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#removeEdge(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void removeEdge(E src, E dst) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#removeIncomingEdges(java.lang.Object)
	 */
	@Override
	public void removeIncomingEdges(E node)
			throws UnsupportedOperationException {
		g.removeIncomingEdges(node);
		changed = true;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.util.graph.EdgeManager#removeOutgoingEdges(java.lang.Object)
	 */
	@Override
	public void removeOutgoingEdges(E node)
			throws UnsupportedOperationException {
		g.removeOutgoingEdges(node);
		changed = true;
	}

}
