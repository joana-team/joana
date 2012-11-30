/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.barrier;

import java.util.Collection;
import java.util.Collections;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphModifier;


/** Offers a standard treatment of barriers for barrier slicing and chopping.
 * A given barrier is stored and can be used to determine affected summary edges.
 * Alternatively, both the barrier nodes and the affected (summary) edges can be passed as a whole.
 *
 * @author giffhorn
 */
public class BarrierManager {
	private Collection<SDGEdge> blockedSummaryEdges = Collections.emptySet(); // the blocked summary edges
	private Collection<SDGNode> barrier = Collections.emptySet();             // the barrier

	/** Sets the given set of nodes as the barrier and computes the affected summary edges in the given sdg.
	 *
	 * @param barrier  A set of nodes.
	 * @param sdg      A sdg, should be the sdg to which the barrier nodes belong.
	 */
	public void setBarrier(SDG sdg, Collection<SDGNode> barrier) {
		this.barrier = barrier;
		this.blockedSummaryEdges = GraphModifier.blockSummaryEdges(sdg, barrier);
	}

	/** Sets the given set of nodes as the barrier and the given set of edges as the blocked summary edges.
	 * The passed edges are not verified in any way, so it is possible to pass an arbitrary set of edges.
	 *
	 * @param barrier               A set of nodes.
	 * @param blockedSummaryEdges   A set of edges.
	 */
	public void setBarrier(Collection<SDGNode> barrier, Collection<SDGEdge> blockedSummaryEdges) {
		this.barrier = barrier;
		this.blockedSummaryEdges = blockedSummaryEdges;
	}

	/** Checks if the given node is in the stored barrier.
	 *
	 * @param n  A node.
	 * @return true if n is in the stored barrier.
	 */
	public boolean isBlocked(SDGNode n) {
		return barrier.contains(n);
	}

	/** Checks if the given edge is in the stored set of blocked edges.
	 *
	 * @param e  An edge.
	 * @return true if e is in the stored edge set.
	 */
	public boolean isBlocked(SDGEdge e) {
		return blockedSummaryEdges.contains(e);
	}

	public String toString() {
		String str = super.toString()+"\n";
		str += "Barrier: "+barrier+"\n";
		str += "Edges: "+blockedSummaryEdges+"\n";
		return str;
	}
}
