/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import edu.kit.joana.util.graph.KnowsVertices;

/**
 * TODO: @author Add your name here.
 */
public abstract class DomEdge<V> implements KnowsVertices<V> {
	private V source;
	private V target;
	public DomEdge(V source, V target) {
		this.source = source;
		this.target = target;
	}
	
	@Override
	public String toString() {
		return "ISINKDOM";
	}
	
	@Override
	public V getSource() {
		return source;
	}
	
	@Override
	public V getTarget() {
		return target;
	}
}
