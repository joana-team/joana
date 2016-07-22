/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.sdg;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;

/**
 * Builds a reduced control-flow graph from a given SDG by removing 'uninteresting' nodes.
 * @author Martin Mohr
 */
public final class ReducedCFGBuilder {

	/**
	 * private constructor to prevent instantiation of this class
	 */
	private ReducedCFGBuilder() {
	}


	/**
	 * Given an SDG, extracts a reduced control-flow graph from it. How this reduction is performed is
	 * controlled by a SDGNodePredicate parameter which determines when a node is 'interesting'. The algorithm
	 * starts by extracting the control flow graph (see {@link ICFGBuilder}, {@link CFG}). Afterwards, it successively
	 * searches for a node which is not 'interesting' and removes it while connecting all its adjacent nodes. This reduction
	 * step is performed until there is no uninteresting node anymore.
	 * @param sdg sdg to reduce
	 * @param nodePred node predicate which decides whether a given sdg node is 'interesting'
	 * @return the control-flow graph of the given sdg, which only contains nodes considered 'interesting' by the given
	 * node predicate
	 */
	public static CFG extractReducedCFG(JoanaGraph sdg, SDGNodePredicate nodePred) {
		CFG cfgBase = ICFGBuilder.extractICFG(sdg);
		CFG cfgRet = new CFG();

		cfgRet.addAllVertices(cfgBase.vertexSet());
		cfgRet.addAllEdges(cfgBase.edgeSet());


		boolean change;
		do {
			change = false;
			SDGNode uNode = findUninterestingNode(cfgRet, nodePred);
			if (uNode != null) {
				removeNode(cfgRet, uNode);
				change = true;
			}
		} while (change);

		stripTrivialCycles(cfgRet);
		removeEntryExitExcConnections(cfgRet);
		return cfgRet;
	}

	/**
	 * Extracts the reduced control-flow graph from the given sdg using the {@link DefaultSDGNodePredicate}.
	 * @param sdg sdg to extract the reduced control-flow graph from
	 * @return reduced control-flow graph of the given sdg
	 */
	public static CFG extractReducedCFG(JoanaGraph sdg) {
		return extractReducedCFG(sdg, new DefaultSDGNodePredicate());
	}


	/**
	 * Removes a node from the given control-flow graph. The node itself is deleted and afterwards each of its
	 * control-flow-predecessors is connected with each of its control-flow-successors.
	 * @param cfg control-flow graph from which the given node is to be removed
	 * @param toRemove node to remove
	 */
	private static void removeNode(CFG cfg, SDGNode toRemove) {
		final List<SDGEdge> edgesToAdd = new LinkedList<SDGEdge>();
		final List<SDGEdge> edgesToRemove = new LinkedList<SDGEdge>();
		for (SDGEdge eIncoming : cfg.getIncomingEdgesOfKind(toRemove, SDGEdge.Kind.CONTROL_FLOW)) {
			for (SDGEdge eOutgoing : cfg
					.getOutgoingEdgesOfKind(toRemove, SDGEdge.Kind.CONTROL_FLOW)) {
				edgesToAdd.add(new SDGEdge(eIncoming.getSource(), eOutgoing.getTarget(),
						SDGEdge.Kind.CONTROL_FLOW));
				edgesToRemove.add(eIncoming);
				edgesToRemove.add(eOutgoing);
			}
		}

		cfg.addAllEdges(edgesToAdd);
		cfg.removeAllEdges(edgesToRemove);
		cfg.removeVertex(toRemove);
	}

	/**
	 * Finds a node in the given control-flow graph, which is not 'interesting' according to the given node predicate
	 * and returns it. If there is no such node, {@code null} is returned.
	 * @param cfgRet control-flow graph in which to look for 'uninteresting' nodes
	 * @param nodePred node predicate which decides whether a node is 'interesting'
	 * @return an 'uninteresting' node in the given control-flow graph, if there is one, {@code null} otherwise
	 */
	private static SDGNode findUninterestingNode(CFG cfgRet, SDGNodePredicate nodePred) {
		SDGNode ret = null;
		for (SDGNode n : cfgRet.vertexSet()) {
			if (!nodePred.isInteresting(n)) {
				ret = n;
			}
		}
		return ret;
	}
	
	/**
	 * Removes from the given CFG all control-flow edges for which source and target are the same.
	 * These edges can occur while removing uninteresting nodes: If an interesting node is connected
	 * to itself via a path of uninteresting nodes, this manifests itself in such a trivial cycle.
	 * @param cfg control-flow graph from which the cycles are to be removed
	 */
	private static void stripTrivialCycles(CFG cfg) {
		Collection<SDGEdge> toRemove = new LinkedList<SDGEdge>();
		for (SDGEdge e : cfg.edgeSet()) {
			if (e.getSource().equals(e.getTarget())) {
				toRemove.add(e);
			}
		}
		
		cfg.removeAllEdges(toRemove);
	}
	
	/**
	 * Removes all control-flow edges which connect an entry node with an exit or exception node
	 * from the given control-flow graph.
	 * @param cfg the control-flow graph from which the fore-mentioned connections are to be removed.
	 */
	private static void removeEntryExitExcConnections(CFG cfg) {
		Collection<SDGEdge> toRemove = new LinkedList<SDGEdge>();
		Collection<SDGEdge> toAdd = new LinkedList<SDGEdge>();
		for (SDGEdge e : cfg.edgeSet()) {
			if (e.getSource().getKind() == SDGNode.Kind.ENTRY 
					&& (e.getTarget().getKind() == SDGNode.Kind.EXIT || e.getTarget().getKind() == SDGNode.Kind.FORMAL_OUT)) {
				toRemove.add(e);
				toAdd.add(new SDGEdge(e.getSource(), e.getTarget(), SDGEdge.Kind.HELP));
			}
		}
		
		cfg.removeAllEdges(toRemove);
		cfg.addAllEdges(toAdd);
	}

}
