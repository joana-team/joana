/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.wala.viz;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.labeled.LabeledGraph;

public class SubLabeledGraph<T,U> implements Graph<T> {

	private final LabeledGraph<T, U> graph;
	private final Set<U> labels;

	public SubLabeledGraph(LabeledGraph<T, U> graph, Set<U> labels) {
		this.graph = graph;
		this.labels = labels;
	}

	public void removeNodeAndEdges(T N) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public void addNode(T n) {
		throw new UnsupportedOperationException();
	}

	public boolean containsNode(T N) {
			return graph.containsNode(N);
	}

	public int getNumberOfNodes() {
		return graph.getNumberOfNodes();
	}

	public Iterator<T> iterator() {
		return graph.iterator();
	}

	public void removeNode(T n) {
		throw new UnsupportedOperationException();
	}

	public void addEdge(T src, T dst) {
		throw new UnsupportedOperationException();
	}

	public int getPredNodeCount(T N) {
		int i = 0;
		for (Iterator<? extends T> it = getPredNodes(N); it.hasNext(); it.next()) {
			i++;
		}

		return i;
	}

	public Iterator<T> getPredNodes(T N) {
		Set<T> preds = HashSetFactory.make();

		for (U l : labels) {
			for (Iterator<? extends T> it = graph.getPredNodes(N, l); it.hasNext();) {
				T node = it.next();
				preds.add(node);
			}
		}

		return preds.iterator();
	}

	public int getSuccNodeCount(T N) {
		int i = 0;
		for (Iterator<? extends T> it = getSuccNodes(N); it.hasNext(); it.next()) {
			i++;
		}

		return i;
	}

	public Iterator<T> getSuccNodes(T N) {
		Set<T> succs = HashSetFactory.make();

		for (U l : labels) {
			for (Iterator<? extends T> it = graph.getSuccNodes(N, l); it.hasNext();) {
				T node = it.next();
				succs.add(node);
			}
		}

		return succs.iterator();
	}

	public boolean hasEdge(T src, T dst) {
		for (U l : labels) {
			if (graph.hasEdge(src, dst, l)) {
				return true;
			}
		}

		return false;
	}

	public void removeAllIncidentEdges(T node)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public void removeEdge(T src, T dst) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public void removeIncomingEdges(T node)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public void removeOutgoingEdges(T node)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

}
