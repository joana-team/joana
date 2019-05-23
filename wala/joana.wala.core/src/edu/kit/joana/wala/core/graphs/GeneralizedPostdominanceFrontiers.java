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

/**
 * Computes generalized control dependence.
 * 
 * @author Martin Hecker  <martin.hecker@kit.edu>
 *
 */
@SuppressWarnings("serial")
public class GeneralizedPostdominanceFrontiers<V extends IntegerIdentifiable, E extends KnowsVertices<V>> extends AbstractJoanaGraph<V, E> {

	private GeneralizedPostdominanceFrontiers(EdgeFactory<V, E> edgeFactory, Class<E> classE, int size) {
		super(edgeFactory, () -> new HashMap<>(size), classE);
	}

	public static boolean DEBUG = false;

	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>, N extends AbstractPseudoTreeNode<V, ?>, D extends DomEdge<N>> GeneralizedPostdominanceFrontiers<V, E> compute(
			DirectedGraph<V, E> cfg,
			EdgeFactory<V, E> edgeFactory,
			Class<E> classE,
			AbstractJoanaGraph<N, D> isinkdom,
			Function<Set<N>, Set<D>> incomingToSccProvider
	) {
		final GeneralizedPostdominanceFrontiers<V, E> cdg = new GeneralizedPostdominanceFrontiers<>(edgeFactory, classE, cfg.vertexSet().size());
		for (V n : cfg.vertexSet()) {
			cdg.addVertexUnsafe(n);
		}
		final KosarajuStrongConnectivityInspector<N, D> sccs = new KosarajuStrongConnectivityInspector<>(isinkdom);
		compute(
			cfg,
			isinkdom,
			(y,x) -> { cdg.addEdgeUnsafe(y, x.getV(), edgeFactory.createEdge(y, x.getV()));},
			(z) -> new Iterable<V>() { // TODO: refactor this, or reuse some existing MapIterator
				final Set<E> es = cdg.incomingEdgesOf(z);
				final Iterator<E> it = es.iterator();
				public Iterator<V> iterator() {
					return new Iterator<V>() {
						public boolean hasNext() {
							return it.hasNext();
						}
						public V next() {
							return it.next().getSource();
						}
					};
				}
			},
			sccs.stronglyConnectedSets(),
			incomingToSccProvider
		);
		return cdg;
	}
	
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>, N extends AbstractPseudoTreeNode<V, ?>, D extends DomEdge<N>> void compute(
			DirectedGraph<V, E> cfg,
			AbstractJoanaGraph<N,D> isinkdom,
			BiConsumer<V,N> addDf,
			Function<V, Iterable<V>> dfOf,
			Iterable<Set<N>> sccs,
			Function<Set<N>, Set<D>> incomingToSccProvider
	) {
		for (Set<N> scc : sccs) {
			final Set<D> incoming = incomingToSccProvider.apply(scc);
			final Set<V> sccV; {
				final IntegerIdentifiable[] sccA = new  IntegerIdentifiable[scc.size()];
				int i = 0;
				for (N x : scc) {
					sccA[i++] = x.getV();
				}
				Arrays.sort(sccA, ArraySet.COMPARATOR);
				sccV = ArraySet.own(sccA);
			}
			
			
			final Set<V> sccImmediates; {
				final IntegerIdentifiable[] sccImmediatesA = new IntegerIdentifiable[incoming.size()];
				int i = 0;
				for (DomEdge<N> e : incoming) {
					sccImmediatesA[i++] = e.getSource().getV();
				}
				Arrays.sort(sccImmediatesA, ArraySet.COMPARATOR);
				sccImmediates = ArraySet.own(sccImmediatesA);
			}
			final Set<V> localAndUps = new HashSet<>();
			for (N x : scc) {
				for (E e : cfg.incomingEdgesOf(x.getV())) {
					final V y = e.getSource();
					if (!sccImmediates.contains(y) && !sccV.contains(y)) localAndUps.add(y);
				}
				for (V z : sccImmediates) {
					for (V y : dfOf.apply(z)) {
						if (!sccImmediates.contains(y)) localAndUps.add(y);
					}
				}
			}
			for (N x : scc) {
				for (V y : localAndUps) {
					if (y != x.getV()) addDf.accept(y, x);
				}
			}
			
		}
	}
}
