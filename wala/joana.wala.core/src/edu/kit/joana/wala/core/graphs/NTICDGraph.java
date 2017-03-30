/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;

import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.Graphs;
import edu.kit.joana.util.graph.KnowsVertices;

import static edu.kit.joana.wala.core.graphs.NTSCDGraph.*;

/**
 * This class is supposded to compute the nontermination insensitive control dependence as described by
 * the pseudo code in "A New Foundation for Control Dependence and Slicing
 * for Modern Program Structures" from Ranganath, Amtoft, Banerjee and Hatcliff
 * 
 * This implementation started as a direct implementation of their Algorithm 
 * in [1], Figure 5. After it turned out to be incorrect, several attempts to fix it,
 * keeping the lft approach, were tried out, but didnt succeed.
 * 
 * [1] Figure 5
 * 
 *
 * @author Juergen Graf <graf@kit.edu>
 * @author Martin Hecker <martin.hecker@kit.edu>
 *
 */
public class NTICDGraph<V, E extends KnowsVertices<V>> extends AbstractJoanaGraph<V, E> {

	private NTICDGraph(EdgeFactory<V, E> edgeFactory) {
		super(edgeFactory);
	}

	private static boolean DEBUG = false;

	/**
	 * Computes the nontermination sensitive control dependence as described by
	 * the pseudo code in "A New Foundation for Control Dependence and Slicing
	 * for Modern Program Structures" from Ranganath, Amtoft, Banerjee and Hatcliff
	 * 
	 * From what i can tell, their Algorithm as presented is flawed, as it fails to revisit predicate nodes for which the
	 * condition "|S[m,n]| = T_n" is established due to change in |S[m,n]| elsewhere.
	 * We attempt this by putting nodes p on the worklist in appropriate places.
	 */
	public static <V, E extends KnowsVertices<V>> NTICDGraph<V, E> compute(DirectedGraph<V, E> cfg, EdgeFactory<V, E> edgeFactory) {
		NTICDGraph<V, E> cdg = new NTICDGraph<>(edgeFactory);
		for (V n : cfg.vertexSet()) {
			cdg.addVertex(n);
		}
		
		
		

		//# (1) Initialize
		Set<V> condNodes = condNodes(cfg);
 		Set<V> selfRef = selfRef(cfg);
		Stack<V> workbag = new Stack<>();
		Map<V, Map<V, Set<MaxPaths<V>>>> S = new HashMap<>();
		if (DEBUG) {
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


		final Map<V, Set<V>> condsInSccOf = new HashMap<>(); {
			final KosarajuStrongConnectivityInspector<V, E> sccInspector = new KosarajuStrongConnectivityInspector<>(cfg);
			
			for (Set<V> scc : sccInspector.stronglyConnectedSets()) {
				if (scc.size() > 1) {
					final Set<V> condsInScc = scc.stream().filter(v -> condNodes.contains(v)).collect(Collectors.toSet());
					for (V v : condsInScc) {
						condsInSccOf.put(v,condsInScc);
					}
				}
			}
		}

		
		for (V n : condNodes) {
			for (Iterator<? extends E> it = cfg.outgoingEdgesOf(n).iterator(); it.hasNext();) {
				V m = it.next().getTarget();
				//Set<IntSet> t_nm = maxPaths(cfg, n, m);
				MaxPaths<V> t_nm = maxPaths(cfg, n, m);
				add(S, m, n, t_nm);
				merge(workbag, m);
			}
		}

		//# (2) calculate all-path reachability
		
		boolean changed;
		while (!workbag.isEmpty()) {
			changed = false;
			final V n = workbag.pop();
			if (condNodes.contains(n)) {
				//# (2.2) n has >1 succ
				for (V m : cfg.vertexSet()) {
					Set<MaxPaths<V>> Smn = get(S, m, n);
					final int Tn = Graphs.getSuccNodeCount(cfg, n);
					if ((Smn == null && Tn == 0) || (Smn != null && Smn.size() == Tn)) {
						for (V p : condNodes) {
							if (p != n && update(S, n, m, p)) {
								merge(workbag, m);
								merge(workbag, p);
								changed = true;
							}
						}
					}
				}
			} else if (Graphs.getSuccNodeCount(cfg, n) == 1 && !selfRef.contains(n)) {
				//# (2.1) n has exact 1 succ
				V m = Graphs.getSuccNodes(cfg, n).iterator().next();
				for (V p : condNodes) {
					if (update(S, n, m, p)) {
						merge(workbag, m);
						merge(workbag, p);
						changed = true;
					}
				}
			}
			
			final Set<MaxPaths<V>> Snn = get(S, n, n);
			
			// TODO: assert "Snn.size > 0 given Snn != null" ?!!?!?
			if (Snn != null && Snn.size() > 0) {
				for (V m : Graphs.getSuccNodes(cfg, n)) {
					if (n == m) continue;
					if (update(S, n, m, n)) {
						merge(workbag, m);
						merge(workbag, n);
						changed = true;
					}
				}
			}
			
			// NEW RULE. A failed Attempt to fix nticd.
			// this iteration order is utter horseshit.
			final Set<V> condsInSccOfN = condsInSccOf.get(n);
			if (condsInSccOfN != null) {
				for (V x : Graphs.getSuccNodes(cfg, n)) {
					for (V z : cfg.vertexSet()) {
						for (V m : condsInSccOfN) {
							boolean zIsInevitable = true;
							final Set<MaxPaths<V>> Szm = get(S, z, m);
							final Set<MaxPaths<V>> Snm = get(S, n, m);
	
							if (Snm == null || Szm == null) continue;
							
							for (V m_ : Graphs.getSuccNodes(cfg, m)) {
								final MaxPaths<V> t_mm_ = maxPaths(cfg, n, m_);
								if (!(Snm.contains(t_mm_) || Szm.contains(t_mm_))) {
									zIsInevitable = false;
									break;
								}
							}
							
							if (zIsInevitable) {
								final Set<MaxPaths<V>> t_nx = Collections.singleton(maxPaths(cfg, n, x));
								changed |= add(S, z, n, t_nx);
							}
						}
					}
				}
			}
			
			if (changed) {
				workbag.clear();
				workbag.addAll(cfg.vertexSet());
			}
			
		}

		if (DEBUG) { for (int i = 0; i < 10; i++) { System.out.print("\n"); } }
		//# (3) Calculate non-termination insensitive control dependence
		for (V n : cfg.vertexSet()) {
			for (V m : condNodes) {
				if (n == m) {
					continue;
				}

				Set<MaxPaths<V>> Snm = get(S, n, m);
				final int Tm = Graphs.getSuccNodeCount(cfg,m);

				if (DEBUG) {
					System.out.print("S(" + n + ", " + m + ") = {");
					if (Snm != null) {
						for (MaxPaths<V> t_nm : Snm) {
							System.out.print(t_nm + "; ");
						}
					}
					System.out.println("}");
				}

				if (Snm != null && Snm.size() > 0 && Snm.size() < Tm) {
					cdg.addEdge(m, n);
				}
			}
		}
		return cdg;
	}
}
