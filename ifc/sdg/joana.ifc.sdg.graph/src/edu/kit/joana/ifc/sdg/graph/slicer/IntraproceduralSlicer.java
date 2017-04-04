/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * @author  Dennis Giffhorn
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public abstract class IntraproceduralSlicer implements Slicer {
    protected SDG graph;
    /**
     * Creates a new instance of IntraproceduralSlicer
     */
    public IntraproceduralSlicer(SDG g) {
        graph = g;
    }

    public void setGraph(SDG graph) {
        this.graph = graph;
    }
    
    /**
     * Returns either the {@link SDG#outgoingEdgesOf(SDGNode) outgoing} or the {@link SDG#outgoingEdgesOf(SDGNode) incoming} edges of
     * the given node, depending on whether the overriding subclass implements a forward or a backward slicer.
     * @param n node to retrieve the adjacent edges of
     * @return the {@link SDG#outgoingEdgesOf(SDGNode) outgoing} or the {@link SDG#outgoingEdgesOf(SDGNode) incoming} edges of
     * the given node, depending on whether the overriding subclass implements a forward or a backward slicer
     */
    protected abstract Collection<SDGEdge> adjacentEdges(SDGNode n);
    
    /**
     * Returns either the {@link SDGEdge#getTarget() target} or the {@link SDGEdge#getSource() source} node of
     * the given edge, depending on whether the overriding subclass implements a forward or a backward slicer.
     * @param e edge to retrieve the adjacent node of
     * @return the {@link SDGEdge#getTarget() target} or the {@link SDGEdge#getSource() source} node of
     * the given edge, depending on whether the overriding subclass implements a forward or a backward slicer.
     */
    protected abstract SDGNode adjacentNode(SDGEdge e);
    
    /**
     * Returns whether the slicer shall traverse the given edge. Standardly, the slicer is allowed to traverse
     * the given edge if it is a valid {@link SDGEdge#isIntraproceduralEdge()} intraprocedural} {@link SDGEdge#isSDGEdge sdg edge}. Subclasses are
     * allowed to be more restrictive, for example they can restrict the allowed edges to data flow edges (which then results in an intraprocedural data slice).<br>
     * Only override this method if you want to restrict the traverseable edges and know what you are doing!
     * @param e edge to be checked
     * @return whether the slicer shall traverse the given edge
     */
    protected boolean isAllowedEdge(SDGEdge e) {
    	return e.getKind().isIntraSDGEdge();
    }
    
    
    public Collection<SDGNode> slice(Collection<SDGNode> nodes) {
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        HashSet<SDGNode> slice = new HashSet<SDGNode>();

        worklist.addAll(nodes);
        slice.addAll(nodes);

        while (!worklist.isEmpty()) {
            SDGNode next = worklist.poll();

            for (SDGEdge e : adjacentEdges(next)) {
            	final SDGNode adjacentNode = adjacentNode(e);
                if (isAllowedEdge(e) && !slice.contains(adjacentNode)){

                    worklist.add(adjacentNode);
                    slice.add(adjacentNode);
                }
            }
        }

        return slice;
    }
    
    public Collection<SDGNode> slice(SDGNode criterion) {
		return slice(Collections.singleton(criterion));
	}

}
