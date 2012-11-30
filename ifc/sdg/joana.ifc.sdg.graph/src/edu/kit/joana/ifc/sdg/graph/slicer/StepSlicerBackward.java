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

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 *
 *
 * -- Created on September 6, 2005
 *
 * @author  Dennis Giffhorn
 */
public class StepSlicerBackward extends StepSlicer {

    /**
     * Creates a new instance of SummarySlicerBackward
     */
    public StepSlicerBackward(SDG graph, Set<SDGEdge.Kind> omit, Set<SDGEdge.Kind> step) {
        super(graph, omit, step);
    }

    public StepSlicerBackward(SDG graph, Set<SDGEdge.Kind> omit) {
        super(graph, omit);
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
                return !omittedEdges.contains(e.getKind());
            }

            public boolean saveInOtherWorklist(SDGEdge e) {
                return e.getKind() == SDGEdge.Kind.PARAMETER_OUT;
            }

            public String toString() {
                return "phase 1";
            }
        };
    }

    protected EdgePredicate phase2Predicate() {
        return new EdgePredicate() {
            public boolean phase1() {
                return false;
            }

            public boolean follow(SDGEdge e) {
                return e.getKind() != SDGEdge.Kind.PARAMETER_IN &&
                        e.getKind() != SDGEdge.Kind.CALL &&
                        !omittedEdges.contains(e.getKind());
            }

            public boolean saveInOtherWorklist(SDGEdge e) {
                return e.getKind() == SDGEdge.Kind.INTERFERENCE ||
                        ((e.getKind() == SDGEdge.Kind.DATA_DEP || e.getKind() == SDGEdge.Kind.DATA_HEAP
                        		|| e.getKind() == SDGEdge.Kind.DATA_ALIAS) &&
                         e.getSource().getProc() != e.getTarget().getProc());
            }

            public String toString() {
                return "phase 2";
            }
        };
    }
}
