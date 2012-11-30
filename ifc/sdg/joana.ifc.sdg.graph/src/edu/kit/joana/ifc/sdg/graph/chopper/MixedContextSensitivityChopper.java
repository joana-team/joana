/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*****************************************************************************
 *                                                                           *
 *   Mixed Context-Sensitivity Chopping Algorithm MCC                        *
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
import java.util.Stack;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * The MixedContextSensitivityChopper (MSC) is an almost context-sensitive same-level chopper.
 * It improves on the CIC {@link ContextInsensitiveChopper} by using two-phase slicing.
 *
 * This algorithm should be used if very fast same-level chops of very large SDGs are needed.
 * Otherwise the usage of the {@link SummaryMergedChopper} is preferable.
 *
 * @author Bernd Nuernberger
 * @since 1.5
 */
public class MixedContextSensitivityChopper extends Chopper {

    /**
     * Constructs a MSC for a given SDG.
     * @param g   The SDG to operate on. Can be null. Can be a cSDG.
     */
    public MixedContextSensitivityChopper(SDG g) {
        super(g);
    }

    /**
     * Nothing to do here.
     */
    protected void onSetGraph() { }

	/**
     * Computes an almost context-sensitive same-level chop from <code>sourceSet</code> to <code>sinkSet</code>.
     *
     * @param sourceSet  The source criterion set. Should not contain null, should not be empty.
     * @param sinkSet    The target criterion set. Should not contain null, should not be empty.
     * @return           The chop (a HashSet).
     * @throws InvalidCriterionException, if the nodes in sourceSet and targetSet do not belong to the same procedure
     * or if one of the sets is empty.
     */
    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet)
    throws InvalidCriterionException {// the bouncer
    	if (!Chopper.testSameLevelSetCriteria(sourceSet, sinkSet)) {
    		// Thou shall not pass!
    		throw new InvalidCriterionException("This is not a same-level chopping criterion: "+sourceSet+", "+sinkSet);
        }

        // === initialization ===
        Stack<SDGNode> worklist = new Stack<SDGNode>();
        HashSet<SDGNode> visitedBackward = new HashSet<SDGNode>();
        HashSet<SDGNode> visitedForward = new HashSet<SDGNode>();

        // === backward phase ===

        worklist.addAll(sinkSet);
        visitedBackward.addAll(sinkSet);

        while (!worklist.empty()) {
            SDGNode n = worklist.pop();
            for (SDGEdge e : sdg.incomingEdgesOf(n)) {

                SDGEdge.Kind kind = e.getKind();
                if (!kind.isSDGEdge()
                		|| kind.isThreadEdge()
                		|| kind == SDGEdge.Kind.PARAMETER_IN
                		|| kind == SDGEdge.Kind.CALL)
                    continue;

                SDGNode m = e.getSource();

                if (visitedBackward.add(m)) {
                    worklist.push(m);
                }
            }
        }

        // === forward phase ===

        sourceSet.retainAll(visitedBackward);
        worklist.addAll(sourceSet);
        visitedForward.addAll(sourceSet);

        while (!worklist.empty()) {
            SDGNode n = worklist.pop();
            for (SDGEdge e : sdg.outgoingEdgesOf(n)) {

                SDGEdge.Kind kind = e.getKind();
                if (!kind.isSDGEdge()
                		|| kind.isThreadEdge()
                		|| kind == SDGEdge.Kind.PARAMETER_OUT)
                    continue;

                SDGNode m = e.getTarget();

                if (visitedBackward.contains(m) && visitedForward.add(m)) {
                    worklist.push(m);
                }
            }
        }

        return visitedForward;
    }
}
