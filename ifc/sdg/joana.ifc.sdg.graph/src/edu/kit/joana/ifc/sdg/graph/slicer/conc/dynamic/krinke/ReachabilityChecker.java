/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke;

import java.util.HashSet;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.ContextComparator;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCFG;


/**
 * A class for examining whether a context can reach a second context in an ICFG.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class ReachabilityChecker {
    private FoldedCFG foldedIcfg;

    public ReachabilityChecker(FoldedCFG g) {
        foldedIcfg = g;
    }


    /**
     * Tests if a source context can reach a target context.
     *
     * @param source
     *                  The source context.
     * @param target
     *                  The target.
     */
    public boolean reaches(DynamicContext source, DynamicContext target) {
        if (source.equals(target)) return true;

        // the common suffix of source and target
        Suffix suffix = suffix(source, target);

        // 1. if source is suffix of target, then source reaches target
        ContextComparator comp = DynamicContextManager.contextComparator();
        if (comp.compare(source, suffix.suffix) == 0) {
            return true;
        }

        // 2. if the suffix contains a folded node, then source can reach target
        if (!suffix.suffix.isEmpty()) {
            for (int i = 0; i < suffix.suffix.size(); i++) {
                if (suffix.suffix.get(i).getKind() == SDGNode.Kind.FOLDED) {
                    return true;
                }
            }
        }

        // 3. compute if source reaches target
        SDGNode from = source.getNode();

        if (from != suffix.nextSourceNode) {
            from = getReturnNode(suffix.nextSourceNode);
        }

        // special case: if the next node of source is null, then the main method
        // is finished
        if (from == null){
            return false;
        }

        return reachable(from, suffix.nextTargetNode);
    }

    private Suffix suffix(DynamicContext source, DynamicContext target) {
        Suffix suffix = new Suffix();
        LinkedList<SDGNode> suff = new LinkedList<SDGNode>();
        SDGNode theNode = null;
        int s = source.size() -1;
        int t = target.size() -1;

        while (s >= 0 && t >= 0) {
            suffix.nextSourceNode = source.get(s);
            suffix.nextTargetNode = target.get(t);

            if (source.get(s) != target.get(t)) {
                break;

            } else {
                suff.addFirst(source.get(s));
                s--;
                t--;
            }
        }

        theNode = suff.poll();
        suffix.suffix  = new DynamicContext(suff, theNode, source.getThread());
        return suffix;
    }

    private SDGNode getReturnNode(SDGNode call) {
        if (call.getKind() == SDGNode.Kind.FOLDED) {
            return call;
        }

        for (SDGEdge e : foldedIcfg.outgoingEdgesOf(call)) {
            if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
                return e.getTarget();
            }
        }

        // special case, when main method returns
        return null;
    }

    private boolean reachable(SDGNode from, SDGNode to) {
        // worklist and marked list
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        HashSet<SDGNode> marked = new HashSet<SDGNode>();

        //context.setNode(foldedIcfg.map(context.getNode()));
        //target.setNode(foldedIcfg.map(target.getNode()));

        // init both lists with 'context'
        worklist.add(from);
        marked.add(from);

        // Build all contexts resulting from traversing outgoing control-flow,
        // call and return edges. If 'next' and 'target' are equal, return 'true'.
        // If worklist is empty, return 'false'.
        while (!worklist.isEmpty()) {
            SDGNode next = worklist.poll();

            // test whether 'target' is reached
            if (next == to) {
                return true;
            }

            // traverse forward, but do not cross thread boundaries
            // source and target contexts are always in the same thread
            for (SDGEdge edge : foldedIcfg.outgoingEdgesOf(next)) {
                if (edge.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
                    SDGNode successor = edge.getTarget();

                    // add new context to worklist
                    if (marked.add(successor)) {
                        worklist.add(successor);
                    }
                }
            }
        }

        // source context doesn't reach target
        return false;
    }

    static class Suffix {
        DynamicContext suffix;
        SDGNode nextSourceNode;
        SDGNode nextTargetNode;
    }
}
