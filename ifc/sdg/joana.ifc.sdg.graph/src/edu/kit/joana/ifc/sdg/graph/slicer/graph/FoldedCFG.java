/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;



/** A convenience class for folded graphs.
 *
 * @see SDG
 * @see Folded
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class FoldedCFG extends CFG implements Folded {

    /** Creates a new instance of CallGraph from a given IPDG.
     *
     * @param graph  The IPDG.
     */
    public FoldedCFG(CFG g) {
        super();
        addAllVertices(g.vertexSet());
        addAllEdges(g.edgeSet());
    }

    /** Maps a vertex to its fold vertex and returns the latter.
     * If the vertex isn't folded, it will be returned itself.
     *
     * @param node  The vertex.
     */
    public SDGNode map(SDGNode node) {
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
            throw new RuntimeException();
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
        for (SDGEdge edge : outgoingEdgesOf(folded)) {
            if (edge.getKind() == SDGEdge.Kind.FOLD_INCLUDE) {
                return edge.getTarget();
            }
        }

        return null;
    }

    /** Returns all vertices a given fold vertex folds.
     *
     * @param fold  The fold vertex.
     * @return  A list with all folded vertices.
     */
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
    
    @Override
    public Set<SDGNode> getNodesOfProcedure(final SDGNode node) {
    	final SDGNode entry = getEntry(node);
    	final int procedure = node.getProc();
    	
    	final Queue<SDGNode> wl = new LinkedList<>();
    	final Set<SDGNode> found = new HashSet<>();
    	final Set<SDGNode> foldedFound = new HashSet<>();
    	
    	wl.offer(entry);
    	{ 
    		SDGNode next;
    		while ((next = wl.poll()) != null) {
				if (next.getKind() == SDGNode.Kind.FOLDED) {
					if (foldedFound.add(next)) {
						for (SDGEdge e : outgoingEdgesOf(next)) {
							wl.offer(e.getTarget());
						}
						for (SDGNode foldedNode : getFoldedNodesOf(next)) {
							wl.offer(foldedNode);
						}
					}
				} else if (next.getProc() == procedure ) {
					if (found.add(next)) {
		    			for (SDGEdge e : outgoingEdgesOf(next)) {
		    				final SDGNode successor = e.getTarget();
		    				wl.offer(successor);
		    			}
					}
				}
    		}
    	}
    	
    	List<SDGNode> foundSlow;
    	Set<SDGNode> foundSlowSet = new HashSet<>();
    	assert ((foundSlow = getNodesOfProcedureSlow(node)) != null && (foundSlowSet.addAll(foundSlow) || true) && (foundSlowSet.size() == found.size()) && foundSlow.containsAll(found) && found.containsAll(foundSlow));
    	
        return found;
    }
}
