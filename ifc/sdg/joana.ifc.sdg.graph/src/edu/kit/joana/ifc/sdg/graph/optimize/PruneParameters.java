/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.optimize;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

public class PruneParameters {

	private final Logger debug = Log.getLogger(Log.L_SDG_GRAPH_DEBUG);
	private final SDG g;
	private final Collection<SDGNode> worklist;

	public PruneParameters(SDG g) {
		this.g = g;
		worklist = new LinkedHashSet<SDGNode>();
	}

	/**
	 * initializes worklist with parameter nodes
	 */
	private void initWorklist() {
		// initialize worklist with parameter nodes
		for (Iterator<SDGNode> i = g.vertexSet().iterator(); i.hasNext();) {
			SDGNode currNode = i.next();
			if (currNode.isParameter() /*
										 * && !(currNode.getKind() ==
										 * SDGNode.Kind.EXIT)
										 */) {
				// add parameter nodes to worklist
				worklist.add(currNode);
			}
		}
	}

	/**
	 * Returns <tt>true</tt> if this list of edges contains a edge of kind kind
	 * 
	 * @param outEdges
	 *            list of edges
	 * @param kind
	 *            type of edge
	 * @return <tt>true</tt> if this list of edges contains a edge of specified
	 *         kind
	 */
	private static boolean hasEdge(Collection<SDGEdge> outEdges, SDGEdge.Kind kind) {
		for (SDGEdge e : outEdges) {
			if (e.getKind() == kind)
				return true;
		}
		return false;
	}

	/**
	 * Returns <tt>true</tt> if this list of edges contains a CE edge from a
	 * CALL or ENTRY node.
	 * 
	 * @param inEdges
	 *            list of edges
	 * @return <tt>true</tt> if this list of edges contains a CE edge from a
	 *         CALL or ENTRY node
	 */
	private static boolean isFirstLevelNode(Collection<SDGEdge> inEdges) {
		for (SDGEdge e : inEdges) {
			SDGNode currNode = e.getSource();
			if ((e.getKind() == Kind.CONTROL_DEP_EXPR)
					&& (currNode.getKind() == SDGNode.Kind.CALL || currNode.getKind() == SDGNode.Kind.ENTRY))
				return true;
		}
		return false;
	}

	/**
	 * adds nodes to worklist if opposite vertex is of expected kind and
	 * connecting edge is the expected kind of edge
	 * 
	 * @param inEdges
	 *            list of edges
	 * @param n
	 *            actual node
	 * @param edgeKind
	 *            specified kind of edge
	 */
	private void addNodes(Collection<SDGEdge> inEdges, SDGNode n, SDGEdge.Kind edgeKind) {
		// look for parameter edges in edgeList
		for (SDGEdge e : g.edgesOf(n)) {// edgeList) {
			SDGNode currNode = e.getOppositeVertex(n);
			if (currNode.isParameter()) {// e.getKind() == edgeKind) {
				// add opposite vertex of parameter edge to worklist
				// if specified parameter node
				worklist.add(currNode);
			}
		}
	}

	/**
	 * adds parameter-node (father) to worklist if connected by incoming CE edge
	 * 
	 * @param inEdges
	 *            list of edges
	 */
	private void addParent(Collection<SDGEdge> inEdges) {
		// look for incoming CE edge in edgeList
		for (SDGEdge e : inEdges) {
			SDGNode currNode = e.getSource();
			/*
			 * && ! ( currNode . getKind ( ) == SDGNode . Kind . EXIT )
			 */
			if (e.getKind() == Kind.CONTROL_DEP_EXPR && currNode.isParameter()) {
				// add source-vertex of incoming CE edge to worklist
				worklist.add(currNode);
				// return worklist; // remove line if multiple fathers possible
			}
		}

	}

	private void processIn(SDGNode n, Collection<SDGEdge> inEdges, Collection<SDGEdge> outEdges, SDGEdge.Kind param,
			boolean removeAll) {
		// look for datadependence edges in outEdges
		boolean ddExists = hasEdge(outEdges, Kind.DATA_DEP) || hasEdge(outEdges, Kind.DATA_HEAP)
				|| hasEdge(outEdges, Kind.DATA_ALIAS);
		// look for ParameterIn edges in inEdges
		boolean piExists = hasEdge(inEdges, param);
		// look for CE-edges in outEdges
		boolean ceExists = hasEdge(outEdges, Kind.CONTROL_DEP_EXPR);
		// lookup if father is CALL or ENTRY node
		boolean inFirstLevel = isFirstLevelNode(inEdges);
		if (((!ddExists || !piExists) && !ceExists) && (removeAll || !inFirstLevel)) {
			if (!hasNoOther(param, n))
				System.out.println();
			// add actual-in nodes from incoming parameter-in-edges
			// to worklist
			addNodes(inEdges, n, param);
			remove(inEdges, n);
		}
	}

	private boolean hasNoOther(SDGEdge.Kind param, SDGNode n) {
		for (SDGEdge edge : g.edgesOf(n)) {
			if (edge.getKind() != param
					&& (edge.getKind() != Kind.DATA_DEP || edge.getKind() != Kind.DATA_HEAP || edge.getKind() != Kind.DATA_ALIAS)
					&& (edge.getKind() != Kind.CONTROL_DEP_EXPR || edge.getTarget() != n))
				if (!g.containsEdge(edge.getTarget(), edge.getSource()) && edge.getKind() != Kind.SUMMARY)
					return false;
		}
		return true;
	}

	private void processOut(SDGNode n, Collection<SDGEdge> inEdges, Collection<SDGEdge> outEdges, SDGEdge.Kind param,
			boolean removeAll) {
		// look for datadependence edges in inEdges
		boolean ddExists = hasEdge(inEdges, Kind.DATA_DEP) || hasEdge(inEdges, Kind.DATA_HEAP)
				|| hasEdge(inEdges, Kind.DATA_ALIAS);
		// look for ParameterOut edges in outEdges
		boolean poExists = hasEdge(outEdges, param);
		// look for CE-edges in outEdges
		boolean ceExists = hasEdge(outEdges, Kind.CONTROL_DEP_EXPR);
		// lookup if father is CALL or ENTRY node
		boolean inFirstLevel = isFirstLevelNode(inEdges);
		if (((!ddExists || !poExists) && !ceExists) && (removeAll || !inFirstLevel)) {
			assert hasNoOther(param, n);
			// add actual-out nodes from outgoing parameter-out-edges
			// to worklist
			addNodes(outEdges, n, param);
			remove(inEdges, n);
		}
	}

	private void remove(Collection<SDGEdge> inEdges, SDGNode n) {
		// add parent node to worklist
		addParent(inEdges);
		debug.outln("remove Node : " + n.toString());
		g.removeVertex(n);
		worklist.remove(n); // make sure we didn't re-add n
	}

	/**
	 * slicing-algorithm by krinke
	 * 
	 * @param g
	 *            SDG
	 * @return graph after slicing algorithm with probably reduced number of
	 *         nodes
	 */
	public void prune() {
		initWorklist();
		// print initial size of worklist
		if (debug.isEnabled()) {
			debug.outln("\ninitial Size of worklist : " + worklist.size());
			debug.outln("Nodes: ");
			debug.outln(g.vertexSet().size());
			debug.outln("Edges: ");
			debug.outln(g.edgeSet().size() + "\n");
		}

		Collection<SDGEdge> inEdges, outEdges;
		while (!worklist.isEmpty()) {
			// remove one node from worklist
			Iterator<SDGNode> i = worklist.iterator();
			SDGNode currNode = i.next();
			i.remove();
			assert g.containsVertex(currNode);
			inEdges = g.incomingEdgesOf(currNode);
			outEdges = g.outgoingEdgesOf(currNode);

			if (currNode.getKind() == SDGNode.Kind.FORMAL_IN) {
				// formal_in-node
				processIn(currNode, inEdges, outEdges, Kind.PARAMETER_IN, false);
			} else if (currNode.getKind() == SDGNode.Kind.ACTUAL_IN) {
				// actual_in-node
				processOut(currNode, inEdges, outEdges, Kind.PARAMETER_IN, false);
			} else if (currNode.getKind() == SDGNode.Kind.ACTUAL_OUT) {
				// actual_out-node
				processIn(currNode, inEdges, outEdges, Kind.PARAMETER_OUT, true);
			} else {
				assert currNode.getKind() == SDGNode.Kind.FORMAL_OUT || currNode.getKind() == SDGNode.Kind.EXIT;
				// formal_out-node and exit, do not remove the exit node!
				processOut(currNode, inEdges, outEdges, Kind.PARAMETER_OUT,
						currNode.getKind() == SDGNode.Kind.FORMAL_OUT);
			}
		}
		if (debug.isEnabled()) {
			debug.outln("Nodes: ");
			debug.outln(g.vertexSet().size());
			debug.outln("Edges: ");
			debug.outln(g.edgeSet().size());
		}
	} // end prune ()

	public static void main(String[] args) throws IOException {
		SDG g = SDG.readFrom(args[0]);
		new PruneParameters(g).prune();
		new PruneParameters(g).prune();
		StringBuilder sb = new StringBuilder(args[0]);
		int pos = sb.lastIndexOf(".pdg");
		sb.insert(pos, ".pruned");
		PrintStream out = new PrintStream(new File(sb.toString()));
		out.print(SDGSerializer.toPDGFormat(g));
		out.close();
	}
}
