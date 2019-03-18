/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;

import edu.kit.joana.util.collections.ArrayMap;
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
		super(edgeFactory, () -> new ArrayMap<>(size), classE);
	}

	public static boolean DEBUG = false;

	/**
	 * Computes nontermination sensitive control dependence.
	 */
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> NTICDGraphPostdominanceFrontiers<V, E> compute(DirectedGraph<V, E> cfg, EdgeFactory<V, E> edgeFactory, Class<E> classE) {
		final NTICDGraphPostdominanceFrontiers<V, E> cdg = new NTICDGraphPostdominanceFrontiers<>(edgeFactory, classE, cfg.vertexSet().size());
		for (V n : cfg.vertexSet()) {
			cdg.addVertexUnsafe(n);
		}

		final DirectedGraph<Node<V>, SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdom = SinkpathPostDominators.compute(cfg);
		
		final KosarajuStrongConnectivityInspector<Node<V>, ISinkdomEdge<Node<V>>> sccs = new KosarajuStrongConnectivityInspector<>(isinkdom);
		
		for (Set<Node<V>> scc : sccs.stronglyConnectedSets()) {
			final Node<V> representant = scc.iterator().next().getRepresentant();
			final Set<V> sccV; {
				final IntegerIdentifiable[] sccA = new  IntegerIdentifiable[scc.size()];
				int i = 0;
				for (Node<V> x : scc) {
					sccA[i++] = x.getV();
				}
				Arrays.sort(sccA, ArraySet.COMPARATOR);
				sccV = ArraySet.own(sccA);
			}
			
			final Set<ISinkdomEdge<Node<V>>> incoming = isinkdom.incomingEdgesOf(representant);
			final Set<V> sccImmediates; {
				final IntegerIdentifiable[] sccImmediatesA = new IntegerIdentifiable[incoming.size()];
				int i = 0;
				for (ISinkdomEdge<Node<V>> e : incoming) {
					sccImmediatesA[i++] = e.getSource().getV();
				}
				Arrays.sort(sccImmediatesA, ArraySet.COMPARATOR);
				sccImmediates = ArraySet.own(sccImmediatesA);
			}
			final Set<V> localAndUps = new HashSet<>();
			for (Node<V> x : scc) {
				for (E e : cfg.incomingEdgesOf(x.getV())) {
					final V y = e.getSource();
					if (!sccImmediates.contains(y) && !sccV.contains(y)) localAndUps.add(y);
				}
				for (V z : sccImmediates) {
					for (E e : cdg.incomingEdgesOf(z)) {
						final V y = e.getSource();
						if (!sccImmediates.contains(y)) localAndUps.add(y);
					}
				}
			}
			for (Node<V> x : scc) {
				for (V y : localAndUps) {
					if (y != x.getV()) cdg.addEdgeUnsafe(y, x.getV(), edgeFactory.createEdge(y, x.getV()));
				}
			}
			
		}
		return cdg;
	}
}
