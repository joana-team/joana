/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.EdgeReversedGraph;

import edu.kit.joana.util.collections.ModifiableNotTightArraySet;
import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.GraphWalker;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;
import edu.kit.joana.util.graph.LeastCommonAncestor;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class SinkpathPostDominators<V  extends IntegerIdentifiable, E extends KnowsVertices<V>> {
	public static class ISinkdomEdge<V> implements KnowsVertices<V> {
		private V source;
		private V target;
		public ISinkdomEdge(V source, V target) {
			this.source = source;
			this.target = target;
		}
		
		@Override
		public String toString() {
			return "ISINKDOM";
		}
		
		@Override
		public V getSource() {
			return source;
		}
		
		@Override
		public V getTarget() {
			return target;
		}
	};

	public static final class Node<V extends IntegerIdentifiable> extends AbstractPseudoTreeNode<V, Node<V>> {
		
		private boolean processed;
		private boolean isSinkNode;
		
		private Node<V> representant;
		
		
		public Node(V v) {
			super(v);
			this.processed = false;
			this.isSinkNode = false;
			
			this.representant = this;
		}
		
		public boolean isSinkNode() {
			return isSinkNode;
		}
		
		public void setSinkNode(boolean isSinkNode) {
			this.isSinkNode = isSinkNode;
		}
		
		
		public final Node<V> getRepresentant() {
			return representant;
		}
		
		public void setInWorkset(boolean inWorkset) {
			this.inWorkset = inWorkset;
		}
	}
	
	private final AbstractJoanaGraph<Node<V>, ISinkdomEdge<Node<V>>> result;
	private final DirectedGraph<V, E> graph;
	private final Map<V, Node<V>> vToNode;
	private final LinkedList<Node<V>> workqueue;
	
	private final TreeSet<Node<V>> workset;
	
	@SuppressWarnings("serial")
	private SinkpathPostDominators(DirectedGraph<V, E> graph) {
		this.graph = graph;
		final ISinkdomEdge<Node<V>> dummy = new ISinkdomEdge<SinkpathPostDominators.Node<V>>(null, null);
		@SuppressWarnings("unchecked")
		final Class<ISinkdomEdge<Node<V>>> clazz = (Class<ISinkdomEdge<Node<V>>>) dummy.getClass();
		this.result = new AbstractJoanaGraph<Node<V>, ISinkdomEdge<Node<V>>>(
			new EdgeFactory<Node<V>, ISinkdomEdge<Node<V>>>() {
				@Override
				public ISinkdomEdge<Node<V>> createEdge(Node<V> sourceVertex, Node<V> targetVertex) {
					return new ISinkdomEdge<>(sourceVertex, targetVertex); 
				}
			},
			() -> new HashMap<>(graph.vertexSet().size()),
			(ISinkdomEdge<Node<V>>[] es) ->  ModifiableNotTightArraySet.own(es, clazz),
			clazz
		) {	};
		
		this.vToNode = new HashMap<>();
		
		for (V v : graph.vertexSet()) {
			final Node<V> n = new Node<V>(v);
			vToNode.put(v, n);
			result.addVertexUnsafe(n);
		}
		
		this.workqueue = new LinkedList<>();
		this.workset = new TreeSet<>(new Comparator<Node<V>>() {
			public int compare(SinkpathPostDominators.Node<V> o1, SinkpathPostDominators.Node<V> o2) {
				assert (o1.dfsNumber == o2.dfsNumber) == (o1 == o2);
				return Integer.compare(o2.dfsNumber, o1.dfsNumber);
			};
		});
	}
	
	public Map<V, Node<V>> getvToNode() {
		return vToNode;
	}
	
	public AbstractJoanaGraph<Node<V>, ISinkdomEdge<Node<V>>> getResult() {
		return result;
	}
	
	private void processed(Node<V> x) {
		final GraphWalker<Node<V>, ISinkdomEdge<Node<V>>> rdfs = new GraphWalker<Node<V>, ISinkdomEdge<Node<V>>>(new EdgeReversedGraph<>(result)) {
			@Override
			public void discover(Node<V> node) {}

			@Override
			public void finish(Node<V> node) {
				node.processed = true;
			}
			
			@Override
			public boolean traverse(Node<V> node, ISinkdomEdge<Node<V>> edge) {
				return !node.processed;
			}
		};
		rdfs.traverseDFS(x);
	}
	
	void newEdge(Node<V> x, Node<V> z) {
		if (x.next != null) result.removeOutgoingEdgesOf(x);
		if (z != null) result.addEdgeUnsafe(x, z, new ISinkdomEdge<SinkpathPostDominators.Node<V>>(x, z));
		x.next = z;
	}
	
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> SinkpathPostDominators<V, E> compute(DirectedGraph<V, E> graph) {
		final KosarajuStrongConnectivityInspector<V, E> sccInspector = new KosarajuStrongConnectivityInspector<V, E>(graph);
		final List<Set<V>> sccs = sccInspector.stronglyConnectedSets();
		return compute(graph, sccs);
	}
	
	private void initialize(Iterable<Set<V>> sccs, boolean initializeWorkset) {
		final List<V> representants = new LinkedList<>();
		for (Set<V> scc : sccs) {
			final boolean isSink = ! scc.stream().anyMatch(
				v -> graph.outgoingEdgesOf(v).stream().anyMatch(
					e -> !scc.contains(graph.getEdgeTarget(e))
				)
			);
			if (isSink) {
				Node<V> last = null;
				Node<V> first = null;
				for (V v : scc) {
					final Node<V> n = vToNode.get(v);
					if (last != null) {
						newEdge(last, n);
						processed( last);
						last = n;
					} else {
						last = n;
						first = n;
					}
					assert first != null;
					n.representant = first;
					n.isSinkNode = true;
				}
				if (last != first) newEdge( last, first);
				processed( last);
				representants.add(first.v);
			}
		}
		
		
		final LinkedList<V> rdfsOrder = new LinkedList<>(); {
			final GraphWalker<V, E> rdfs = new GraphWalker<V,E>(new EdgeReversedGraph<>(graph)) {
				@Override
				public void discover(V node) {}
	
				@Override
				public void finish(V node) {
					rdfsOrder.addFirst(node);
				}
				
			};
			rdfs.traverseDFS(representants);
		}
		
		int dfsNumber = 0;
		for (V v : rdfsOrder) {
			final Node<V> x = vToNode.get(v);
			x.dfsNumber = dfsNumber++;
			if (!x.isSinkNode) {
				final Set<E> successorEs = graph.outgoingEdgesOf(v);
				final int successorEsSize = successorEs.size();
				switch (successorEsSize) {
					case 0: break;
					case 1: {
						final Node<V> z = vToNode.get(successorEs.iterator().next().getTarget());
						if (z != x) {
							newEdge(x, z.representant);
							if (z.processed) processed( x);
						}
						break;
					}
					default: {
						x.isRelevant = true;
						@SuppressWarnings("unchecked")
						final Node<V>[] successors = (Node<V>[]) new Node<?>[successorEsSize];
						int i = 0;
						for (E e : successorEs) {
							successors[i++] =  vToNode.get(e.getTarget());
						}
						x.successors = successors;
						workqueue.addLast(x);
						if (initializeWorkset) workset.add(x);
						x.inWorkset = true;
					}
				}
			}
		}
	}
	
	private void sinkUp() {
		{
			while (!workqueue.isEmpty()) {
				final Node<V> x = workqueue.removeFirst();
				assert x.next == null && !x.processed;
				final Node<V>[] successors = x.successors;
				final List<Node<V>> ys = new ArrayList<>(successors.length);
				for (Node<V> y : successors) {
					if (y.processed) ys.add(y);
				}
				final Node<V> z;
				if (ys.isEmpty()) {
					z = null;
				} else {
					final Node<V> a = LeastCommonAncestor.lca(ys);
					z = a == null ? ys.get(0) : a;
				}
				if (z != null) {
					newEdge(x, z.representant);
					processed(x);
				} else {
					workqueue.addLast(x);
				}
			}
		}
		
	}
	
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> SinkpathPostDominators<V, E> compute(DirectedGraph<V, E> graph, Iterable<Set<V>> sccs) {
		
		final SinkpathPostDominators<V, E> isinkdom = new SinkpathPostDominators<>(graph);
		isinkdom.initialize(sccs, true);
		isinkdom.sinkUp();
		isinkdom.sinkDown();

		return isinkdom;
	}

	private void sinkDown() {
		sinkDown(this.graph, this.workset);
	}
	
	public void sinkDown(DirectedGraph<V, E> graph, TreeSet<Node<V>> workset) {
		{
			while (!workset.isEmpty()) {
				final Node<V> x = workset.pollFirst();
				assert x.inWorkset;
				x.inWorkset = false;
				
				final Node<V>[] successors = x.successors;
				final Node<V> a = LeastCommonAncestor.lca(successors);
				final Node<V> z = a == null ? null : a.representant;
				assert x.next != null || z == null;
				if (z != x.next) {
					final GraphWalker<Node<V>, ISinkdomEdge<Node<V>>> rdfs = new GraphWalker<Node<V>, ISinkdomEdge<Node<V>>>(new EdgeReversedGraph<>(result)) {
						@Override
						public void discover(Node<V> node) {}

						@Override
						public void finish(Node<V> node) {
							for (E e : graph.incomingEdgesOf(node.v) ) {
								final V vn = e.getSource();
								final Node<V> n = vToNode.get(vn);
								assert (!n.isRelevant) || (n.inWorkset == workset.contains(n));
								if (n.isRelevant && !n.inWorkset) {
									workset.add(n);
									n.inWorkset = true;
								}
							}
						}
						
						@Override
						public boolean traverse(Node<V> node, ISinkdomEdge<Node<V>> edge) {
							return true;
						}
					};
					rdfs.traverseDFS(x);
					newEdge( x, z);
				}
			}
		}
	}

	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> SinkpathPostDominators<V, E> computeFixed(DirectedGraph<V, E> graph) {
		
		final KosarajuStrongConnectivityInspector<V, E> sccInspector = new KosarajuStrongConnectivityInspector<V, E>(graph);
		final List<Set<V>> sccs = sccInspector.stronglyConnectedSets();
		
		final SinkpathPostDominators<V, E> isinkdom = new SinkpathPostDominators<>(graph);
		isinkdom.initialize(sccs, false);
		isinkdom.sinkUp();
		isinkdom.sinkDownFixed();

		return isinkdom;
	}
	
	private void sinkDownFixed() {
		{
			final LinkedList<Node<V>> work  = new LinkedList<>();
			
			for (Node<V> n : vToNode.values()) {
				if (n.next != null && n.isRelevant) work.add(n);
			}
			
			ArrayList<Node<V>> workLeft  = new ArrayList<>(work.size());
			ArrayList<Node<V>> workRight = new ArrayList<>(work.size());
			workLeft.addAll(work);
			work.clear();

			
			boolean changed;
			do {
				changed = false;
				for (int i = 0; i < workLeft.size(); i++) {
					final Node<V> x =  workLeft.get(i);
					final Node<V>[] successors = x.successors;
					final Node<V> a = LeastCommonAncestor.lca(successors);
					final Node<V> z = a == null ? null : a.representant;
					assert x.next != null || z == null;
					final boolean done = (z == null);
					if (z != x.next) {
						changed = true;
						if (!x.changed) {
							x.changed = true;
							work.add(x);
						}
						
						x.next = z;
					}
					if (!done) workRight.add(x);
				}
				workLeft  = workRight;
				workRight = new ArrayList<>(workLeft.size());
			} while (changed);
			
			for (Node<V> x : work) {
				result.removeOutgoingEdgesOf(x);
				final Node<V> z = x.next;
				if (z != null) result.addEdgeUnsafe(x, z, new ISinkdomEdge<SinkpathPostDominators.Node<V>>(x, z));
			}
		}
	}
}
