/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;

import edu.kit.joana.util.graph.DeleteSuccessorNodes;
import edu.kit.joana.util.graph.DeleteSuccessorNodesAndToFromOnly;
import edu.kit.joana.util.graph.DeleteSuccessorNodesAndToOnly;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class NTSCDControlSlices {

	/**
	 * Computes the weakly deciding nodes, by computing nticd for a modification of graph
	 * 
	 * @param graph The control flow graph
	 * @param ms the slicing criterion -- a set of nodes in graph
	 * @param classE {@link Class} of E 
	 * @param edgeFactory a factory for E
	 * @return the set of weakly deciding nodes for ms
	 */
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> Set<V> wd(DirectedGraph<V,E> graph, Set<V> ms, Class<E> classE, EdgeFactory<V, E> edgeFactory) {
		final DirectedGraph<V, E> gMS = new DeleteSuccessorNodesAndToOnly<>(graph, ms, classE);
		
		final NTICDGraphPostdominanceFrontiers<V, E> nticdMS = NTICDGraphPostdominanceFrontiers.compute(gMS, edgeFactory, classE);

		final Set<V> result = new HashSet<>(ms); {
			
			final Set<V> newNodes = new HashSet<>(ms);
			
			while (!newNodes.isEmpty()) {
				final V m; {
					final Iterator<V> it = newNodes.iterator();
					m = it.next();
					it.remove();
				}
				
				for (E e : nticdMS.incomingEdgesOfUnsafe(m)) {
					if (e == null) continue;
					V n = e.getSource();
					if (result.add(n)) {
						boolean isNew = newNodes.add(n);
						assert isNew;
					}
				}
			}
		}
		return result;
	}
	
	public static <V extends IntegerIdentifiable ,E extends KnowsVertices<V>> Set<V> wcc(DirectedGraph<V,E> graph, Set<V> ms, Class<E> classE, EdgeFactory<V, E> edgeFactory) {
		final DirectedGraph<V, E> gMS = new DeleteSuccessorNodesAndToFromOnly<>(graph, ms, classE);
		
		final NTICDGraphPostdominanceFrontiers<V, E> nticdMS = NTICDGraphPostdominanceFrontiers.compute(gMS, edgeFactory, classE);

		final Set<V> result = new HashSet<>(ms); {
			
			final Set<V> newNodes = new HashSet<>(ms);
			
			while (!newNodes.isEmpty()) {
				final V m; {
					final Iterator<V> it = newNodes.iterator();
					m = it.next();
					it.remove();
				}
				
				for (E e : nticdMS.incomingEdgesOfUnsafe(m)) {
					if (e == null) continue;
					V n = e.getSource();
					if (result.add(n)) {
						boolean isNew = newNodes.add(n);
						assert isNew;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Computes the (nticd ⨃ ntiod)-backward slice, by computing nticd for a modification of graph
	 * 
	 * @param graph The control flow graph
	 * @param ms the slicing criterion -- a set of nodes in graph
	 * @param classE {@link Class} of E 
	 * @param edgeFactory a factory for E
	 * @return (nticd ⨃ ntiod)(ms)
	 */
	public static <V extends IntegerIdentifiable ,E extends KnowsVertices<V>> Set<V> nticdNtiodViaNticd(DirectedGraph<V,E> graph, Set<V> ms, Class<E> classE, EdgeFactory<V, E> edgeFactory) {
		final DirectedGraph<V, E> gMS = new DeleteSuccessorNodes<>(graph, ms, classE);
		
		final NTICDGraphPostdominanceFrontiers<V, E> nticdMS = NTICDGraphPostdominanceFrontiers.compute(gMS, edgeFactory, classE);

		final Set<V> result = new HashSet<>(ms); {
			
			final Set<V> newNodes = new HashSet<>(ms);
			
			while (!newNodes.isEmpty()) {
				final V m; {
					final Iterator<V> it = newNodes.iterator();
					m = it.next();
					it.remove();
				}
				
				for (E e : nticdMS.incomingEdgesOfUnsafe(m)) {
					if (e == null) continue;
					V n = e.getSource();
					if (result.add(n)) {
						boolean isNew = newNodes.add(n);
						assert isNew;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Computes the (nticd ⨃ ntiod)-backward slice, by computing nticd and ntiod for graph
	 * 
	 * @param graph The control flow graph
	 * @param ms the slicing criterion -- a set of nodes in graph
	 * @param classE {@link Class} of E 
	 * @param edgeFactory a factory for E
	 * @return (nticd ⨃ ntiod)(ms)
	 */
	public static <V extends IntegerIdentifiable ,E extends KnowsVertices<V>> Set<V> nticdNtiod(DirectedGraph<V,E> graph, Set<V> ms, Class<E> classE, EdgeFactory<V, E> edgeFactory) {
		final NTICDGraphPostdominanceFrontiers<V, E> nticd = NTICDGraphPostdominanceFrontiers.compute(graph, edgeFactory, classE);
		final Map<V, Map<V, Set<V>>> ntiod = NTIOD.compute(graph, edgeFactory, classE);
		
		final Set<V> result = new HashSet<>(ms); {
			
			final Set<V> newNodes = new HashSet<>(ms);
			
			while (!newNodes.isEmpty()) {
				final V m; {
					final Iterator<V> it = newNodes.iterator();
					m = it.next();
					it.remove();
				}
				
				for (E e : nticd.incomingEdgesOfUnsafe(m)) {
					if (e == null) continue;
					V n = e.getSource();
					if (result.add(n)) {
						boolean isNew = newNodes.add(n);
						assert isNew;
					}
				}
				
				final LinkedList<V> toAdd = new LinkedList<>();
				final Map<V, Set<V>> asM2 = ntiod.getOrDefault(m, Collections.emptyMap());
				for (V m1 : result) {
					for (V n : asM2.getOrDefault(m1, Collections.emptySet())) {
						toAdd.add(n);
					}
				}
				for (V m2 : result) {
					for (V n : ntiod.getOrDefault(m2, Collections.emptyMap()).getOrDefault(m, Collections.emptySet())) {
						toAdd.add(n);
					}
				}
				for (V n : toAdd) {
					if (result.add(n)) {
						boolean isNew = newNodes.add(n);
						assert isNew;
					}
				}
			}
		}
		return result;
	}

}
