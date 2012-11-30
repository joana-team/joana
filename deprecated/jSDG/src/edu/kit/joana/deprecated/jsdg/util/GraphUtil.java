/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.util;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;

/**
 * Utility class for graph related problems. Includes methods to create subgraphs
 * of a given graph.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class GraphUtil {

	private GraphUtil() {
	}

	/**
	 * Creates a subgraph of a given graph that contains only nodes contained in a given set.
	 * @param <T> Node type of the given graph.
	 * @param g The graph a subgraph is created from.
	 * @param nodesOk Set of nodes the subgraph should contain. All other nodes are omitted.
	 * @return Subgraph of a the graph <tt>g</tt> that contains only nodes contained in the set <tt>nodesOk</tt>.
	 */
	public static <T> NumberedGraph<T> createSubGraph(NumberedGraph<T> g, IntSet nodesOk) {
		return new SubGraph<T>(g, nodesOk);
	}

	/**
	 * Creates a subgraph of a given graph that contains only nodes contained in a given set.
	 * @param <T> Node type of the given graph.
	 * @param g The graph a subgraph is created from.
	 * @param nodes Set of nodes the subgraph should contain. All other nodes are omitted.
	 * @return Subgraph of a the graph <tt>g</tt> that contains only nodes contained in the set <tt>nodes</tt>.
	 */
	public static <T> NumberedGraph<T> createSubGraph(NumberedGraph<T> g, Set<T> nodes) {
		MutableIntSet nodesOk = new BitVectorIntSet();
		for (T node : nodes) {
			int num = g.getNumber(node);
			if (num < 0) {
				throw new IllegalArgumentException("Node " + node + " is not contained in " + g);
			}

			nodesOk.add(num);
		}

		return new SubGraph<T>(g, nodesOk);
	}

	/**
	 * Creates a subgraph of a given graph that contains all nodes that are reachable
	 * from a set of given nodes.
	 * @param <T> Node type of the given graph.
	 * @param g The graph a subgraph is created from.
	 * @param nodes Set of nodes used as the starting point.
	 * @return Subgraph containing all nodes reachable from an element of <tt>nodes</tt>.
	 */
	public static <T> NumberedGraph<T> createReachableFromSubGraph(NumberedGraph<T> g, Set<T> nodes) {
		IntSet reachFrom = findNodesReachableFrom(g, nodes);

		return new SubGraph<T>(g, reachFrom);
	}

	/**
	 * Computes a subgraph that contains all nodes reachable from the given node
	 * @param <T> Node type of the given graph.
	 * @param g The graph a subgraph is created from.
	 * @param node The node used as starting point.
	 * @return Subgraph containing all nodes reachable from <tt>node</tt>.
	 */
	public static <T> NumberedGraph<T> createReachableFromSubGraph(NumberedGraph<T> g, T node) {
		IntSet nodesOk = GraphUtil.findNodesReachableFrom(g, node);

		return new SubGraph<T>(g, nodesOk);
	}


	/**
	 * Computes a subgraph that contains all nodes from where a node of a given set of nodes may be reached.
	 * @param <T> Node type of the given graph.
	 * @param g The graph a subgraph is created from.
	 * @param nodes The set of nodes containing nodes that have to be reached.
	 * @return Subgraph containing all nodes that transitively reach <tt>node</tt>.
	 */
	public static <T> NumberedGraph<T> createReachingToSubGraph(NumberedGraph<T> g, Set<T> nodes) {
		IntSet reachTo = findNodesReachingTo(g, nodes);

		return new SubGraph<T>(g, reachTo);
	}

	/**
	 * Computes a subgraph that contains all nodes from where the a given node may be reached.
	 * @param <T> Node type of the given graph.
	 * @param g The graph a subgraph is created from.
	 * @param node The node that has to be reached.
	 * @return Subgraph containing all nodes that transitively reach <tt>node</tt>.
	 */
	public static <T> NumberedGraph<T> createReachingToSubGraph(NumberedGraph<T> g, T node) {
		IntSet reachTo = findNodesReachingTo(g, node);

		return new SubGraph<T>(g, reachTo);
	}

	/**
	 * Computes a subgraph that contains all nodes reachable from a node of a given set of
	 * nodes as well as all nodes from where the given nodes may be reached.
	 * @param <T> Node type of the given graph.
	 * @param g The graph a subgraph is created from.
	 * @param nodes The set nodes used as starting point.
	 * @return Subgraph containing all nodes reachable from <tt>nodes</tt> and all nodes
	 * that transitively reach <tt>nodes</tt>.
	 */
	public static <T> NumberedGraph<T> createReachFromAndToSubGraph(NumberedGraph<T> g, Set<T> nodes) {
		IntSet reachFrom = findNodesReachableFrom(g, nodes);
		IntSet reachTo = findNodesReachingTo(g, nodes);

		IntSet reachFromAndTo = reachFrom.union(reachTo);

		return new SubGraph<T>(g, reachFromAndTo);
	}

	/**
	 * Computes a subgraph that contains all nodes reachable from the given
	 * node as well as all nodes from where the given node may be reached.
	 * @param <T> Node type of the given graph.
	 * @param g The graph a subgraph is created from.
	 * @param node The node used as starting point.
	 * @return Subgraph containing all nodes reachable from <tt>node</tt> and all nodes
	 * that transitively reach <tt>node</tt>.
	 */
	public static <T> NumberedGraph<T> createReachFromAndToSubGraph(NumberedGraph<T> g, T node) {
		IntSet reachFrom = findNodesReachableFrom(g, node);
		IntSet reachTo = findNodesReachingTo(g, node);

		IntSet reachFromAndTo = reachFrom.union(reachTo);

		return new SubGraph<T>(g, reachFromAndTo);
	}

	/**
	 * Search all nodes in a graph that are reachable from a given set of nodes.
	 * @param <T> Node type of the given graph.
	 * @param graph The graph a subgraph is created from.
	 * @param nodes Set of nodes used as starting point.
	 * @return Set of node numbers for all nodes reachable from the nodes in the set <tt>nodes</tt>.
	 */
	public static <T> IntSet findNodesReachableFrom(NumberedGraph<T> graph, Set<T> nodes) {
		BitVectorIntSet bvNodes = new BitVectorIntSet();

		for (T node : nodes) {
			bvNodes.add(graph.getNumber(node));

			searchSuccs(graph, bvNodes, node);
		}

		return bvNodes;
	}

	/**
	 * Search all nodes in a graph that are reachable from a given node.
	 * @param <T> Node type of the given graph.
	 * @param g The graph a subgraph is created from.
	 * @param node The node used as starting point.
	 * @return Set of node numbers for all nodes reachable from the node <tt>node</tt>.
	 */
	public static <T> IntSet findNodesReachableFrom(NumberedGraph<T> graph, T node) {
		BitVectorIntSet bvNodes = new BitVectorIntSet();

		bvNodes.add(graph.getNumber(node));

		searchSuccs(graph, bvNodes, node);

		return bvNodes;
	}

	private static <T> void searchSuccs(NumberedGraph<T> graph, BitVectorIntSet bvNodes, T node) {

		for (Iterator<T> it = graph.getSuccNodes(node); it.hasNext();) {
			T succ = it.next();
			int num = graph.getNumber(succ);
			if (!bvNodes.contains(num)) {
				bvNodes.add(num);
				searchSuccs(graph, bvNodes, succ);
			}
		}
	}

	/**
	 * Search all nodes in a graph that transitively reach a node from a given set of nodes.
	 * @param <T> Node type of the given graph.
	 * @param g The graph a subgraph is created from.
	 * @param nodes The node that should be reached.
	 * @return Set of node numbers for all nodes that reach the given node <tt>node</tt>.
	 */
	public static <T> IntSet findNodesReachingTo(NumberedGraph<T> graph, Set<T> nodes) {
		BitVectorIntSet bvNodes = new BitVectorIntSet();

		for (T node : nodes) {
			bvNodes.add(graph.getNumber(node));

			searchPreds(graph, bvNodes, node);
		}

		return bvNodes;
	}

	/**
	 * Search all nodes in a graph that transitively reach a given node.
	 * @param <T> Node type of the given graph.
	 * @param g The graph a subgraph is created from.
	 * @param node The node that should be reached.
	 * @return Set of node numbers for all nodes that reach the given node <tt>node</tt>.
	 */
	public static <T> IntSet findNodesReachingTo(NumberedGraph<T> graph, T node) {
		BitVectorIntSet bvNodes = new BitVectorIntSet();

		bvNodes.add(graph.getNumber(node));

		searchPreds(graph, bvNodes, node);

		return bvNodes;
	}

	private static <T> void searchPreds(NumberedGraph<T> graph, BitVectorIntSet bvNodes, T node) {
		for (Iterator<T> it = graph.getPredNodes(node); it.hasNext();) {
			T pred = it.next();
			int num = graph.getNumber(pred);
			if (!bvNodes.contains(num)) {
				bvNodes.add(num);
				searchPreds(graph, bvNodes, pred);
			}
		}
	}

}
