/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.graph;

import java.util.function.Predicate;

import org.jgrapht.Graph;

/**
 *  @author Martin Hecker <martin.hecker@kit.edu>
 */
public interface EfficientGraph<V, E> extends Graph<V, E> {
	boolean addEdgeUnsafe(V sourceVertex, V targetVertex, E e);
	boolean containsEdge(V sourceVertex, V targetVertex, Predicate<E> predicate);
	E[] outgoingEdgesOfUnsafe(V vertex);
	E[] incomingEdgesOfUnsafe(V vertex);
	void removeIncomingEdgesOf(V vertex);
	void removeOutgoingEdgesOf(V vertex);
	void trimToSize();
}
