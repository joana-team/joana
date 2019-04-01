/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;

import edu.kit.joana.util.Pair;
import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.DeleteSuccessorNodes;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;
import edu.kit.joana.util.graph.ToFromOnly;
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators.ISinkdomEdge;
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators.Node;

/**
 * Computes nontermination insensitive order dependence.
 * 
 * @author Martin Hecker  <martin.hecker@kit.edu>
 *
 */
public class NTICDMyWod {

	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> Map<V, Map<V, Set<V>>> compute(DirectedGraph<V, E> cfg, EdgeFactory<V, E> edgeFactory, Class<E> classE) {
		
		final Map<V, Map<V, Set<V>>> result = new HashMap<>();
		
		final DirectedGraph<Node<V>, SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdom = SinkpathPostDominators.compute(cfg);
		
		final Map<Node<V>, Set<V>> entryNodesFor = new HashMap<>(); {
			for (Node<V> n : isinkdom.vertexSet()) {
				final Node<V> next = n.getNext();
				if (next != null && next.isSinkNode()) {
					entryNodesFor.compute(next.getRepresentant(), (rep, entryNodes) -> {
						if (entryNodes == null) entryNodes = new HashSet<>();
						entryNodes.add(n.getV());
						return entryNodes;
					});
				}
			}
		}
		
		final Map<Node<V>, Set<V>> sinkNodesFor = new HashMap<>(); {
			for (Node<V> n : isinkdom.vertexSet()) {
				if (n.isSinkNode() && n.getRepresentant() == n && n.getNext() != null) {
					final Set<V> sinkNodes = new HashSet<>();
					sinkNodes.add(n.getV());
					
					Node<V> current = n;
					do {
						sinkNodes.add(current.getV());
						current = current.getNext();
						
					} while (current != n);
					
					sinkNodesFor.put(n, sinkNodes);
				}
			}
		}
		
		for (Entry<Node<V>, Set<V>> representantAndSinkNodes : sinkNodesFor.entrySet()) {
			final Node<V> representant = representantAndSinkNodes.getKey();
			final Set<V> sinkNodes = representantAndSinkNodes.getValue();
			final Set<V> entryNodes = entryNodesFor.get(representant);
			final DirectedGraph<V, E> gM = new ToFromOnly<V, E>(cfg, entryNodes, sinkNodes, classE);
			
			final List<LinkedList<V>> paths = new LinkedList<>();
			final Set<V> m = new HashSet<>(gM.vertexSet());
			for (V v : gM.vertexSet()) {
				if (!m.contains(v)) continue;
				if (!sinkNodes.contains(v)) continue;
				if (gM.incomingEdgesOf(v).size() > 1) {
					from(gM, paths, m, v);
				}
			}
			for (V v : gM.vertexSet()) {
				if (!m.contains(v)) continue;
				if (!sinkNodes.contains(v)) continue;
				from(gM, paths, m, v);
			}
			
			@SuppressWarnings("unchecked")
			final Node<V>[] zeroSuccessors = (Node<V>[]) new Node<?>[0];
			
			for (LinkedList<V> vpath : paths) {

				V m2v = vpath.poll();
				final DirectedGraph<V, E> gm2 = new DeleteSuccessorNodes<V, E>(gM, Collections.singleton(m2v), classE);

				
				final Pair<AbstractJoanaGraph<Node<V>, ISinkdomEdge<Node<V>>>, Map<V, Node<V>>> pair = SinkpathPostDominators.computeWithNodeMap(gm2);
				final AbstractJoanaGraph<Node<V>, SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdomM = pair.getFirst();
				final Map<V, Node<V>> vToNode = pair.getSecond();
				for (Node<V> x : isinkdomM.vertexSet()) {
					assert x == vToNode.get(m2v) ||  !x.isSinkNode();
				}

				
				final NTICDGraphPostdominanceFrontiers<V, E> dfM2 = NTICDGraphPostdominanceFrontiers.compute(gm2, edgeFactory, classE, isinkdomM);
				process(sinkNodes, m2v, dfM2, result);
				
				
				{ // restore successors for m2
					final Node<V> m2 = vToNode.get(m2v); 
					final Set<E> successorEs = gM.outgoingEdgesOf(m2v);
					final int successorEsSize = successorEs.size();
	
					@SuppressWarnings("unchecked")
					final Node<V>[] successors = (Node<V>[]) new Node<?>[successorEsSize];
					int i = 0;
					for (E e : successorEs) {
						successors[i++] =  vToNode.get(e.getTarget());
					}
					m2.setSuccessors(successors);
					m2.setRelevant(successorEsSize > 1);
					m2.setSinkNode(false);
				}

				for (V m2Suc : vpath) {
					assert gM.outgoingEdgesOf(m2v).stream().map(e -> e.getTarget()).anyMatch( t -> t.equals(m2Suc));
					final Node<V> m2SucNode = vToNode.get(m2Suc);
					final DirectedGraph<V, E> gm2Suc = new DeleteSuccessorNodes<V, E>(gM, Collections.singleton(m2Suc), classE);
					
					final Node<V>[] m2SucNodeSuccessors =  m2SucNode.getSuccessors();
					final boolean m2IsRelevant = m2SucNode.isRelevant();

					m2SucNode.setSinkNode(true);
					m2SucNode.setRelevant(false);
					m2SucNode.setSuccessors(zeroSuccessors); {
						SinkpathPostDominators.newEdge(isinkdomM, m2SucNode, null);
						for (E e : gM.incomingEdgesOf(m2Suc)) {
							final V n = e.getSource();
							if (n.equals(m2Suc)) continue;
							SinkpathPostDominators.newEdge(isinkdomM, vToNode.get(n), m2SucNode);
						}
						if (gM.incomingEdgesOf(m2Suc).size() > 1) {
							final TreeSet<Node<V>> workset = new TreeSet<Node<V>>(new Comparator<Node<V>>() {
								@Override
								public int compare(Node<V> o1, Node<V> o2) {
									return Integer.compare(o1.getId(), o2.getId());
								}
							});
							for (Node<V> x : isinkdomM.vertexSet()) {
								assert x == m2SucNode ||  !x.isSinkNode();
								if (x.isRelevant()) {
									workset.add(x);
									x.setInWorkset(true);
								}
//								final Node<V>[] successors = x.getSuccessors();
//								final int successorEsSize = successors.length;
//								switch (successorEsSize) {
//									case 0: 
//									case 1:
//										x.setRelevant(false);
//										break;
//									default: {
//										x.setRelevant(true);
//										workset.add(x);
//									}
//								}
							}
							SinkpathPostDominators.sinkDown(gm2Suc, vToNode, workset, isinkdomM);
						}
						final NTICDGraphPostdominanceFrontiers<V, E> dfM2Suc = NTICDGraphPostdominanceFrontiers.compute(gm2Suc, edgeFactory, classE, isinkdomM);
						process(sinkNodes, m2Suc, dfM2Suc, result);

					}
					m2SucNode.setSuccessors(m2SucNodeSuccessors);
					m2SucNode.setRelevant(m2IsRelevant);
					m2SucNode.setSinkNode(false);
					
					
					m2v = m2Suc;
				}
				
			}
		}
		return result;
	}
	
	private static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> void process(Set<V> sinkNodes, V m2, NTICDGraphPostdominanceFrontiers<V, E> dfM2, final Map<V, Map<V, Set<V>>> result) {
		assert sinkNodes.contains(m2);
		for (E e : dfM2.edgeSet()) {
			final V n  = e.getSource();
			final V m1 = e.getTarget();
			assert !m1.equals(m2);
			if (n.equals(m1)) continue;
			if (!sinkNodes.contains(m1)) continue;
			result.compute(m1, (mm1, map) -> {
				if (map == null) map = new HashMap<>();
				map.compute(m2, (mm2, set) -> {
					if (set == null) set = new HashSet<>();
					set.add(n);
					return set;
				});
				return map;
			});
		}
	}
	
	private static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> void from(DirectedGraph<V, E> gM, List<LinkedList<V>> paths, Set<V> m, V n) {
		m.remove(n);
		final LinkedList<V> path = new LinkedList<>();
		path.add(n);
		
		Set<E> successors = new HashSet<>(gM.outgoingEdgesOf(n));
		successors.removeIf(e -> !m.contains(e.getTarget()));
		while (!successors.isEmpty()) {
			V next = null;
			for (E e : successors) {
				assert gM.incomingEdgesOf(e.getTarget()).size() > 0;
				if (gM.incomingEdgesOf(e.getTarget()).size() == 1) {
					next = e.getTarget();
					break;
				}
			}
			if (next == null) {
				next = successors.iterator().next().getTarget();
			}
			path.add(next);
			m.remove(next);
			
			successors = new HashSet<>(gM.outgoingEdgesOf(next));
			successors.removeIf(e -> !m.contains(e.getTarget()));
		}
		paths.add(path);
	}
}
