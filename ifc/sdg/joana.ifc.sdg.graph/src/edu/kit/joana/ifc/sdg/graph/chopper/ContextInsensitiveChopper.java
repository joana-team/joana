/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*****************************************************************************
 *                                                                           *
 *   Context-Insensitive Chopping Algorithm CIC                              *
 *                                                                           *
 *   This file is part of the chopper package of the sdg library.            *
 *   The used chopping algorithms are described in Jens Krinke's PhD thesis   *
 *   "Advanced Slicing of Sequential and Concurrent Programs".               *
 *                                                                           *
 *   authors:                                                                *
 *   Bernd Nuernberger, nuerberg@fmi.uni-passau.de                           *
 *                                                                           *
 *****************************************************************************/

package edu.kit.joana.ifc.sdg.graph.chopper;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;



/**
 * The ContextInsensitiveChopper (CIC) is a context-insensitive same-level chopper.
 *
 * It computes a context-insensitive backward slice for the target criterion and
 * then a context-insensitive forward slice for the source criterion on the sub-graph
 * spanned by the nodes in the backward slice.
 *
 * This algorithm was only implemented for completeness. It should not be used by an application.
 * Use {@link SummaryMergedChopper} if you need same-level chops.
 *
 * @author Bernd Nuernberger
 * @since 1.5
 */
public class ContextInsensitiveChopper extends Chopper {

    /**
     * Constructs a chopper for a given SDG.
     * @param g   The SDG to operate on. Can be null. Can be a cSDG.
     */
    public ContextInsensitiveChopper(SDG g) {
        super(g);
    }

    /**
     * Has nothing to do.
     */
	protected void onSetGraph() { }

    /**
     * Computes a context-insensitive same-level chop from <code>sourceSet</code> to <code>sinkSet</code>.
     *
     * @param sourceSet  The source criterion set. Should not contain null, should not be empty.
     * @param sinkSet    The target criterion set. Should not contain null, should not be empty.
     * @return           The chop (a HashSet).
     * @throws InvalidCriterionException, if the nodes in sourceSet and targetSet do not belong to the same procedure
     * or if one of the sets is empty.
     */
    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet)
    throws InvalidCriterionException {
    	// the bouncer
    	if (!Chopper.testSameLevelSetCriteria(sourceSet, sinkSet)) {
    		// Thou shall not pass!
    		throw new InvalidCriterionException("This is not a same-level chopping criterion: "+sourceSet+", "+sinkSet);
        }

        // === initialization ===
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        Set<SDGNode> visitedBackward = new HashSet<SDGNode>();
        Set<SDGNode> visitedForward = new HashSet<SDGNode>();

        worklist.addAll(sinkSet);
        visitedBackward.addAll(sinkSet);

        // === backward slice ===

        while (!worklist.isEmpty()) {
            SDGNode n = worklist.pop();
            for (SDGEdge e : sdg.incomingEdgesOf(n)) {

            	// traverse dependences only and remain in the thread.
                if (!e.getKind().isSDGEdge() || e.getKind().isThreadEdge()) continue;

                SDGNode m = e.getSource();
                if (visitedBackward.add(m)) {
                    worklist.push(m);
                }
            }
        }

        // === forward slice ===

        sourceSet.retainAll(visitedBackward);
        worklist.addAll(sourceSet);
        visitedForward.addAll(sourceSet);

        while (!worklist.isEmpty()) {
            SDGNode n = worklist.pop();
            for (SDGEdge e : sdg.outgoingEdgesOf(n)) {

            	// traverse dependences only and remain in the thread.
                if (!e.getKind().isSDGEdge() || e.getKind().isThreadEdge()) continue;

                SDGNode m = e.getTarget();
                if (visitedBackward.contains(m) && visitedForward.add(m)) {
                    worklist.push(m);
                }
            }
        }

        return visitedForward;
    }
}
