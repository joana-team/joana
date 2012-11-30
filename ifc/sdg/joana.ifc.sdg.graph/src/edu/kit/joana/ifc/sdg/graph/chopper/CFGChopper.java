/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;


/**
 * The CFGChopper computes intra-procedural chops in CFGs.
 *
 * It computes an intra-procedural backward slice for the target criterion and
 * then an intra-procedural forward slice for the source criterion on the sub-graph
 * spanned by the nodes in the backward slice.
 */
public class CFGChopper {
	private CFG g;

    /**
     * Constructs an intra-procedural chopper for a given CFG.
     * @param g   The CFG to operate on.
     */
    public CFGChopper(CFG g) {
    	setGraph(g);
    }

    /**
     */
    public void setGraph(CFG g) {
    	this.g = g;
    }

	/**
     * Computes an intra-procedural chop from <code>source</code> to <code>sink</code>.
     *
     * @param source  The source criterion. Should not be null.
     * @param sink    The target criterion. Should not be null.
     * @return        The chop (a HashSet).
     */
    public Collection<SDGNode> chop(SDGNode source, SDGNode sink) {
        return chop(Collections.singleton(source), Collections.singleton(sink));
    }

    /**
     * Computes an intra-procedural chop from <code>source</code> to <code>sink</code>.
     *
     * @param source  The source criterion. Should not be null.
     * @param sink    The target criterion. Should not be null.
     * @return        The chop (a HashSet).
     */
    public Collection<SDGNode> chop(Collection<SDGNode> source, Collection<SDGNode> sink) {
        // === initialization ===
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        Set<SDGNode> visitedBackward = new HashSet<SDGNode>();
        Set<SDGNode> visitedForward = new HashSet<SDGNode>();

        // === backward phase ===

        worklist.addAll(sink);
        visitedBackward.addAll(sink);

        while (!worklist.isEmpty()) {
            SDGNode n = worklist.pop();

            for (SDGEdge e : g.incomingEdgesOf(n)) {
                SDGEdge.Kind kind = e.getKind();
                if (!kind.isIntraproceduralEdge()) continue;

                SDGNode m = e.getSource();

                if (visitedBackward.add(m)) {
                    worklist.push(m);
                }
            }
        }

        // === forward phase ===
        for (SDGNode s : source) {
        	if (visitedBackward.contains(s)) {
                worklist.add(s);
                visitedForward.add(s);
        	}
        }

        while (!worklist.isEmpty()) {
            SDGNode n = worklist.pop();

            for (SDGEdge e : g.outgoingEdgesOf(n)) {

                SDGEdge.Kind kind = e.getKind();
                if (!kind.isIntraproceduralEdge()) continue;

                SDGNode m = e.getTarget();

                if (visitedBackward.contains(m) && visitedForward.add(m)) {
                    worklist.push(m);
                }
            }
        }

        return visitedForward;
    }
}
