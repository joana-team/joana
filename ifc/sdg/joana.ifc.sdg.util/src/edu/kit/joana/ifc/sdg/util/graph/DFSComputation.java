/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.graph;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jgrapht.DirectedGraph;

public class DFSComputation<V,E> {

	enum DFSState {
		UNFINISHED,
		FINISHED;
	}

	private final DirectedGraph<V,E> graph;
	private Map<V, DFSState> nodeState;

	public DFSComputation(DirectedGraph<V,E> graph) {
		this.graph = graph;
	}

	private void init() {
		nodeState = new HashMap<V, DFSState>();
	}

	public List<DFSNode<V>> getDFSForest() {
		init();
		List<DFSNode<V>> retForest = new LinkedList<DFSNode<V>>();
		int time = 0;
		for (V node : graph.vertexSet()) {
			if (!nodeState.containsKey(node)) {
				DFSNode<V> nextRoot = dfs(node, time);
				retForest.add(nextRoot);
				time = nextRoot.getFinishingTime();
			}
		}
		return retForest;
	}

	private DFSNode<V> dfs(V start, int time) {
		time++;
		int discoveringTime = time;
		List<DFSNode<V>> subTrees = new LinkedList<DFSNode<V>>();
		for (E e : graph.outgoingEdgesOf(start)) {
			V next = graph.getEdgeTarget(e);
			if (!notDiscoveredYet(next)) {
				DFSNode<V> dfsNodeOfNext = dfs(next, time);
				subTrees.add(dfsNodeOfNext);
				time = dfsNodeOfNext.getFinishingTime();
			}
		}
		time++;
		int finishingTime = time;
		return new DFSNode<V>(start, discoveringTime, finishingTime, subTrees);
	}

	private boolean notDiscoveredYet(V node) {
		return !nodeState.containsKey(node);
	}

}
