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
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;

import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.Graphs;
import edu.kit.joana.util.graph.KnowsVertices;

import static edu.kit.joana.wala.core.graphs.NTSCDGraph.*;

/**
 * Computes the nontermination sensitive control dependence as described by
 * the pseudo code in "A New Foundation for Control Dependence and Slicing
 * for Modern Program Structures" from Ranganath, Amtoft, Banerjee and Hatcliff
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class NTICDGraph<V, E extends KnowsVertices<V>> extends AbstractJoanaGraph<V, E> {

	private NTICDGraph(EdgeFactory<V, E> edgeFactory) {
		super(edgeFactory);
	}

	private static boolean DEBUG = true;

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
		DEBUG = cfg.vertexSet().toString().contains("1|ENTR|joana.api.testdata.toy.rec.MyList.main(java.lang.String[])");
		NTSCDGraph.DEBUG = DEBUG;
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
		while (!workbag.isEmpty()) {
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
					}
				}
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
		DEBUG = false;
		NTSCDGraph.DEBUG = false;
		return cdg;
	}
}
