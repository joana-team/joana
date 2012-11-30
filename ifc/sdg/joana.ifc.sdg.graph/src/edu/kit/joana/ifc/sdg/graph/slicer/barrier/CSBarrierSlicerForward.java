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


/** A 2-phase barrier forward slicer.
 * A barrier slicer receives a set of nodes - the barrier - which it will not trespass.
 *
 * @author Dennis Giffhorn
 */
public class CSBarrierSlicerForward extends CSBarrierSlicer {

    public CSBarrierSlicerForward(SDG graph) {
        super(graph);
    }

    protected Collection<SDGEdge> edgesToTraverse(SDGNode node) {
        return this.g.outgoingEdgesOf(node);
    }

    protected SDGNode reachedNode(SDGEdge edge) {
        return edge.getTarget();
    }

    protected EdgePredicate phase1Predicate() {
        return new EdgePredicate() {
            public boolean phase1() {
                return true;
            }

            public boolean follow(SDGEdge e) {
                return e.getKind() != SDGEdge.Kind.PARAMETER_IN
                	&& e.getKind() != SDGEdge.Kind.CALL;
            }

            public boolean saveInOtherWorklist(SDGEdge e) {
                return !follow(e);
            }
        };
    }

    protected EdgePredicate phase2Predicate() {
        return new EdgePredicate() {
            public boolean phase1() {
                return false;
            }

            public boolean follow(SDGEdge e) {
                return e.getKind() != SDGEdge.Kind.PARAMETER_OUT;
            }

            public boolean saveInOtherWorklist(SDGEdge e) {
                return false;
            }
        };
    }
}
