/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.graph;

import java.util.*;

import org.jgrapht.*;
import org.jgrapht.generate.GraphGenerator;

/**
 * This Generator creates a ladder Graph
 *
 * @author Martin Hecker <martin.hecker@kit.edu>
 * @since Aug 6, 2005
 */
public class LadderGraphGenerator<V, E> implements GraphGenerator<V, E, V> {

	protected final int n;
	protected V entry;
	protected V exit1;
	protected V exit2;

	public LadderGraphGenerator(int n) {
		if (n < 0)
			throw new IllegalArgumentException("n < 0");
		this.n = n;

	}

	@Override
	public void generateGraph(Graph<V, E> target, VertexFactory<V> vertexFactory, Map<String, V> resultMap) {
		final int nrVertex = 2 * n + 2 + 1;
		@SuppressWarnings("unchecked")
		V[] vs = (V[]) new Object[nrVertex];
		for (int i = 0; i < nrVertex; i++) {
			vs[i] = vertexFactory.createVertex();
			target.addVertex(vs[i]);
		}
		
		this.entry = vs[0];
		this.exit1  = vs[nrVertex - 2];
		this.exit2  = vs[nrVertex - 1];

		target.addEdge(vs[0], vs[1]);
		target.addEdge(vs[0], vs[2]);
		for (int i = 1; i <= n; i++) {
			target.addEdge(vs[2 * i], vs[2 * i + 1]);
			target.addEdge(vs[2 * i], vs[2 * i + 2]);
			target.addEdge(vs[2 * i - 1], vs[2 * i + 1]);
			// eds n = [(2*n, 2*n+1, ()), (2*n, 2*n+2, ()), (2*n-1, 2*n+1, ())] ++ eds (n-1)
		}
	}
	
	public V getEntry() {
		return entry;
	}
	
	public V getExit1() {
		return exit1;
	}
	public V getExit2() {
		return exit2;
	}

}

