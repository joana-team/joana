/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.graph;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.DirectedGraphBuilder;

/**
 * Transitive reduction of directed graphs g.
 * 
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class TransitiveReductionGeneral {
	/**
	 * Singleton instance.
	 */
	public static final TransitiveReductionGeneral INSTANCE = new TransitiveReductionGeneral();

	/**
	 * Private Constructor.
	 */
	private TransitiveReductionGeneral() {
	}
	
	/**
	 * Computes a transitive reduction of a directed graph g.
	 * 
	 * Note that this transitive reduction of directed graph is, in general, neither unique, nor a subgraph of g,
	 * specifically:  if g contains cycles (see, e.g, http://dx.doi.org/10.1137/0201008 ).
	 * 
	 * Hence, the reduction will not modify g, but create a new graph using builder.
	 *
	 * @param g 
	 * @param builder the builder used to build the transitive reduction.
	 */
	public <V, E, G extends DirectedGraph<V, E>> G reduction(
		final DirectedGraph<V, E> g,
		final DirectedGraphBuilder<V, E, G> builder
	) {
		return reduction(g,builder, new KosarajuStrongConnectivityInspector<V, E>(g));
	}
	
	/**
	 * Computes a transitive reduction of a directed graph g.
	 * 
	 * Note that this transitive reduction of directed graph is, in general, neither unique, nor a subgraph of g,
	 * specifically:  if g contains cycles (see, e.g, http://dx.doi.org/10.1137/0201008 ).
	 * 
	 * Hence, the reduction will not modify g, but create a new graph using builder.
	 *
	 * @param g 
	 * @param builder the builder used to build the transitive reduction.
	 * @param sccInspector an sccInspector for g. Useful if the SCCs for g have already been computed elsewhere,
	 *        and can be reused here
	 */
	public <V, E, G extends DirectedGraph<V, E>> G reduction(
		final DirectedGraph<V, E> g,
		final DirectedGraphBuilder<V, E, G> builder,
		final KosarajuStrongConnectivityInspector<V, E> sccInspector
	) {
		final List<Set<V>> sccs = sccInspector.stronglyConnectedSets();
		
		if (sccs.stream().anyMatch(scc -> scc.size() > 1)) {
			// If g is cyclic, we cannot use TransitiveReduction.reduce(..) directly,
			// but have to base the reduction on the condensed graph of g's SCCs,
			// each represented by its "canonical" vertex (i.e.: it's first node in iteration order of the SCC).

			final Map<V, Set<V>> canonicalToSccs = new HashMap<>();
			Map<V, V> nodeTocanonical = new HashMap<>();
			sccs.stream().forEach( scc -> {
				final V canonical = scc.iterator().next();
				canonicalToSccs.put(canonical, scc);
				
				scc.stream().forEach( node -> {
					nodeTocanonical.put(node, canonical);
				});
				
			});
			
			DirectedGraph<V, DefaultEdge> g1 = new DefaultDirectedGraph<>(DefaultEdge.class);
			
			canonicalToSccs.entrySet().stream().forEach( entry -> {
				final V canonical = entry.getKey();
				g1.addVertex(canonical);
			});
			g.edgeSet().stream().forEach( e -> {
				g1.addEdge(
				    nodeTocanonical.get(g.getEdgeSource(e)),
				    nodeTocanonical.get(g.getEdgeTarget(e))
				);
			});
			
			TransitiveReduction.INSTANCE.reduce(g1);
			g.vertexSet().stream().forEach( node -> builder.addVertex(node));
			
			canonicalToSccs.entrySet().stream().forEach( entry -> {
				final V canonical = entry.getKey();
				final Set<V> scc  = entry.getValue();
				// We assume that the iteration order of n1s and n2s is the same.
				// TODO: make code more robust wrt. Set.iterator() implementations for which this does not hold,
				// by using a List or similar.
				Iterator<V> n1s = scc.iterator();
				Iterator<V> n2s = scc.iterator();
				n2s.next();
				while (n1s.hasNext() && n2s.hasNext()) {
					V n1 = n1s.next();
					V n2 = n2s.next();
					builder.addEdge(n1, n2);
				};
				
				if (scc.size() > 1) {
					builder.addEdge(n1s.next(), canonical);
				}
			});
			g1.edgeSet().stream().forEach( e -> {
				builder.addEdge(
				    g1.getEdgeSource(e),
				    g1.getEdgeTarget(e)
				);
			});
			return builder.build();
		} else {
			// if g is acyclic, we can directly use TransitiveReduction.reduce(..)
			
			g.vertexSet().stream().forEach(builder::addVertex);
			g.edgeSet().stream().forEach(e -> builder.addEdge(g.getEdgeSource(e), g.getEdgeTarget(e)));
			final G result = builder.build();
			TransitiveReduction.INSTANCE.reduce(result);
			return result;
		}
	}
}
