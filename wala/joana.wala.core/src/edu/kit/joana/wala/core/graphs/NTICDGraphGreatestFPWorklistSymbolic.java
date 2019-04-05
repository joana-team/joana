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
import edu.kit.joana.util.collections.ArrayMap;
import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.Graphs;
import edu.kit.joana.util.graph.IntegerIdentifiable;
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
@SuppressWarnings("serial")
public class NTICDGraphGreatestFPWorklistSymbolic<V extends IntegerIdentifiable, E extends KnowsVertices<V>> extends AbstractJoanaGraph<V, E> {

	private NTICDGraphGreatestFPWorklistSymbolic(EdgeFactory<V, E> edgeFactory, Class<E> classE) {
		super(edgeFactory, () -> new ArrayMap<>(), classE);
	}

	public static boolean DEBUG = false;

	/**
	 * Computes nontermination sensitive control dependence.
	 * The pseudo code in "A New Foundation for Control Dependence and Slicing
	 * for Modern Program Structures" from Ranganath, Amtoft, Banerjee and Hatcliff
	 * is flawed.
	 * 
	 * This Algorithm fixes theirs, and is not flawed.
	 * 
	 * Instead of computing the least fixed point of a modification f0' of the functional f0 used in {@link NTSCDGraph},
	 * this algorithm computes the greatest fixed point of a reformulation f of f0 such that
	 *   lfp(f) == lfp(f0)
	 * and indeed:
	 *   (p,x) ∈ S[m,p]   <==> any path starting with (p,x) can be extended to include m
	 * for S := gfp(f).
	 * 
	 */
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> NTICDGraphGreatestFPWorklistSymbolic<V, E> compute(DirectedGraph<V, E> cfg, EdgeFactory<V, E> edgeFactory, Class<E> classE) {
		NTICDGraphGreatestFPWorklistSymbolic<V, E> cdg = new NTICDGraphGreatestFPWorklistSymbolic<>(edgeFactory, classE);
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
		final Map<V, Set<V>> prevConds = new HashMap<>();
		
		final Set<V> representants = new HashSet<>();
		final Map<V, V> representantOf = new HashMap<>();
		
		final Map<V, Set<V>> succNodesOf = new HashMap<>(condNodes.size());
		
		final Map<V, Map<V, Set<MaxPaths<V>>>> S = new HashMap<>();

		
		for (final V p : condNodes) {
			prevConds.put(p, new HashSet<>());
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
						
						prevConds.compute(current, (c, ps) -> {
							if (ps == null) {
								final Set<V> prevs = new HashSet<>();
								prevs.add(n);
								return prevs;
							} else {
								ps.add(n);
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
			for (V m : reachableFromX) {
				if (representants.contains(m)) {
					add(S, m, p, t_px);
				}
			}
		}
		
		final LinkedHashSet<Pair<V, V>> workbag = new LinkedHashSet<>();
		for (V p : condNodes) {
			for (V m : representants) {
				workbag.add(Pair.pair(m,p));
			}
		}
		
		int iterations = 0;
		while (!workbag.isEmpty()) {
			iterations ++;
			final V m;
			final V p;
			{
				final Iterator<Pair<V,V>> iterator = workbag.iterator();
				final Pair<V,V> next = iterator.next();
				iterator.remove();
				
				m = next.getFirst();
				p = next.getSecond();
			}
			
			final Set<MaxPaths<V>> smp = get(S, m, p);
			final Set<MaxPaths<V>> smpNew = new HashSet<>();
			
			for (V x : succNodesOf.get(p)) {
				if (toNextCond.get(x).contains(m)) {
					smpNew.add(maxPaths(cfg, p, x));
				}
				
				
				final V n = nextCond.get(x);
				if (n != null) {
					final Set<MaxPaths<V>> Smn = get(S, m, n);
					final int Tn = succNodesOf.get(n).size();
					if (Smn.size() == Tn) {
						smpNew.add(maxPaths(cfg, p, x));
					}
					
				}
			}
			if (smp.size() != smpNew.size()) {
				for (V n : prevConds.get(p)) {
					workbag.add(Pair.pair(m, n));
				}
				set(S, m, p, smpNew);
			}
		}
		
		
		for (V p : condNodes) {
			for (V m : cfg.vertexSet()) {
				assert !representants.contains(m) || representantOf.get(m) == m;
				set(S, m, p, get(S, representantOf.get(m), p));
			}
		} 
		
		if (DEBUG && iterations > 200000) {
			System.out.println("Iterations: " + iterations);
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
