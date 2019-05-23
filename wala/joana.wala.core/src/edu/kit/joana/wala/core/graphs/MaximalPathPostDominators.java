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
import java.util.HashSet;
import java.util.Iterator;
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
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators.Node;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class MaximalPathPostDominators<V  extends IntegerIdentifiable, E extends KnowsVertices<V>> {
	public static class IMaximalDomEdge<V> extends DomEdge<V> {
		public IMaximalDomEdge(V source, V target) {
			super(source, target);
		}
	};

	public static final class Node<V extends IntegerIdentifiable> extends AbstractPseudoTreeNode<V, Node<V>> {
		public Node(V v) {
			super(v);
		}
		
	}
	
	private final AbstractJoanaGraph<Node<V>, IMaximalDomEdge<Node<V>>> result;
	private final DirectedGraph<V, E> graph;
	private final Map<V, Node<V>> vToNode;
	private final LinkedList<Node<V>> workqueue;
	
	private final Set<Node<V>> workset;
	
	@SuppressWarnings("serial")
	private MaximalPathPostDominators(DirectedGraph<V, E> graph) {
		this.graph = graph;
		final IMaximalDomEdge<Node<V>> dummy = new IMaximalDomEdge<MaximalPathPostDominators.Node<V>>(null, null);
		@SuppressWarnings("unchecked")
		final Class<IMaximalDomEdge<Node<V>>> clazz = (Class<IMaximalDomEdge<Node<V>>>) dummy.getClass();
		this.result = new AbstractJoanaGraph<Node<V>, IMaximalDomEdge<Node<V>>>(
			new EdgeFactory<Node<V>, IMaximalDomEdge<Node<V>>>() {
				@Override
				public IMaximalDomEdge<Node<V>> createEdge(Node<V> sourceVertex, Node<V> targetVertex) {
					return new IMaximalDomEdge<>(sourceVertex, targetVertex); 
				}
			},
			() -> new HashMap<>(graph.vertexSet().size()),
			(IMaximalDomEdge<Node<V>>[] es) ->  ModifiableNotTightArraySet.own(es, clazz),
			clazz
		) {	};
		
		this.vToNode = new HashMap<>();
		
		for (V v : graph.vertexSet()) {
			final Node<V> n = new Node<V>(v);
			vToNode.put(v, n);
			result.addVertexUnsafe(n);
		}
		
		this.workqueue = new LinkedList<>();
		this.workset = new HashSet<>();
	}
	
	public Map<V, Node<V>> getvToNode() {
		return vToNode;
	}
	
	public AbstractJoanaGraph<Node<V>, IMaximalDomEdge<Node<V>>> getResult() {
		return result;
	}
	
	void newEdge(Node<V> x, Node<V> z) {
		if (x.next != null) result.removeOutgoingEdgesOf(x);
		if (z != null) result.addEdgeUnsafe(x, z, new IMaximalDomEdge<MaximalPathPostDominators.Node<V>>(x, z));
		x.next = z;
	}
	
	private void initialize(boolean initializeWorkset) {
		for (Map.Entry<V, Node<V>> entry : vToNode.entrySet()) {
			final V v = entry.getKey();
			final Node<V> x = entry.getValue();
			assert x.v == v;
			{
				final Set<E> successorEs = graph.outgoingEdgesOf(v);
				final int successorEsSize = successorEs.size();
				switch (successorEsSize) {
					case 0: break;
					case 1: {
						final Node<V> z = vToNode.get(successorEs.iterator().next().getTarget());
						if (z != x) {
							newEdge(x, z);
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
	
	private void maximalUpWorkSet() {
		{
			while (!workset.isEmpty()) {
				final Node<V> x; {
					final Iterator<Node<V>> it = workset.iterator();
					x = it.next();
					it.remove();
				}
				assert x.inWorkset;
				x.inWorkset = false;
				
				final Node<V>[] successors = x.successors;
				final Node<V> a = LeastCommonAncestor.lca(successors);
				final Node<V> z = (a == null || a == x) ? null : a;
				assert (z != x.next) ? z != null : true;
				if (z != x.next) {
					final GraphWalker<Node<V>, IMaximalDomEdge<Node<V>>> rdfs = new GraphWalker<Node<V>, IMaximalDomEdge<Node<V>>>(new EdgeReversedGraph<>(result)) {
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
						public boolean traverse(Node<V> node, IMaximalDomEdge<Node<V>> edge) {
							return true;
						}
					};
					rdfs.traverseDFS(x);
					newEdge(x, z);
				}
			}
		}
		
	}
	
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> MaximalPathPostDominators<V, E> compute(DirectedGraph<V, E> graph) {
		
		final MaximalPathPostDominators<V, E> isinkdom = new MaximalPathPostDominators<>(graph);
		isinkdom.initialize(true);
		isinkdom.maximalUpWorkSet();

		return isinkdom;
	}

	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> MaximalPathPostDominators<V, E> computeFixed(DirectedGraph<V, E> graph) {
		
		final MaximalPathPostDominators<V, E> isinkdom = new MaximalPathPostDominators<>(graph);
		isinkdom.initialize(false);
		isinkdom.maximalUpFixed();

		return isinkdom;
	}

	private void maximalUpFixed() {
		{
			Node<V> oldest = null;
			while (!workqueue.isEmpty()) {
				final Node<V> x = workqueue.removeFirst();
				assert x.next == null;
				if (x == oldest) return;
				
				if (oldest == null) {
					oldest = x;
				}
				final Node<V>[] successors = x.successors;
				final Node<V> a = LeastCommonAncestor.lca(successors);
				final Node<V> z = (a == null || a == x) ? null : a;
				if (z != null) {
					newEdge(x, z);
					oldest = null;
				} else {
					workqueue.addLast(x);
				}
			}
		}
	}
}
