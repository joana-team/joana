package edu.kit.joana.graph.dominators.slca;
import java.util.Map;

import org.jgrapht.DirectedGraph;

/**
 * This is a simple computation of the "single lowest common ancestor" of two nodes in a DAG G. The
 * single lowest common ancestor was proposed in "New common ancestor problems in trees and directed acyclic graphs"
 * by Johannes Fischer and Daniel H. Huson (2010).
 *
 * Observe that their definition of "single lowest common ancestor" for DAGs coincides with the definition of "common dominator"
 * on G. So, if we compute the dominator tree T_G of G, then the computation of lca on T_G yields
 * the sclas for G. [The application we have in mind is that G itself is a dominator graph of an interprocedural control-flow
 * graph, but with a slightly different definition of dominance, which does not neccessarily yield trees but at least DAGs]
 *
 * The algorithm proceeds as follows:
 * - As a preprocessing step, the dominator tree of G is computed.
 * - Given two nodes u,v, the single lowest common ancestor of u,v is computed as the traditional lowest common ancestor
 * in the dominator tree of G.
 *
 * The cost of each slca operation is linear in the height of the dominator tree, since the dominator tree is basically
 * traversed from one of the two nodes to the lca and it may be the case that both nodes are leaves are maximally deep in the tree.
 * Since the dominator tree may be arbitrarily deranged, this means that the cost of each slca is no more than O(N), where
 * N is the number of nodes in the given graph.
 *
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 *
 * @param <V> type of nodes in the given graph
 * @param <E> type of edges in the given graph
 */
public class SLCAComputation<V,E> {
	private DirectedGraph<V,E> graph;
	private V start;
	private DirectedGraph<V,E> domTree;
	private Map<V,V> domMap;
	private DFSIntervalOrder<V,E> rpoDomTree;
	private boolean init = false;

	public SLCAComputation(DirectedGraph<V,E> graph, V start) {
		this.graph = graph;
		this.start = start;
	}

	private void preprocess() {
		if (!init) {
			DominatorComputation<V, E> domComp = new DominatorComputation<V,E>(graph, start);
			domTree = domComp.getDominatorTree();
			rpoDomTree = new DFSIntervalOrder<V, E>(domTree);
			domMap = domComp.getDomMap();
			init = true;
		}
	}

	public V slca(V v, V w) {
		preprocess();
		/**
		 * The following algorithm does what we want:
		 * Let I(v) be the interval [discoverTime(v),finishTime(v)] yielded from a depth-first search on the dominator tree.
		 * V cur = v;
		 * while (I(w) is not contained in I(cur)) {
		 *   // inv: I(v) \subseteq I(cur) \subseteq I(lca(v,w)), i.e. cur is on the path between lca(v,w) and v.
		 *   // Clearly, this holds before the first iteration.
		 *   // If it holds before the execution of the loop body and we
		 *   // entered the loop body, cur must still be a descendant of lca(v,w) (either that or cur = lca, but cur = lca cannot be since we entered the loop body)
		 *   // Hence, either cur.parent = lca(v,w) or cur.parent is a descendant of lca(v,w).
		 *   // However in any case, the invariant still holds.
		 *   cur = cur.parent; (or domMap.get(cur))
		 * }
		 * After the loop we have I(cur) \supseteq I(w) and also I(cur) \supseteq I(v) (because of the loop inv),
		 * hence cur is a ca of v and w, so I(cur) \supset I(lca(v,w)). Furthermore, because of the loop inv,
		 * we also have I(cur) \subseteq I(lca(v,w)), hence I(cur) = I(lca(v,w)), hence cur = lca(v,w).
		 * After a one time precomputation of the depth-first intervals in linear time,
		 * this algorithm yields lcas in O(h) time, where h is the height of the tree.
		 */
		if (rpoDomTree.isLeq(v, w)) {
			return w;
		} else if (rpoDomTree.isLeq(w, v)) {
			return v;
		} else {
			V cur = v;
			while (!rpoDomTree.isLeq(w, cur)) {
				cur = domMap.get(cur);
			}
			return cur;
		}
	}
}
