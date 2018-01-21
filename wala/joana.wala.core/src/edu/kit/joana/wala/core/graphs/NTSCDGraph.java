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

import edu.kit.joana.util.collections.ArrayMap;
import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.Graphs;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;

/**
 * Computes the nontermination sensitive control dependence as described by
 * the pseudo code in "A New Foundation for Control Dependence and Slicing
 * for Modern Program Structures" from Ranganath, Amtoft, Banerjee and Hatcliff
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class NTSCDGraph<V extends IntegerIdentifiable, E extends KnowsVertices<V>> extends AbstractJoanaGraph<V, E> {

	private NTSCDGraph(EdgeFactory<V, E> edgeFactory) {
		super(edgeFactory, () -> new ArrayMap<>());
	}

	static boolean DEBUG = false;

	static class MaxPaths<V> {

		public final V n;
		public final V m;

		public MaxPaths(V n, V m) {
			this.n = n;
			this.m = m;
		}

		public boolean equals(Object obj) {
			if (obj instanceof MaxPaths) {
				@SuppressWarnings("rawtypes")
				MaxPaths other = (MaxPaths) obj;
				return n.equals(other.n) && m.equals(other.m);
			} else {
				return false;
			}
		}

		public int hashCode() {
			return n.hashCode() + 2314 * m.hashCode();
		}

		public String toString() {
			return "t(" + n + ", " + m + ")";
		}

	}

	/**
	 * Computes the nontermination sensitive control dependence as described by
	 * the pseudo code in "A New Foundation for Control Dependence and Slicing
	 * for Modern Program Structures" from Ranganath, Amtoft, Banerjee and Hatcliff
	 * 
	 * From what i can tell, their Algorithm as presented is flawed, as it fails to revisit predicate nodes for which the
	 * condition "|S[m,n]| = T_n" is established due to change in |S[m,n]| elsewhere.
	 * We attempt this by putting nodes p on the worklist in appropriate places.
	 */
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> NTSCDGraph<V, E> compute(DirectedGraph<V, E> cfg, EdgeFactory<V, E> edgeFactory) {
		NTSCDGraph<V, E> cdg = new NTSCDGraph<>(edgeFactory);
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
		}

		
		//# (3) Calculate non-termination sensitive control dependence
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

	static <T> void merge(Collection<T> col, T elem) {
		if (!col.contains(elem)) {
			col.add(elem);
		}
	}

	static <T> boolean update(Map<T, Map<T, Set<MaxPaths<T>>>> S, T n, T m, T p) {
		boolean changed = false;
		Set<MaxPaths<T>> Snp = get(S, n, p);
		Set<MaxPaths<T>> Smp = get(S, m, p);

		if (Snp != null && !Snp.isEmpty() && (Smp == null || !Smp.containsAll(Snp))) {
			if (DEBUG) {
				System.out.println("S[" + m + "," + p + "]   <------ " + "S[" + n + "," + p + "]");
			}

			// S[n,p] \ S[m,p] != {}
			add(S, m, p, Snp);
			changed = true;
		}

		return changed;
	}

	static <T> Set<MaxPaths<T>> get(Map<T, Map<T, Set<MaxPaths<T>>>> S, T a, T b) {
		Map<T, Set<MaxPaths<T>>> Sn = S.get(a);
		if (Sn != null) {
			Set<MaxPaths<T>> set = Sn.get(b);
			return set;
		}

		return null;
	}

	static <T> boolean add(Map<T, Map<T, Set<MaxPaths<T>>>> S, T a, T b, Set<MaxPaths<T>> sets) {
		boolean changed = false;
		for (MaxPaths<T> mp : sets) {
			changed |= add(S, a, b, mp);
		}
		return changed;
	}
	
	/**
	 * S[a,b] := set;
	 */
	static <T> void set(Map<T, Map<T, Set<MaxPaths<T>>>> S, T a, T b, Set<MaxPaths<T>> set) {
		Map<T, Set<MaxPaths<T>>> Sn = S.get(a);
		if (Sn == null) {
			Sn = new HashMap<>();
			S.put(a, Sn);
		}

		Sn.put(b, set);
	}

	/**
	 * S[a,b] += set;
	 */
	static <T> boolean add(Map<T, Map<T, Set<MaxPaths<T>>>> S, T a, T b, MaxPaths<T> set) {
		Map<T, Set<MaxPaths<T>>> Sn = S.get(a);
		if (Sn == null) {
			Sn = new HashMap<>();
			S.put(a, Sn);
		}

		Set<MaxPaths<T>> mps = Sn.get(b);
		if (mps == null) {
			mps = new HashSet<>();
			Sn.put(b, mps);
		}

		return mps.add(set);
	}

	static <V, E extends KnowsVertices<V>> MaxPaths<V> maxPaths(final DirectedGraph<V,E> cfg, V nNode, V mNode) {
		return new MaxPaths<V>(nNode, mNode);
	}

	/**
	 * Find all maximal path that start with n->m
	 */
//	private static <T> Set<IntSet> realMaxPaths(final NumberedGraph<T> cfg, T nNode, T mNode) {
//		Set<IntSet> allPaths = HashSetFactory.make();
//
//		final int n = cfg.getNumber(nNode);
//		final int m = cfg.getNumber(mNode);
//
//		MutableIntSet maxPath = new BitVectorIntSet();
//		maxPath.add(n);
//
//		searchPath(cfg, allPaths, m, maxPath);
//
//		if (debug) {
//			System.out.println("Max paths: " + nNode + "->" + mNode);
//			for (IntSet path : allPaths) {
//				System.out.print("{ ");
//
//				path.foreach(new IntSetAction() {
//
//					@Override
//					public void act(int x) {
//						T node = cfg.getNode(x);
//						System.out.print(node + "; ");
//					}
//				});
//				System.out.println("}");
//			}
//		}
//
//		return allPaths;
//	}

//	private static <T> void searchPath(NumberedGraph<T> cfg, Set<IntSet> allPaths, int n, MutableIntSet maxPath) {
//		maxPath.add(n);
//
//		final T node = cfg.getNode(n);
//		final IntSet succSet = cfg.getSuccNodeNumbers(node);
//		boolean canBeExtended = false;
//
//		if (succSet != null) {
//			for (IntIterator it = succSet.intIterator(); it.hasNext();) {
//				final int succ = it.next();
//				if (!maxPath.contains(succ)) {
//					canBeExtended = true;
//					searchPath(cfg, allPaths, succ, duplicate(maxPath));
//					//maxPath.add(succ);
//				}
//			}
//		}
//
//		if (!canBeExtended) {
//			allPaths.add(maxPath);
//		}
//	}
//
//	private static MutableIntSet duplicate(MutableIntSet set) {
//		MutableIntSet dup = new BitVectorIntSet(set);
//		return dup;
//	}

	/**
	 * Find all nodes in the cfg with more then one successor
	 */
	static <V, E extends KnowsVertices<V>> Set<V> condNodes(DirectedGraph<V, E> cfg) {
		Set<V> condNodes = new HashSet<>();

		for (V node : cfg.vertexSet()) {
			if (Graphs.getSuccNodeCount(cfg, node) > 1) {
				condNodes.add(node);
			}
		}

		return condNodes;
	}

	/**
	 * Find all self referring nodes in the cfg
	 */
	static <V, E extends KnowsVertices<V>> Set<V> selfRef(DirectedGraph<V, E> cfg) {
		Set<V> selfRef = new HashSet<>();

		for (V node : cfg.vertexSet()) {
			Set<V> succ = Graphs.getSuccNodes(cfg, node);
			if (succ != null && succ.contains(node)) {
				selfRef.add(node);
			}
		}

		return selfRef;
	}

}
