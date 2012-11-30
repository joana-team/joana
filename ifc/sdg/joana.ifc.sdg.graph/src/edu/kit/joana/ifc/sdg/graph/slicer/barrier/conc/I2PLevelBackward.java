/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.barrier.conc;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;



/** An iterated-2-phase level forward slicer.
 * It counts the minimal number of edge traversals each visited node is away from the slicing criterion.
 *
 * @author Dennis Giffhorn, Christian Hammer
 */
public class I2PLevelBackward extends I2PLevelSlicer {

    /** Creates a new instance of CSSBackward */
    /**
     *
     */
    public I2PLevelBackward(SDG g) {
        super(g);

    }

    protected Collection<SDGEdge> edgesToTraverse(SDGNode node) {
        return this.g.incomingEdgesOf(node);
    }

    protected SDGNode reachedNode(SDGEdge edge) {
        return edge.getSource();
    }

    protected EdgePredicate phase1Predicate() {
        return new EdgePredicate() {
            public boolean phase1() {
                return true;
            }

            public boolean follow(SDGEdge e) {
                return true;
            }

            public boolean saveInOtherWorklist(SDGEdge e) {
                return e.getKind() == SDGEdge.Kind.PARAMETER_OUT;
            }
        };
    }

    protected EdgePredicate phase2Predicate() {
        return new EdgePredicate() {
            public boolean phase1() {
                return false;
            }

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
