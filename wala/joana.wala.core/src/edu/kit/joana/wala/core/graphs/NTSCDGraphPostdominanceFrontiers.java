/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;

import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;
import edu.kit.joana.wala.core.graphs.MaximalPathPostDominators.IMaximalDomEdge;
import edu.kit.joana.wala.core.graphs.MaximalPathPostDominators.Node;

/**
 * Computes nontermination sensitive control dependence.
 * 
 * @author Martin Hecker  <martin.hecker@kit.edu>
 *
 */
@SuppressWarnings("serial")
public class NTSCDGraphPostdominanceFrontiers<V extends IntegerIdentifiable, E extends KnowsVertices<V>> extends AbstractJoanaGraph<V, E> {

	private NTSCDGraphPostdominanceFrontiers(EdgeFactory<V, E> edgeFactory, Class<E> classE, int size) {
		super(edgeFactory, () -> new HashMap<>(size), classE);
	}

	/**
	 * Computes nontermination sensitive control dependence.
	 */
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> GeneralizedPostdominanceFrontiers<V, E> compute(DirectedGraph<V, E> cfg, EdgeFactory<V, E> edgeFactory, Class<E> classE) {
		final MaximalPathPostDominators<V, E> isinkdom = MaximalPathPostDominators.compute(cfg);
		return compute(cfg, edgeFactory, classE, isinkdom.getResult());
	}
	
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> GeneralizedPostdominanceFrontiers<V, E> compute(DirectedGraph<V, E> cfg, EdgeFactory<V, E> edgeFactory, Class<E> classE, final AbstractJoanaGraph<Node<V>, IMaximalDomEdge<Node<V>>> isinkdom) {
		final Function<Set<Node<V>>, Set<IMaximalDomEdge<Node<V>>>> incomingToSccProvider = ((scc) -> {
			final HashSet<IMaximalDomEdge<Node<V>>> incoming = new HashSet<>();
			for (Node<V> n : scc) incoming.addAll(isinkdom.incomingEdgesOf(n));
			return incoming;
		});
		return GeneralizedPostdominanceFrontiers.compute(cfg, edgeFactory, classE, isinkdom, incomingToSccProvider);
	}
	
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> void compute(DirectedGraph<V, E> cfg, AbstractJoanaGraph<Node<V>,IMaximalDomEdge<Node<V>>> isinkdom, BiConsumer<V, Node<V>> addDf, Function<V, Iterable<V>> dfOf, Iterable<Set<Node<V>>> sccs) {
		final Function<Set<Node<V>>, Set<IMaximalDomEdge<Node<V>>>> incomingToSccProvider = ((scc) -> {
			final HashSet<IMaximalDomEdge<Node<V>>> incoming = new HashSet<>();
			for (Node<V> n : scc) incoming.addAll(isinkdom.incomingEdgesOf(n));
			return incoming;
		});
		GeneralizedPostdominanceFrontiers.compute(cfg, isinkdom, addDf, dfOf, sccs, incomingToSccProvider);
	}
}
