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
		
		public Node(V v) {
			this.v = v;
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
	private boolean  confirm(Map<V, V> obs, V u, V uObs) {
		for (E e : graph.outgoingEdgesOf(u)) {
			final V v = e.getTarget();
			final V obsV = obs.get(v);
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

	private  Set<V> propagate(Set<V> w, Map<V, V> obs, V u, V v) {
		final Set<V> worklist = new HashSet<>();
		worklist.add(u);
		
		final Set<V> candidates = new HashSet<>();
		while (!worklist.isEmpty()) {
			final V n; {
				Iterator<V> it = worklist.iterator();
				n = it.next();
				it.remove();
			}
			for (E e : graph.incomingEdgesOf(n)) {
				final V u0 = e.getSource();
				if (!w.contains(u0)) {
					final V obsU0 = obs.get(u0);
					if (obsU0 != null) {
						if (v != obsU0) {
							obs.put(u0, v);
							worklist.add(u0);
							if (graph.outgoingEdgesOf(u0).size() > 1) {
								candidates.add(u0);
							}
						}
					} else {
						obs.put(u0, v);
						worklist.add(u0);
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
	
	private Pair<Set<V>, Map<V,V>> main(Set<V> vv) {
		final Set<V> w = new HashSet<>(vv);
		final Map<V,V> obs = new HashMap<>(graph.vertexSet().size());
		for (V v : vv) {
			obs.put(v, v);
		}
		final Set<V> worklist = new HashSet<>(vv);
		while (!worklist.isEmpty()) {
			final V u; {
				Iterator<V> it = worklist.iterator();
				u = it.next();
				it.remove();
			}
			final Set<V> delta = new HashSet<>();
			final Set<V> c = propagate(w, obs,u, u);
			for (V v : c) {
				if (confirm(obs, v, u)) {
					delta.add(v);
				}
			}
			w.addAll(delta);
			for (V v : delta) {
				obs.put(v, v);
			}
			worklist.addAll(delta);
		}		
		return Pair.pair(w, obs);
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
