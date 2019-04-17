/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;

import edu.kit.joana.util.Pair;
import edu.kit.joana.util.graph.GraphWalker;
import edu.kit.joana.util.graph.KnowsVertices;

/**
 * An implementation of the weak-control-closure algorithm from [1].
 * I somewhat follows the reference WHY3 implementation provided in [2],
 * but used efficient representations whenever profiling revealed it to be beneficial.
 *   
 * [1] Léchenet JC., Kosmatov N., Le Gall P. (2018) Fast Computation of Arbitrary Control Dependencies,
 *     https://doi.org/10.1007/978-3-319-89363-1_12
 * [2] http://perso.ecp.fr/~lechenetjc/control/download.html
 * 
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class FCACD<V, E extends KnowsVertices<V>> {
	
	/**
	 * Efficient representation of 
	 * * the map "obs",
	 * * the set "w", and
	 * * membership in the worklists in proceure "main" and "propagate".
	 * 
	 * Wrapping V in Node<V> is benficial overall, but does not completly avoid HashMap/Set lookups,
	 * because i still sometimes need to obtain the wrapped Node<V> from a V by lookup in FCACD.v2Node
	 * 
	 * @author Martin Hecker <martin.hecker@kit.edu>
	 */
	static class Node<V> {
		final V v;
		Node<V> obs;
		boolean inW;
		Object candidate;
		
		Object propagateWorklist;
		Object mainWorklist;
		
		public Node(V v) {
			this.v = v;
			this.inW = false;
			this.obs = null;
		}

	}
	
	private final Map<V, Node<V>> v2node;
	private final DirectedGraph<V,E> graph;
	private final boolean assertionsEnabled;
	
	private FCACD(DirectedGraph<V,E> graph) {
		this.graph = graph;
		this.v2node = new HashMap<>(graph.vertexSet().size());
		for (V v : graph.vertexSet()) {
			final Node<V> node = new Node<>(v);
			v2node.put(v, node);
		}
		
		boolean assertionsEnabled = false;
		assert (assertionsEnabled = true);
		this.assertionsEnabled = assertionsEnabled;
	}
	
	private boolean  confirm(Map<Node<V>, Node<V>> obs, Node<V> u, Node<V> uObs) {
		for (E e : graph.outgoingEdgesOf(u.v)) {
			final Node<V> v = v2node.get(e.getTarget());
			
			assert v.obs == obs.get(v);
			final Node<V> obsV = v.obs;
			if (obsV != null && obsV != uObs) return true;
		}
		return false;
	}

	private  List<Node<V>> propagate(Set<V> w, Map<Node<V>, Node<V>> obs, Node<V> u, Node<V> v) {
		final LinkedList<Node<V>> worklist = new LinkedList<>();
		final Object WORKLIST = new Object();
		worklist.add(u);
		u.propagateWorklist = WORKLIST;
		
		final List<Node<V>> candidates = new LinkedList<>();
		final Object CANDIDATE = new Object();
		while (!worklist.isEmpty()) {
			final Node<V> n = worklist.poll();
			assert n.propagateWorklist == WORKLIST;
			
			n.propagateWorklist = null;
			for (E e : graph.incomingEdgesOf(n.v)) {
				final Node<V> u0 = v2node.get(e.getSource());
				assert u0.inW == w.contains(u0.v);
				if (!u0.inW) {
					assert u0.obs == obs.get(u0);
					final Node<V> obsU0 = u0.obs;
					if (obsU0 != null) {
						if (v != obsU0) {
							if (assertionsEnabled) obs.put(u0, v);
							u0.obs = v;
							
							assert (u0.propagateWorklist == WORKLIST) == (worklist.contains(u0));
							if (u0.propagateWorklist != WORKLIST) {
								worklist.add(u0);
								u0.propagateWorklist = WORKLIST;
							}
							
							if (graph.outgoingEdgesOf(u0.v).size() > 1) {
								assert (u0.candidate == CANDIDATE) == candidates.contains(u0);
								if (u0.candidate != CANDIDATE) {
									candidates.add(u0);
									u0.candidate = CANDIDATE;
								}
								
							}
						}
					} else {
						if (assertionsEnabled) obs.put(u0, v);
						u0.obs = v;
						
						assert (u0.propagateWorklist == WORKLIST) == (worklist.contains(u0));
						if (u0.propagateWorklist != WORKLIST) {
							worklist.add(u0);
							u0.propagateWorklist = WORKLIST;
						}
					}
				}
			}
		}
		return candidates;
	}
	
	private Pair<Set<V>, Map<Node<V>,Node<V>>> main(Set<V> vv) {
		final Set<V> w = new HashSet<>(vv.size());
		final Map<Node<V>,Node<V>> obs = new HashMap<>(graph.vertexSet().size());
		final LinkedList<Node<V>> worklist = new LinkedList<>();
		final Object WORKLIST = new Object();
		for (V v : vv) {
			final Node<V> nodeV = v2node.get(v);
			
			w.add(v);
			nodeV.inW = true;
			
			if (assertionsEnabled) obs.put(nodeV, nodeV);
			nodeV.obs = nodeV;
			
			worklist.add(nodeV);
			nodeV.mainWorklist = WORKLIST;
		}
		
		while (!worklist.isEmpty()) {
			final Node<V> u = worklist.poll();
			assert u.mainWorklist == WORKLIST;
			u.mainWorklist = null;
			
			
			final List<Node<V>> c = propagate(w, obs,u, u);
			final List<Node<V>> delta = new ArrayList<>(c.size());
			for (Node<V> v : c) {
				if (confirm(obs, v, u)) {
					delta.add(v);
					
					w.add(v.v);
					v.inW = true;
					assert (v.mainWorklist == WORKLIST) == (worklist.contains(v));
					if (v.mainWorklist != WORKLIST) {
						worklist.add(v);
						v.mainWorklist = WORKLIST;
					}
				}
			}
			for (Node<V> v : delta) {
				if (assertionsEnabled) obs.put(v, v);
				v.obs = v;
			}
		}
		return Pair.pair(w, obs);
	}
	
	/**
	 * Computes the weakly deciding nodes.
	 * 
	 * @param graph The control flow graph
	 * @param vv a set of nodes in graph
	 * @return the set of weakly deciding nodes for vv
	 */
	public static <V,E extends KnowsVertices<V>> Set<V> wd(DirectedGraph<V,E> graph, Set<V> vv) {
		final FCACD<V, E> fcacd = new FCACD<>(graph);
		return fcacd.main(vv).getFirst();
	}
	
	/**
	 * Computes the weakly control closure.
	 * 
	 * @param graph The control flow graph
	 * @param vv a set of nodes in graph
	 * @return the weakly control closure of vv
	 */
	public static <V,E extends KnowsVertices<V>> Set<V> wcc(DirectedGraph<V,E> graph, Set<V> vv) {
		final FCACD<V, E> fcacd = new FCACD<>(graph);
		final Set<V> w = fcacd.main(vv).getFirst();
		
		final GraphWalker<V, E> dfs = new GraphWalker<V, E>(graph) {
			@Override
			public void discover(V node) { }

			@Override
			public void finish(V node) {}
		};
		final Set<V> fromVV = dfs.traverseDFS(vv);
		
		w.retainAll(fromVV);
		return w;
	}
}
