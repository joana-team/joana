/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.ContextManager;

/**
 * A two-phase slice that gets a context as slicing criterion and
 * computes the precise slice for that context.
 * It works by using call strings in the first phase to ascend to
 * calling methods context-sensitively.
 *
 * -- Created on January 31, 2006
 *
 * @author  Dennis Giffhorn
 */
public class C2PForward extends Context2PhaseSlicer {

    /**
     * Creates a new instance of Context2PhaseSlicer
     *
     * @param g The SDG to slice.
     */
    public C2PForward(SDG g) {
        super(g);
    }

    public C2PForward(SDG g, ContextManager m) {
        super(g,m);
    }

    protected Collection<SDGEdge> edgesToTraverse(SDGNode node) {
        return sdg.outgoingEdgesOf(node);
    }

    protected SDGNode reachedNode(SDGEdge edge) {
        return edge.getTarget();
    }

    protected boolean ascend(SDGEdge edge) {
        return edge.getKind() == SDGEdge.Kind.PARAMETER_OUT;
    }

    protected boolean descend(SDGEdge edge) {
        return edge.getKind() == SDGEdge.Kind.PARAMETER_IN || edge.getKind() == SDGEdge.Kind.CALL;
    }
}
