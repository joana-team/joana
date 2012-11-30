/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.util.Collection;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CallGraph;


/**
 * @author grafj
 *
 */
public class CallGraphForward extends CallGraphSlicer {

    public CallGraphForward(CallGraph g, Set<SDGEdge.Kind> omit) {
        super(g, omit);
    }

    public CallGraphForward(CallGraph g) {
        super(g);
    }

    protected Collection<SDGEdge> edgesToTraverse(SDGNode node) {
        return this.g.outgoingEdgesOf(node);
    }

    protected SDGNode reachedNode(SDGEdge edge) {
        return edge.getTarget();
    }
}
