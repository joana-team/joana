package edu.kit.joana.graph.dominators.slca;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.traverse.DepthFirstIterator;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 *
 * @author Martin Mohr
 *
 * @param <V> type of nodes
 * @param <E> type of edges
 */
public class DFSIntervalOrder<V, E> {
	private DirectedGraph<V, E> graph;
	private Map<V,Interval> dfsInterval;
	private TObjectIntMap<V> discoverTime;
	private TObjectIntMap<V> finishTime;
	private LinkedList<V> list;
	private int maxIndex = 0;
	private boolean initialized = false;

	public DFSIntervalOrder(DirectedGraph<V, E> graph) {
		this.graph = graph;
	}

	private V findRoot() {
		for (V v : this.graph.vertexSet()) {
			if (this.graph.inDegreeOf(v) == 0) {
				return v;
			}
		}
		return null;
	}

	private void init() {
		if (!initialized) {
			discoverTime = new TObjectIntHashMap<V>();
			finishTime = new TObjectIntHashMap<V>();
			dfsInterval = new HashMap<V, Interval>();
			list = new LinkedList<V>();
			DepthFirstIterator<V, E> dfsIter;
			V root = findRoot();
			if (root != null) {
				dfsIter = new DepthFirstIterator<V, E>(graph, root);
			} else {
				dfsIter = new DepthFirstIterator<V, E>(graph);
			}

			dfsIter.addTraversalListener(new TraversalListener<V, E>() {

				@Override
				public void vertexTraversed(VertexTraversalEvent<V> e) {
					discoverTime.put(e.getVertex(), maxIndex);
					maxIndex++;
				}

				@Override
				public void vertexFinished(VertexTraversalEvent<V> e) {
					list.addFirst(e.getVertex());
					finishTime.put(e.getVertex(), maxIndex);
					dfsInterval.put(e.getVertex(), new Interval(discoverTime.get(e.getVertex()), maxIndex));
					maxIndex++;
				}

				@Override
				public void edgeTraversed(EdgeTraversalEvent<V, E> arg0) {
				}

				@Override
				public void connectedComponentStarted(
						ConnectedComponentTraversalEvent arg0) {
				}

				@Override
				public void connectedComponentFinished(
						ConnectedComponentTraversalEvent arg0) {
				}
			});
			while (dfsIter.hasNext()) {
				dfsIter.next();
			}
			for (V v : graph.vertexSet()) {
				if (!dfsInterval.containsKey(v)) throw new RuntimeException();
			}
			initialized = true;
		}
	}

	public int finishTime(V v) {
		init();
		return finishTime.get(v);
	}

	public List<V> listVertices() {
		init();
		return list;
	}

	public boolean isLeq(V o1, V o2) {
		init();
		return dfsInterval.get(o1).isContainedIn(dfsInterval.get(o2));
	}

	public boolean isLeq(V o1, Set<V> o2) {
		init();
		for (V x : o2) {
			if (!isLeq(o1, x)) {
				return false;
			}
		}
		return true;
	}

	public int discoverTime(V o) {
		init();
		return discoverTime.get(o);
	}

}
