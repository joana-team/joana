/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;

import edu.kit.joana.util.graph.DeleteSuccessorNodesAndToFromOnly;
import edu.kit.joana.util.graph.DeleteSuccessorNodesAndToOnly;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class NTICDControlSlices {
	
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

}
