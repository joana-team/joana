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
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;

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
/**
 * Computes the nontermination sensitive control dependence.
 * 
 * @author Martin Hecker  <martin.hecker@kit.edu>
 *
 */
public class NTICDGraphGreatestFP<V extends IntegerIdentifiable, E extends KnowsVertices<V>> extends AbstractJoanaGraph<V, E> {

	private NTICDGraphGreatestFP(EdgeFactory<V, E> edgeFactory) {
		super(edgeFactory, () -> new ArrayMap<>());
	}

	public static boolean DEBUG = false;

	/**
	 * Computes the nontermination sensitive control dependence.
	 * The pseudo code in "A New Foundation for Control Dependence and Slicing
	 * for Modern Program Structures" from Ranganath, Amtoft, Banerjee and Hatcliff
	 * is flawed.
	 * 
	 * This Algorithm fixes theirs, and is not flawed.
	 * 
	 */
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> NTICDGraphGreatestFP<V, E> compute(DirectedGraph<V, E> cfg, EdgeFactory<V, E> edgeFactory) {
		NTICDGraphGreatestFP<V, E> cdg = new NTICDGraphGreatestFP<>(edgeFactory);
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
		Map<V, Map<V, Set<MaxPaths<V>>>> S = new HashMap<>();

		for (final V n : condNodes) {
			for (Iterator<? extends E> it = cfg.outgoingEdgesOf(n).iterator(); it.hasNext();) {
				final V m = it.next().getTarget();
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
					}
				}
				
				for (V u : cfg.vertexSet()) {
					if (reachableFromTnm.contains(u)) {
						add(S, u, n, t_nm);
					}
				}
			}
		}
		
		int iteration = 0;
		boolean isSmaller = true;
		//# (2) calculate all-path reachability
		while (isSmaller) {
			iteration++;
			Map<V, Map<V, Set<MaxPaths<V>>>> Snew = new HashMap<>();
			
			for (V n : condNodes) {
				for (Iterator<? extends E> it = cfg.outgoingEdgesOf(n).iterator(); it.hasNext();) {
					V m = it.next().getTarget();
					MaxPaths<V> t_nm = maxPaths(cfg, n, m);
					for (V v : toNextCond.get(m)) {
						add(Snew, v, n, t_nm);
					}
					
				}
			}

			for (V n : cfg.vertexSet()) {
				if (condNodes.contains(n)) {
					//# (2.2) n has >1 succ
					for (V x : Graphs.getSuccNodes(cfg, n)) {
						final V p = nextCond.get(x);
						if (p != null) {
							for (V m : cfg.vertexSet()) {
								final Set<MaxPaths<V>> Smp = get(S, m, p);
								final int Tp = Graphs.getSuccNodeCount(cfg, p);
								if (Smp != null && Smp.size() == Tp) {
									add(Snew, m, n, maxPaths(cfg, n, x));
								}
							}
							
						}
					}
				}
			}
			isSmaller = false; 
			
findSmaller:
			for (Map.Entry<V, Map<V, Set<MaxPaths<V>>>> en : S.entrySet()) {
				final V n = en.getKey();
				for (Map.Entry<V, Set<MaxPaths<V>>> em : en.getValue().entrySet()) {
					final V m = em.getKey();
					final Set<MaxPaths<V>> Snm = em.getValue();
					final Set<MaxPaths<V>> Snew_nm = get(Snew, n, m);
					
					assert Snew_nm == null || Snm.containsAll(Snew_nm);
					
					if ((Snew_nm == null && !Snm.isEmpty()) || (Snew_nm != null && !Snew_nm.containsAll(Snm))) {
						isSmaller = true;
						break findSmaller;
					}
				}
			}
			S = Snew;
		}
		
		
		if (DEBUG && iteration > 1) {
			System.out.println("Iterations: " + iteration);
		}
		
		//# (3) Calculate non-termination insensitive control dependence
		//DEBUG = cdg.toString().contains("fakeRootMethod");
		for (V n : cfg.vertexSet()) {
			for (V m : condNodes) {
				if (n == m) {
					continue;
				}

				Set<MaxPaths<V>> Snm = get(S, n, m);
				final int Tm = Graphs.getSuccNodeCount(cfg,m);

				if (DEBUG && iteration > 1) {
					System.out.print("S(" + n + ", " + m + ") = {");
					if (Snm != null) {
						for (MaxPaths<V> t_nm : Snm) {
							System.out.print(t_nm + "; ");
						}
					}
					System.out.println("}");
				}

				assert Snm == null || Snm.size() <= Tm;
				if (Snm != null && Snm.size() > 0 && Snm.size() < Tm) {
					cdg.addEdge(m, n);
				}
			}
		}
		return cdg;
	}
	
	static <T> void update(Map<T, Map<T, Set<MaxPaths<T>>>> S, Map<T, Map<T, Set<MaxPaths<T>>>> Snew, T n, T m, T p, Map<MaxPaths<T>, Set<T>> reachable) {
		Set<MaxPaths<T>> Snp = get(S, n, p);

		if (Snp != null && !Snp.isEmpty()) {
			add(Snew, m, p, Snp);
		}
	}
}
