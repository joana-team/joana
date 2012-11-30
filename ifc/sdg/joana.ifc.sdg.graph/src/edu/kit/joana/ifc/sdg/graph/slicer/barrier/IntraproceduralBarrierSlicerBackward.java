/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.barrier;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/** An intra-procedural barrier backward slicer.
 * A barrier slicer receives a set of nodes - the barrier - which it will not trespass.
 *
 * @author Dennis Giffhorn
 */
public class IntraproceduralBarrierSlicerBackward extends IntraproceduralBarrierSlicer {

    public IntraproceduralBarrierSlicerBackward(SDG graph) {
        super(graph);
    }

    protected Collection<SDGEdge> edgesToTraverse(SDGNode node) {
        return this.g.incomingEdgesOf(node);
    }

    protected SDGNode reachedNode(SDGEdge edge) {
        return edge.getSource();
    }
}
