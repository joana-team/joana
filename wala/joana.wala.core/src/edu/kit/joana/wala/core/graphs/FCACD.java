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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;

import edu.kit.joana.util.Pair;
import edu.kit.joana.util.graph.GraphWalker;
import edu.kit.joana.util.graph.KnowsVertices;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class FCACD<V, E extends KnowsVertices<V>> {
	
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
	
	private FCACD(DirectedGraph<V,E> graph) {
		this.graph = graph;
		this.v2node = new HashMap<>(graph.vertexSet().size());
		for (V v : graph.vertexSet()) {
			final Node<V> node = new Node<>(v);
			v2node.put(v, node);
		}
			
		
		// TODO Auto-generated constructor stub
	}
	/*
	confirm :: Map Node [Node] -> Map Node Node -> Node -> Node -> Bool
	confirm sucs obs u u_obs =
	    let result0 = False in
	    let succ0 = Set.fromList $ sucs ! u in
	    loop succ0 result0
	  where loop succ result
	            | Set.null succ                                   = result
	            | Map.member v obs   ∧   (not $ u_obs == obs ! v) = loop succ' True
	            | otherwise                                       = loop succ' result
	          where (v, succ') = Set.deleteFindMin succ
*/	
	private boolean  confirm(Map<Node<V>, Node<V>> obs, Node<V> u, Node<V> uObs) {
		for (E e : graph.outgoingEdgesOf(u.v)) {
			final Node<V> v = v2node.get(e.getTarget());
			
			assert v.obs == obs.get(v);
			final Node<V> obsV = obs.get(v);
			if (obsV != null && obsV != uObs) return true;
		}
		return false;
	}
	
	/*
propagate :: Map Node (Set Node) -> Map Node [Node] -> Set Node -> Map Node Node -> Node -> Node -> (Set Node, Map Node Node)
propagate pres sucs w obs0 u v = 
    let worklist0   = Set.fromList [u]
        candidates0 = Set.empty
        result = loop obs0 worklist0 candidates0
    in -- traceShow (w, obs0, "++++", u, v, "*****", result) $
       result
  where loop obs worklist candidates
            | Set.null worklist = (candidates, obs)
            | otherwise         = let (obs'', worklist'', candidates'') = loop2 pred_todo0 obs worklist' candidates
                                  in loop obs'' worklist'' candidates''
          where (n, worklist') = Set.deleteFindMin worklist
                pred_todo0 = pres ! n
                
                loop2 pred_todo obs worklist candidates
                    | Set.null pred_todo = (obs, worklist, candidates)
                    | not $ u0 ∈  w      = let (obs', worklist', candidates') = 
                                                 if Map.member u0 obs then
                                                   if not $ (obs ! u0) == v then
                                                     (Map.insert u0 v obs, Set.insert u0 worklist, if isCond $ sucs ! u0 then Set.insert u0 candidates      else candidates)
                                                   else
                                                     (                obs,               worklist,                                                               candidates)
                                                 else
                                                     (Map.insert u0 v obs, Set.insert u0 worklist,                                                               candidates)
                                           in -- traceShow (u0, Map.lookup u0 obs, candidates') $
                                              loop2 pred_todo' obs' worklist' candidates'
                    | otherwise          =    loop2 pred_todo' obs  worklist  candidates
                  where (u0, pred_todo') = Set.deleteFindMin pred_todo
        isCond []  = False
        isCond [_] = False
        isCond _   = True
	 */

	private  List<Node<V>> propagate(Set<Node<V>> w, Map<Node<V>, Node<V>> obs, Node<V> u, Node<V> v) {
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
				assert u0.inW == w.contains(u0);
				if (!w.contains(u0)) {
					assert u0.obs == obs.get(u0);
					final Node<V> obsU0 = obs.get(u0);
					if (obsU0 != null) {
						if (v != obsU0) {
							obs.put(u0, v);
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
						obs.put(u0, v);
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
	
	/*
main :: Graph gr => gr a b -> Set Node -> (Set Node, Map Node Node)
main g v' = 
      let w0 = v'
          obs0 = Map.fromList [ (n,n) | n <- Set.toList v' ]
          worklist0 = v'
      in loop w0 obs0 worklist0
  where pres = Map.fromList [ (n, Set.fromList $ pre g n) | n <- nodes g]
        sucs = Map.fromList [ (n,                suc g n) | n <- nodes g]
        loop w obs worklist
            | Set.null worklist = -- traceShow (w, obs, worklist) $
                                  (w, obs)
            | otherwise         = -- traceShow (w, obs, worklist, "*****", u, candidates, new_nodes, obs') $
                                  loop (w ∪ new_nodes)   (Map.union (Map.fromSet id new_nodes) obs')   (worklist' ∪ new_nodes)
          where (u, worklist') = Set.deleteFindMin worklist
                (candidates, obs') =  propagate pres sucs w obs u u
                new_nodes = Set.filter (\v ->  confirm sucs obs' v u) candidates	 */
	
	private Pair<Set<V>, Map<Node<V>,Node<V>>> main(Set<V> vv) {
		final Set<Node<V>> w = new HashSet<>(vv.size());
		final Map<Node<V>,Node<V>> obs = new HashMap<>(graph.vertexSet().size());
		final LinkedList<Node<V>> worklist = new LinkedList<>();
		final Object WORKLIST = new Object();
		for (V v : vv) {
			final Node<V> nodeV = v2node.get(v);
			
			w.add(nodeV);
			nodeV.inW = true;
			
			obs.put(nodeV, nodeV);
			nodeV.obs = nodeV;
			
			worklist.add(nodeV);
			nodeV.mainWorklist = WORKLIST;
		}
		
		while (!worklist.isEmpty()) {
			final Node<V> u = worklist.poll();
			assert u.mainWorklist == WORKLIST;
			u.mainWorklist = null;
			
			final Set<Node<V>> delta = new HashSet<>();
			final List<Node<V>> c = propagate(w, obs,u, u);
			for (Node<V> v : c) {
				if (confirm(obs, v, u)) {
					delta.add(v);
					v.inW = true;
					assert (v.mainWorklist == WORKLIST) == (worklist.contains(v));
					if (v.mainWorklist != WORKLIST) {
						worklist.add(v);
						v.mainWorklist = WORKLIST;
					}
				}
			}
			w.addAll(delta);
			for (Node<V> v : delta) {
				obs.put(v, v);
				v.obs = v;
			}
		}
		final Set<V> result = new HashSet<>(w.size());
		for (Node<V> v : w) {
			result.add(v.v);
		}
		return Pair.pair(result, obs);
	}
	
	public static <V,E extends KnowsVertices<V>> Set<V> wd(DirectedGraph<V,E> graph, Set<V> vv) {
		final FCACD<V, E> fcacd = new FCACD<>(graph);
		return fcacd.main(vv).getFirst();
	}
	
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
