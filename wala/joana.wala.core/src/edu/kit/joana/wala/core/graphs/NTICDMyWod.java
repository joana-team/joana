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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
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

	/**
	 * 
	 * @return a map m s.t.  n ∈ m.get(m2).get(m1)   ⇔   n  →mywod (m1,m2)     (i.e.: roles of m1,m2 are reversed from what you expected!)
	 */
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> Map<V, Map<V, Set<V>>> compute(DirectedGraph<V, E> cfg, EdgeFactory<V, E> edgeFactory, Class<E> classE) {
		
		
		final DirectedGraph<Node<V>, SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdom = SinkpathPostDominators.compute(cfg);
		
		final Map<Node<V>, Set<V>> entryNodesFor = entryNodesFor(isinkdom);
		
		final Map<Node<V>, Set<V>> sinkNodesFor;
		final Integer resultSize; { 
			final Pair<Map<Node<V>, Set<V>>, Integer> p = sinkNodesFor(isinkdom);
			sinkNodesFor = p.getFirst();
			resultSize = p.getSecond();
		}
		
		final Map<V, Map<V, Set<V>>> result = new HashMap<>(resultSize);

		
		for (Entry<Node<V>, Set<V>> representantAndSinkNodes : sinkNodesFor.entrySet()) {
			final Node<V> representant = representantAndSinkNodes.getKey();
			final Set<V> sinkNodes = representantAndSinkNodes.getValue();
			final Set<V> entryNodes = entryNodesFor.get(representant);
			final DirectedGraph<V, E> gM = new ToFromOnly<V, E>(cfg, entryNodes, sinkNodes, classE);
			
			final List<LinkedList<V>> paths = simpleHeuristic(gM, sinkNodes);
			
			@SuppressWarnings("unchecked")
			final Node<V>[] zeroSuccessors = (Node<V>[]) new Node<?>[0];
			
			for (LinkedList<V> vpath : paths) {

				V m2v = vpath.poll();
				final DirectedGraph<V, E> gm2 = new DeleteSuccessorNodes<V, E>(gM, Collections.singleton(m2v), classE);

				
				final Pair<AbstractJoanaGraph<Node<V>, ISinkdomEdge<Node<V>>>, Map<V, Node<V>>> pair =
					SinkpathPostDominators.computeWithNodeMap(
						gm2,
						new Iterable<Set<V>>() {
							public Iterator<Set<V>> iterator() {
								final Iterator<V> it = gm2.vertexSet().iterator();
								return new Iterator<Set<V>>() { // TODO: use common iteratorMap or sth.
									@Override
									public boolean hasNext() {
										return it.hasNext();
									}
									
									@Override
									public Set<V> next() {
										final V v = it.next();
										return Collections.singleton(v);
									}
								};
							}
						}
				);
				final AbstractJoanaGraph<Node<V>, SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdomM = pair.getFirst();
				final Map<V, Node<V>> vToNode = pair.getSecond();
				for (Node<V> x : isinkdomM.vertexSet()) {
					assert x == vToNode.get(m2v) ||  !x.isSinkNode();
				}

				
				process(sinkNodes, m2v, result, isinkdomM, gm2);
				
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
							}
							SinkpathPostDominators.sinkDown(gm2Suc, vToNode, workset, isinkdomM);
						}
						process(sinkNodes, m2Suc, result, isinkdomM, gm2Suc);

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

	private static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> Map<Node<V>, Set<V>> entryNodesFor(DirectedGraph<Node<V>, SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdom) {
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
		return entryNodesFor;
	}

	private static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> Pair<Map<Node<V>, Set<V>>, Integer> sinkNodesFor(DirectedGraph<Node<V>, SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdom) {
		int resultSize = 0;
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
					resultSize += sinkNodes.size();
				}
			}
		}
		return Pair.pair(sinkNodesFor, resultSize);
	}
	
	private static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> List<LinkedList<V>> simpleHeuristic(DirectedGraph<V, E> gM, Set<V> sinkNodes) {
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
		
		return paths;
	}

	private static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> void process(Set<V> sinkNodes, V m2, final Map<V, Map<V, Set<V>>> result, AbstractJoanaGraph<Node<V>, SinkpathPostDominators.ISinkdomEdge<Node<V>>> isinkdomM, DirectedGraph<V, E> gm2) {
		assert sinkNodes.contains(m2);
		assert !result.containsKey(m2);
		
		final Map<V, Set<V>> m2map = new HashMap<>(sinkNodes.size());
		
		NTICDGraphPostdominanceFrontiers.compute(gm2, isinkdomM, addDf(m2map, m2), dfOf(m2map, m2));

		result.put(m2, m2map);
	}
	
	private static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> Function<V, Iterable<V>> dfOf(final Map<V, Set<V>> m2map, V m2) {
		return (z -> m2map.getOrDefault(z, Collections.emptySet()));
	}
	
	private static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> BiConsumer<V, Node<V>> addDf(final Map<V, Set<V>> m2map, V m2) {
		return (n,m1) -> {
			assert !m1.equals(m2);
			if (n.equals(m1)) return;

			m2map.compute(m1.getV(), (mm1, set) -> {
				if (set == null) set = new HashSet<>();
				set.add(n);
				return set;
			});
		};
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
