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



/**
 *
 * @author  Dennis Giffhorn
 * @deprecated  see <code>IncrementalSummarySlicer</code>
 */
public class IncrementalSummaryBackward extends IncrementalSummarySlicer {

    /**
     * Creates a new instance of IncrementalSummaryBackward
     */
    public IncrementalSummaryBackward(SDG g) {
        super(g);
    }


    protected boolean isTransitiveStart(SDGNode n) {
		return n.getKind() == SDGNode.Kind.FORMAL_OUT;
	}

    protected Collection<SDGEdge> edgesToTraverse(SDGNode node) {
        return graph.incomingEdgesOf(node);
    }

    protected SDGNode reachedNode(SDGEdge edge) {
        return edge.getSource();
    }

    protected SDGNode startedNode(SDGEdge edge) {
        return edge.getTarget();
    }

    protected boolean downwardsEdge(SDGEdge edge) {
        return edge.getKind() == SDGEdge.Kind.PARAMETER_OUT;
    }

    protected boolean phase2Edge(SDGEdge edge) {
        if (!edge.getKind().isSDGEdge()
                || edge.getKind() == SDGEdge.Kind.PARAMETER_IN
                || edge.getKind() == SDGEdge.Kind.CALL
                || omittedEdges.contains(edge.getKind())) {

            return false;
        }

        return true;
    }

    protected boolean summaryFound(SDGNode reached) {
    	return reached.getKind() == SDGNode.Kind.FORMAL_IN;
    }
}
