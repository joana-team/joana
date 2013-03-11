/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph;

import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/** Represents folded call graphs.
 * Recursive calls are folded.
 *
 * @see CallGraph
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class FoldedCallGraph extends CallGraph implements Folded {

	/** Creates a new instance of BipartiteCallGraph from a given IPDG.
     *
     * @param graph  The IPDG whose folded bipartite call graph is needed.
     */
    public FoldedCallGraph(CallGraph g) {
        super();
        addAllVertices(g.vertexSet());
        addAllEdges(g.edgeSet());
    }

    /** Returns 'true' if the given call node is a recursive call, else 'false'.
     */
    public boolean isRecursiveCall(SDGNode call) {
        // it is recursive if it is folded
        return isFolded(call);
    }

    /** Maps a given vertex to this graph.
     * Returns the vertex itself or a fold vertex that folds this vertex.
     *
     * @param node  The vertex to map.
     */
    public SDGNode map(SDGNode node) {
    	assert containsVertex(node): node;
        for (SDGEdge e : outgoingEdgesOf(node)) {

            if (e.getKind() == SDGEdge.Kind.FOLD_INCLUDE) {
                return e.getTarget();
            }
        }

        return node;
    }

    /** Unmaps a fold vertex.
     * Delivers _one_ of the vertices the fold vertex folds.
     * More concrete, it returns the first such vertex found.
     * Needed for mapping of fold vertices between different folded graphs.
     *
     * @param node  The fold vertex.
     */
    public SDGNode unmap(SDGNode node) {
        if (!containsVertex(node)) {
            throw new RuntimeException(node+" is not in the graph");
        }

        for (SDGEdge e : incomingEdgesOf(node)) {
            if (e.getKind() == SDGEdge.Kind.FOLD_INCLUDE) {
                return e.getSource();
            }
        }
        return node;
    }

    /** Returns the fold vertex of a given folded vertex.
     *
     * @param folded  The folded vertex whose fold vertex is wanted.
     * @return  The fold vertex or 'null', if not found.
     */
    public SDGNode getFoldNode(SDGNode folded) {
        // simply traverse the outgoing fold-include edge
        for (SDGEdge edge : this.outgoingEdgesOf(folded)) {
            if (edge.getKind() == SDGEdge.Kind.FOLD_INCLUDE) {
                return edge.getTarget();
            }
        }
        return null;
    }

	public List<SDGNode> getFoldedNodesOf(SDGNode fold) {
        if (fold.getKind() != SDGNode.Kind.FOLDED) {
            return new LinkedList<SDGNode>();
        }

        LinkedList<SDGNode> list = new LinkedList<SDGNode>();

        for (SDGEdge e : incomingEdgesOf(fold)) {
            if (e.getKind() == SDGEdge.Kind.FOLD_INCLUDE) {
                list.add(list.size(), e.getSource());
            }
        }

        return list;
	}
}
