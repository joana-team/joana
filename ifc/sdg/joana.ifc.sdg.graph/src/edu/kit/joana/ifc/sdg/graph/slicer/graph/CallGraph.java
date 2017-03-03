/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;



/** A convenience class for call graphs.
 *
 * @see JoanaGraph
 * @author Dennis Giffhorn
 */
public class CallGraph extends JoanaGraph {

	public boolean addEdge(SDGEdge edge) {
		if (edge.getKind() != SDGEdge.Kind.CALL
				&& edge.getKind() != SDGEdge.Kind.FORK
				&& edge.getKind() != SDGEdge.Kind.FOLD_INCLUDE)
			throw new IllegalArgumentException("I am a call graph. Don't add "+edge.getKind()+"-edges!");

		return super.addEdge(edge);
	}

	public boolean addVertex(SDGNode vertex) {
		if (vertex.getKind() != SDGNode.Kind.CALL
				&& vertex.getKind() != SDGNode.Kind.FOLDED
				&& vertex.getKind() != SDGNode.Kind.ENTRY) {
			throw new IllegalArgumentException("I am a call graph. Don't add "+vertex.getKind()+"-nodes!");
		}

		return super.addVertex(vertex);
	}

	/** Creates a subgraph form a sub-collection of the graph's nodes.
	 *
	 * @param vertices  The collection of nodes.
	 */
	public CallGraph subgraph(Collection<SDGNode> vertices) {
		CallGraph g = new CallGraph();

		for (SDGNode n : vertices) {
			g.addVertex(n);
		}

		for (SDGNode n : vertices) {
			for (SDGEdge e : outgoingEdgesOf(n)) {
				if (vertices.contains(e.getTarget())) {
					g.addEdge(e);
				}
			}
		}

		return g;
	}

	/** Returns the entry node of the procedure a given node belongs to.
	 * If the entry node is folded, the fold node is returned.
	 *
	 * @param node  The node.
	 */
	public SDGNode getEntry(SDGNode node){
		throw new UnsupportedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.graph.JoanaGraph#getNodesOfProcedure(edu.kit.joana.ifc.sdg.graph.SDGNode)
	 */
	@Override
	public Set<SDGNode> getNodesOfProcedure(SDGNode proc) {
		throw new UnsupportedOperationException();
	}
}
