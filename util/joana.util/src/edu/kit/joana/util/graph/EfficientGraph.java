/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.graph;

import java.util.Set;
import java.util.function.Predicate;

import org.jgrapht.Graph;

/**
 *  @author Martin Hecker <martin.hecker@kit.edu>
 */
public interface EfficientGraph<V, E> extends Graph<V, E> {
	/**
	 * Adds edges to targetVertex. All edges must have target targetVertex.
	 * 
	 * THIS WILL NOT REGISTER THESE EDGES AT THEIR CORRESPONDING SOURCE VERTICES.
	 * 
	 * CALLS TO THIS METHOD MUST BE ACCOMPANIED BY CORRESPDONDING CALLS TO {@link EfficientGraph#addIncomingEdgesAtUNSAFE(Object, Set)}
	 * 
	 * @param targetVertex
	 * @param edges
	 */
	void addOutgoingEdgesAtUNSAFE(V sourceVertex, Set<E> edges);
	
	/**
	 * Adds edges to sourceVertex. All edges must have source sourceVertex.
	 * 
	 * THIS WILL NOT REGISTER THESE EDGES AT THEIR CORRESPONDING TARGET VERTICES.
	 * 
	 * CALLS TO THIS METHOD MUST BE ACCOMPANIED BY CORRESPDONDING CALLS TO {@link EfficientGraph#addOutgoingEdgesAtUNSAFE(Object, Set)}
	 * 
	 * @param sourceVertex
	 * @param edges
	 */
	void addIncomingEdgesAtUNSAFE(V targetVertex, Set<E> edges);
	
	boolean addEdgeUnsafe(V sourceVertex, V targetVertex, E e);
	boolean containsEdge(V sourceVertex, V targetVertex, Predicate<E> predicate);
	E[] outgoingEdgesOfUnsafe(V vertex);
	E[] incomingEdgesOfUnsafe(V vertex);
	void removeIncomingEdgesOf(V vertex);
	void removeOutgoingEdgesOf(V vertex);
	void trimToSize();
}
