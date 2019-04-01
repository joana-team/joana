/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.EdgeReversedGraph;

import edu.kit.joana.util.graph.DeleteSuccessorNodes;
import edu.kit.joana.util.graph.DeleteSuccessorNodesAndToFromOnly;
import edu.kit.joana.util.graph.DeleteSuccessorNodesAndToOnly;
import edu.kit.joana.util.graph.GraphWalker;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators.Node;
import edu.kit.joana.wala.util.WriteGraphToDot;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class SinkdomControlSlices {
	
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> Set<V> wd(DirectedGraph<V,E> graph, Set<V> ms, Class<E> classE, EdgeFactory<V, E> edgeFactory) {
		final DirectedGraph<V, E> gMS = new DeleteSuccessorNodesAndToOnly<>(graph, ms, classE);
		
		final DirectedGraph<Node<V>, SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdom = SinkpathPostDominators.compute(gMS);
		final Set<V> result = new HashSet<>(ms); {
			for (Node<V> n : isinkdom.vertexSet()) {
				if (n.getNext() == null) result.add(n.getV());
			}
		}
		return result;
	}
	
	public static <V extends IntegerIdentifiable ,E extends KnowsVertices<V>> Set<V> wcc(DirectedGraph<V,E> graph, Set<V> ms, Class<E> classE, EdgeFactory<V, E> edgeFactory) {
		final DirectedGraph<V, E> gMS = new DeleteSuccessorNodesAndToFromOnly<>(graph, ms, classE);
		final String cfgFileName = WriteGraphToDot.sanitizeFileName(SinkdomControlSlices.class.getSimpleName() + "-wcc-cfg.dot");
		try {
			WriteGraphToDot.write(gMS, cfgFileName, e -> true, v -> Integer.toString(v.getId()));
		} catch (FileNotFoundException e) {
		}

		final DirectedGraph<Node<V>, SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdom = SinkpathPostDominators.compute(gMS);
		final Set<V> result = new HashSet<>(ms); {
			for (Node<V> n : isinkdom.vertexSet()) {
				if (n.getNext() == null) result.add(n.getV());
			}
		}
		return result;
	}
	
	public static <V extends IntegerIdentifiable ,E extends KnowsVertices<V>> Set<V> nticdMyWod(DirectedGraph<V,E> graph, Set<V> ms, Class<E> classE, EdgeFactory<V, E> edgeFactory) {
		final DirectedGraph<V, E> gMS = new DeleteSuccessorNodes<>(graph, ms, classE);
		final String cfgFileName = WriteGraphToDot.sanitizeFileName(SinkdomControlSlices.class.getSimpleName() + "-nticdMyWod-cfg.dot");
		try {
			WriteGraphToDot.write(gMS, cfgFileName, e -> true, v -> Integer.toString(v.getId()));
		} catch (FileNotFoundException e) {
		}

		final DirectedGraph<Node<V>, SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdom = SinkpathPostDominators.compute(gMS);
		
		final GraphWalker<V, E> rdfs = new GraphWalker<V, E>(new EdgeReversedGraph<>(graph)) {
			@Override
			public void discover(V node) { }

			@Override
			public void finish(V node) {}
		};
		final Set<V> toMs = rdfs.traverseDFS(ms);
		
		final Set<V> result = new HashSet<>(ms); {
			for (Node<V> n : isinkdom.vertexSet()) {
				if (n.getNext() == null && toMs.contains(n.getV())) result.add(n.getV());
			}
		}
		return result;
	}

}
