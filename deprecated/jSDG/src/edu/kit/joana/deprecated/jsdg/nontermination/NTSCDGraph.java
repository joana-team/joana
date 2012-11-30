/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.nontermination;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.IntSet;

/**
 * Computes the nontermination sensitive control dependence as described by
 * the pseudo code in "A New Foundation for Control Dependence and Slicing
 * for Modern Program Structures" from Ranganath, Amtoft, Banerjee and Hatcliff
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class NTSCDGraph<T> extends SlowSparseNumberedGraph<T> {

	private NTSCDGraph() {}

	private static final boolean DEBUG = false;

	private static class MaxPaths {

		public final int n;
		public final int m;

		public MaxPaths(int n, int m) {
			this.n = n;
			this.m = m;
		}

		public boolean equals(Object obj) {
			if (obj instanceof MaxPaths) {
				MaxPaths other = (MaxPaths) obj;
				return n == other.n && m == other.m;
			} else {
				return false;
			}
		}

		public int hashCode() {
			return n + 2314 * m;
		}

		public String toString() {
			return "t(" + n + ", " + m + ")";
		}

	}

	/**
	 * Computes the nontermination sensitive control dependence as described by
	 * the pseudo code in "A New Foundation for Control Dependence and Slicing
	 * for Modern Program Structures" from Ranganath, Amtoft, Banerjee and Hatcliff
	 */
	public static <T> NumberedGraph<T> compute(NumberedGraph<T> cfg, T entry, T exit) {
		NTSCDGraph<T> cdg = new NTSCDGraph<T>();
		for (T n : cfg) {
			cdg.addNode(n);
		}

		//# (1) Initialize
		Set<T> condNodes = condNodes(cfg);
 		Set<T> selfRef = selfRef(cfg);
		Stack<T> workbag = new Stack<T>();
		Map<T, Map<T, Set<MaxPaths>>> S = HashMapFactory.make();
		if (DEBUG) {
			System.out.print("cond nodes: ");
			for (T n : condNodes) {
				System.out.print(cfg.getNumber(n) + "; ");
			}
			System.out.println();

			System.out.print("self ref: ");
			for (T n : selfRef) {
				System.out.print(cfg.getNumber(n) + "; ");
			}
			System.out.println();
		}

		for (T n : condNodes) {
			for (Iterator<? extends T> it = cfg.getSuccNodes(n); it.hasNext();) {
				T m = it.next();
				//Set<IntSet> t_nm = maxPaths(cfg, n, m);
				MaxPaths t_nm = maxPaths(cfg, n, m);
				add(S, m, n, t_nm);
				merge(workbag, m);
			}
		}

		//# (2) calculate all-path reachability
		while (!workbag.isEmpty()) {
			final T n = workbag.pop();
			if (condNodes.contains(n)) {
				//# (2.2) n has >1 succ
				for (T m : cfg) {
					Set<MaxPaths> Smn = get(S, m, n);
					final int Tn = cfg.getSuccNodeCount(n);
					if (Smn != null && Smn.size() == Tn) {
						for (T p : condNodes) {
							if (p != n && update(S, n, m, p)) {
								merge(workbag, m);
							}
						}
					}
				}
			} else if (cfg.getSuccNodeCount(n) == 1 && !selfRef.contains(n)) {
				//# (2.1) n has exact 1 succ
				T m = cfg.getSuccNodes(n).next();
				for (T p : condNodes) {
					if (update(S, n, m, p)) {
						merge(workbag, m);
					}
				}
			}
		}

		//# (3) Calculate non-termination sensitive control dependence
		for (T n : cfg) {
			for (T m : condNodes) {
				if (n == m) {
					continue;
				}

				Set<MaxPaths> Snm = get(S, n, m);
				final int Tm = cfg.getSuccNodeCount(m);

				if (DEBUG) {
					System.out.print("S(" + cfg.getNumber(n) + ", " + cfg.getNumber(m) + ") = {");
					if (Snm != null) {
						for (MaxPaths t_nm : Snm) {
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

	private static <T> void merge(Collection<T> col, T elem) {
		if (!col.contains(elem)) {
			col.add(elem);
		}
	}

	private static <T> boolean update(Map<T, Map<T, Set<MaxPaths>>> S, T n, T m, T p) {
		boolean changed = false;

		Set<MaxPaths> Snp = get(S, n, p);
		Set<MaxPaths> Smp = get(S, m, p);

		if (Snp != null && !Snp.isEmpty() && (Smp == null || !Smp.containsAll(Snp))) {
			// S[n,p] \ S[m,p] != {}
			add(S, m, p, Snp);
			changed = true;
		}

		return changed;
	}

	private static <T> Set<MaxPaths> get(Map<T, Map<T, Set<MaxPaths>>> S, T a, T b) {
		Map<T, Set<MaxPaths>> Sn = S.get(a);
		if (Sn != null) {
			Set<MaxPaths> set = Sn.get(b);
			return set;
		}

		return null;
	}

	private static <T> void add(Map<T, Map<T, Set<MaxPaths>>> S, T a, T b, Set<MaxPaths> sets) {
		for (MaxPaths mp : sets) {
			add(S, a, b, mp);
		}
	}

	/**
	 * S[a,b] += set;
	 */
	private static <T> void add(Map<T, Map<T, Set<MaxPaths>>> S, T a, T b, MaxPaths set) {
		Map<T, Set<MaxPaths>> Sn = S.get(a);
		if (Sn == null) {
			Sn = HashMapFactory.make();
			S.put(a, Sn);
		}

		Set<MaxPaths> mps = Sn.get(b);
		if (mps == null) {
			mps = HashSetFactory.make();
			Sn.put(b, mps);
		}

		mps.add(set);
	}

	private static <T> MaxPaths maxPaths(final NumberedGraph<T> cfg, T nNode, T mNode) {
		final int n = cfg.getNumber(nNode);
		final int m = cfg.getNumber(mNode);
		return new MaxPaths(n, m);
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
	private static <T> Set<T> condNodes(Graph<T> cfg) {
		Set<T> condNodes = HashSetFactory.make();

		for (T node : cfg) {
			if (cfg.getSuccNodeCount(node) > 1) {
				condNodes.add(node);
			}
		}

		return condNodes;
	}

	/**
	 * Find all self referring nodes in the cfg
	 */
	private static <T> Set<T> selfRef(NumberedGraph<T> cfg) {
		Set<T> selfRef = HashSetFactory.make();

		for (T node : cfg) {
			IntSet succ = cfg.getSuccNodeNumbers(node);
			if (succ != null && succ.contains(cfg.getNumber(node))) {
				selfRef.add(node);
			}
		}

		return selfRef;
	}

}
