package edu.kit.joana.graph.dominators.slca;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;


public class DominatorComputation<V,E> {
	private DirectedGraph<V,E> graph;
	private V start;
	private DFSIntervalOrder<V,E> rpo;
	private Map<V,V> doms;
	private boolean init = false;
	public DominatorComputation(DirectedGraph<V,E> graph, V start) {
		this.graph = graph;
		this.start = start;
	}
	public void run() {
		if (!init) {
			System.out.print("Computing dominator tree...");
			rpo = new DFSIntervalOrder<V, E>(graph);
			doms = new HashMap<V,V>();
			doms.put(start, start);
			boolean changed = true;
			while (changed) {
				changed = false;
				for (V v : rpo.listVertices()) {
					if (v.equals(start)) continue;
					Iterator<E> incEdges = graph.incomingEdgesOf(v).iterator();
					assert incEdges.hasNext();
					V newIDom = null;
					while (incEdges.hasNext()) {
						V next = graph.getEdgeSource(incEdges.next());
						if (newIDom == null) {
							newIDom = next;
						} else {
							newIDom = intersect(next, newIDom);
						}
					}
					if (doms.get(v) == null || !doms.get(v).equals(newIDom)) {
						doms.put(v, newIDom);
						changed = true;
					}
				}
			}
			System.out.print("done.");
			init = true;
		}
	}

	public Map<V,V> getDomMap() {
		run();
		return doms;
	}

	public DirectedGraph<V,E> getDominatorTree() {
		run();
		DirectedGraph<V,E> ret = new DefaultDirectedGraph<V, E>(graph.getEdgeFactory());
		for (Map.Entry<V, V> edge : doms.entrySet()) {
			if (edge.getKey().equals(start)) continue;
			ret.addVertex(edge.getKey());
			ret.addVertex(edge.getValue());
			ret.addEdge(edge.getValue(), edge.getKey());
		}
		return ret;
	}

	private V intersect(V b1, V b2) {
		V cur = b1;
		// climb up the dom tree until we find a node which is an ancestor of both b1 and b2
		while (!rpo.isLeq(b2, cur)) {
			cur = doms.get(cur);
		}
		return cur;
	}
}
