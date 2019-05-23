/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.util.Iterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;

import edu.kit.joana.util.collections.ArraySet;
import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators.ISinkdomEdge;
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators.Node;

/**
 * Computes nontermination insensitive control dependence.
 * 
 * @author Martin Hecker  <martin.hecker@kit.edu>
 *
 */
@SuppressWarnings("serial")
public class NTICDGraphPostdominanceFrontiers<V extends IntegerIdentifiable, E extends KnowsVertices<V>> extends AbstractJoanaGraph<V, E> {

	private NTICDGraphPostdominanceFrontiers(EdgeFactory<V, E> edgeFactory, Class<E> classE, int size) {
		super(edgeFactory, () -> new HashMap<>(size), classE);
	}

	public static boolean DEBUG = false;
	
	

	/**
	 * Computes nontermination sensitive control dependence.
	 */
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> GeneralizedPostdominanceFrontiers<V, E> compute(DirectedGraph<V, E> cfg, EdgeFactory<V, E> edgeFactory, Class<E> classE) {
		final SinkpathPostDominators<V, E> isinkdom = SinkpathPostDominators.compute(cfg);
		return compute(cfg, edgeFactory, classE, isinkdom.getResult());
	}
	
	/*
	private static final class IncomingToSccProvider<V extends IntegerIdentifiable, E extends KnowsVertices<V>> {
		private final AbstractJoanaGraph<Node<V>, SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdom;
		public IncomingToSccProvider(AbstractJoanaGraph<Node<V>, SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdom) {
			this.isinkdom = isinkdom;
		}
		public Set<ISinkdomEdge<Node<V>>> incomingToScc(Set<Node<V>> scc) {
			final Node<V> representant = scc.iterator().next().getRepresentant();
			final Set<ISinkdomEdge<Node<V>>> incoming = isinkdom.incomingEdgesOf(representant);
			return incoming;
		}
	}
	*/
	
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> GeneralizedPostdominanceFrontiers<V, E> compute(DirectedGraph<V, E> cfg, EdgeFactory<V, E> edgeFactory, Class<E> classE, final AbstractJoanaGraph<Node<V>, SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdom) {
		//final Function<Set<Node<V>>, ISinkdomEdge<Node<V>>> incomingToSccProvider = new IncomingToSccProvider(isinkdom);
		final Function<Set<Node<V>>, Set<ISinkdomEdge<Node<V>>>> incomingToSccProvider = ((scc) -> isinkdom.incomingEdgesOf(scc.iterator().next().getRepresentant()));
		return GeneralizedPostdominanceFrontiers.compute(cfg, edgeFactory, classE, isinkdom, incomingToSccProvider);
	}
	
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> void compute(DirectedGraph<V, E> cfg, AbstractJoanaGraph<Node<V>,SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdom, BiConsumer<V, Node<V>> addDf, Function<V, Iterable<V>> dfOf, Iterable<Set<Node<V>>> sccs) {
		//final Function<Set<Node<V>>, ISinkdomEdge<Node<V>>> incomingToSccProvider = new IncomingToSccProvider(isinkdom);
		final Function<Set<Node<V>>, Set<ISinkdomEdge<Node<V>>>> incomingToSccProvider = ((scc) -> isinkdom.incomingEdgesOf(scc.iterator().next().getRepresentant()));
		GeneralizedPostdominanceFrontiers.compute(cfg, isinkdom, addDf, dfOf, sccs, incomingToSccProvider);

	}
}
