/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;

import edu.kit.joana.util.Pair;
import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.Graphs;
import edu.kit.joana.util.graph.KnowsVertices;
import edu.kit.joana.wala.core.graphs.NTSCDGraph.MaxPaths;

import static edu.kit.joana.wala.core.graphs.NTSCDGraph.condNodes;
import static edu.kit.joana.wala.core.graphs.NTSCDGraph.selfRef;
import static edu.kit.joana.wala.core.graphs.NTSCDGraph.maxPaths;
import static edu.kit.joana.wala.core.graphs.NTSCDGraph.add;
import static edu.kit.joana.wala.core.graphs.NTSCDGraph.get;
import static edu.kit.joana.wala.core.graphs.NTSCDGraph.set;
/**
 * Computes nontermination insensitive control dependence.
 * 
 * @author Martin Hecker  <martin.hecker@kit.edu>
 *
 */
public class NTICDGraphLeastFPDualWorklistSymbolic<V, E extends KnowsVertices<V>> extends AbstractJoanaGraph<V, E> {

	private static class WorkbagItem<V> {
		private V m,n,x;

		public WorkbagItem(V m, V n, V x) {
			this.m = m; this.n = n; this.x = x;
		}
		@Override
		public boolean equals(Object obj) {
			@SuppressWarnings("rawtypes")
			final WorkbagItem that = (WorkbagItem) obj;
			return this.m == that.m && this.n == that.n && this.x == that.x;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + m.hashCode();
			result = prime * result + n.hashCode();
			result = prime * result + x.hashCode();
			return result;
		}

		
	}

	private NTICDGraphLeastFPDualWorklistSymbolic(EdgeFactory<V, E> edgeFactory) {
		super(edgeFactory);
	}

	public static boolean DEBUG = false;

	/**
	 * Computes nontermination insensitive control dependence.
	 * The pseudo code in "A New Foundation for Control Dependence and Slicing
	 * for Modern Program Structures" from Ranganath, Amtoft, Banerjee and Hatcliff
	 * is flawed.
	 * 
	 * This Algorithm fixes theirs, and is not flawed.
	 * It works by computing the *least* fixed point of the dual f^C of the functional f used in {@link NTICDGraphGreatestFPWorklistSymbolic}.
	 * 
	 */
	public static <V , E extends KnowsVertices<V>> NTICDGraphLeastFPDualWorklistSymbolic<V, E> compute(DirectedGraph<V, E> cfg, EdgeFactory<V, E> edgeFactory) {
		NTICDGraphLeastFPDualWorklistSymbolic<V, E> cdg = new NTICDGraphLeastFPDualWorklistSymbolic<>(edgeFactory);
		for (V n : cfg.vertexSet()) {
			cdg.addVertex(n);
		}

		//# (1) Initialize
		Set<V> condNodes = condNodes(cfg);
 		Set<V> selfRef = selfRef(cfg);
		if (DEBUG) {
			System.out.print("\n\n\n\n"); 
			System.out.print("cond nodes: ");
			for (V n : condNodes) {
				System.out.print(n + "; ");
			}
			System.out.println();

			System.out.print("self ref: ");
			for (V n : selfRef) {
				System.out.print(n + "; ");
			}
			System.out.println();
		}

		final Map<MaxPaths<V>, Set<V>> reachable = new HashMap<>();
		final Map<V, V>      nextCond  = new HashMap<>();
		final Map<V, Set<V>> toNextCond = new HashMap<>();
		final Map<V, Set<Pair<V,V>>> prevCondsWithSucc = new HashMap<>();
		
		final Set<V> representants = new HashSet<>();
		final Map<V, V> representantOf = new HashMap<>();
		
		final Map<V, Set<V>> succNodesOf = new HashMap<>(condNodes.size());
		
		final Map<V, Map<V, Set<MaxPaths<V>>>> S = new HashMap<>();

		
		for (final V p : condNodes) {
			prevCondsWithSucc.put(p, new HashSet<>());
		}
		
		for (final V p : condNodes) {
			succNodesOf.put(p, Graphs.getSuccNodes(cfg, p));
		}
		
		
		for (final V n : condNodes) {
			for (V m : succNodesOf.get(n)) {
				MaxPaths<V> t_nm = maxPaths(cfg, n, m);
			
				final Set<V> reachableFromTnm = new HashSet<>();
				reachable.put(t_nm, reachableFromTnm);
				
				{ // reachableFromTnm
					Queue<V> workQueue = new LinkedList<>();
					workQueue.add(m);
					
					V next;
					while ((next = workQueue.poll()) != null ) {
						if (reachableFromTnm.add(next)) {
							for (V succ : Graphs.getSuccNodes(cfg, next)) {
								workQueue.add(succ);
							}
						}
					}
				}
				
				
				{ // nextCond, prevConds
					final Set<V> seen = new HashSet<>();
					V current = m;
					V next;
					Set<V> succNodes; 
					while ((succNodes = Graphs.getSuccNodes(cfg, current)).size() == 1 &&
					       !seen.contains((next = succNodes.iterator().next()))
					) {
						seen.add(current);
						current = next;
					}
					
					seen.add(current);
					toNextCond.put(m,seen);
					
					if (succNodes.size() > 1) {
						if (!condNodes.contains(current)) throw new IllegalStateException();
						nextCond.put(m, current);
						
						prevCondsWithSucc.compute(current, (c, ps) -> {
							if (ps == null) {
								final Set<Pair<V,V>> prevs = new HashSet<>();
								prevs.add(Pair.pair(n,m));
								return prevs;
							} else {
								ps.add(Pair.pair(n,m));
								return ps;
							}
						});
					}
				}
			}
		}
		
		
		for (V start : cfg.vertexSet()){ // representantOf
			final Collection<V> sameRepresentant = new LinkedList<>();
			V m = start;
			V n = start;
			V representant = representantOf.get(n);
			final boolean newRepresentant = representant == null;
			
			if (newRepresentant) {
				if (condNodes.contains(start)) {
					final Set<V> preds = Graphs.getPredNodes(cfg, start);
					if (preds.size() != 1) {
						representant = start;
						sameRepresentant.add(start);
					} else {
						n = preds.iterator().next();
					}
				}
			}
			
			while (representant == null) {
				sameRepresentant.add(m);
				final Set<V> succs = Graphs.getSuccNodes(cfg,n);
				if (succs.size() == 1) {
					final Set<V> preds = Graphs.getPredNodes(cfg,n);
					if (preds.size() == 1) {
						m = n;
						n = preds.iterator().next();
					} else {
						representant = n;
					}
				} else {
					representant = m;
				}
			}
			if (newRepresentant) {
				for (V s : sameRepresentant) {
					representantOf.put(s, representant);
				}
				representants.add(representant);
			}
		}

		
		
		for (V m : representants) {
			for (V p : condNodes) {
				set(S, m, p, new HashSet<>());
			}
		}
		
		for (Entry<MaxPaths<V>, Set<V>> entry : reachable.entrySet()) {
			final MaxPaths<V> t_px = entry.getKey();
			final V p = t_px.n; // duh
			@SuppressWarnings("unused")
			final V x = t_px.m; // duh
			final Set<V> reachableFromX = entry.getValue();
			for (V m : representants) {
				if (!reachableFromX.contains(m)) {
					add(S, m, p, t_px);
				}
			}
		}
		
		final LinkedHashSet<WorkbagItem<V>> workbag = new LinkedHashSet<>();
		for (V p : condNodes) {
			for (V m : representants) {
				if (get(S, m, p).size() > 0) {
					for (Pair<V,V> nx : prevCondsWithSucc.get(p)) {
						workbag.add(new WorkbagItem<>(m,nx.getFirst(),nx.getSecond()));
					}
				}
			}
		}
		
		int iterations = 0;
		while (!workbag.isEmpty()) {
			iterations ++;
			final V m;
			final V p;
			final V x;
			{
				final Iterator<WorkbagItem<V>> iterator = workbag.iterator();
				final WorkbagItem<V> next = iterator.next();
				iterator.remove();
				
				m = next.m;
				p = next.n;
				x = next.x;
			}
			
			final Set<MaxPaths<V>> smp = get(S, m, p);
			
			boolean changed = false;
			if (!toNextCond.get(x).contains(m)) {
				changed = smp.add(maxPaths(cfg,p,x));
			}
			if (changed && smp.size() == 1) {
				for (Pair<V,V> nx_ : prevCondsWithSucc.get(p)) {
					workbag.add(new WorkbagItem<V>(m, nx_.getFirst(), nx_.getSecond()));
				}
			}
		}
		
		
		for (V p : condNodes) {
			for (V m : cfg.vertexSet()) {
				assert !representants.contains(m) || representantOf.get(m) == m;
				set(S, m, p, get(S, representantOf.get(m), p));
			}
		} 
		
		if (DEBUG && iterations > 200000) {
			System.out.println("LFP: Iterations (" + cfg.toString() +  "): " + iterations);
		}
		
		//# (3) Calculate non-termination insensitive control dependence
		for (V n : cfg.vertexSet()) {
			for (V m : condNodes) {
				if (n == m) {
					continue;
				}

				final Set<MaxPaths<V>> Snm = get(S, n, m);
				final int Tm = succNodesOf.get(m).size();

				if (DEBUG) {
					System.out.print("S(" + n + ", " + m + ") = {");
					for (MaxPaths<V> t_nm : Snm) {
						System.out.print(t_nm + "; ");
					}
					System.out.println("}");
				}

				assert Snm.size() <= Tm;
				if (Snm.size() > 0 && Snm.size() < Tm) {
					cdg.addEdge(m, n);
				}
			}
		}
		return cdg;
	}
}
