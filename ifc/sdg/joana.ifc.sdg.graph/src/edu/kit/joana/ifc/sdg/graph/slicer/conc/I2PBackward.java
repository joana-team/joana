/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;



/**
 *
 * @author hammer, giffhorn
 */
public class I2PBackward extends Iterative2PhaseSlicer {

    public I2PBackward(SDG g) {
        super(g);
    }

    protected Collection<SDGEdge> edgesToTraverse(SDGNode node) {
        return this.g.incomingEdgesOf(node);
    }

    protected SDGNode reachedNode(SDGEdge edge) {
        return edge.getSource();
    }

    protected Phase phase1() {
        return new Phase() {
            public boolean follow(SDGEdge e) {
                return true;
            }

            public boolean saveInOtherWorklist(SDGEdge e) {
                return e.getKind() == SDGEdge.Kind.PARAMETER_OUT;
            }
        };
    }

    protected Phase phase2() {
        return new Phase() {
            public boolean follow(SDGEdge e) {
                return e.getKind() != SDGEdge.Kind.PARAMETER_IN
                        && e.getKind() != SDGEdge.Kind.CALL;
            }

            public boolean saveInOtherWorklist(SDGEdge e) {
                return e.getKind() == SDGEdge.Kind.INTERFERENCE ||
                       e.getKind() == SDGEdge.Kind.FORK_IN ||
                       e.getKind() == SDGEdge.Kind.FORK_OUT ||
                       e.getKind() == SDGEdge.Kind.FORK;
            }
        };
    }
}
