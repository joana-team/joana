/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*****************************************************************************
 *                                                                           *
 *   Context-Sensitive Chopping Algorithm CSC                                *
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
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;


/**
 * The ContextSensitiveChopper (CSC) is a context-sensitive same-level chopper.
 *
 * It computes an initial intra-procedural chop and extends it gradually by computing intra-procedural
 * chops in the procedures called by the nodes in the chop.
 *
 * This algorithm is fairly practical, albeit not as fast as the {@link SummaryMergedChopper}.
 *
 * @author Bernd Nuernberger
 * @since 1.5
 */
public class ContextSensitiveChopper extends Chopper {
    /**
     * The intra-procedural chopper used by the algorithm.
     */
    private IntraproceduralChopper intraChopper;

    /**
     * Constructs a CSC for a given SDG.
     * @param g  The SDG to operate on. Can be null. Can be a cSDG.
     */
    public ContextSensitiveChopper(SDG g) {
    	super(g);
    }

    /**
     * Re-initializes the intra-procedural chopper.
     * Triggered by {@link Chopper#setGraph(SDG)}.
     */
	protected void onSetGraph() {
		if (intraChopper == null) {
	        intraChopper = new IntraproceduralChopper(sdg);

		} else {
			intraChopper.setGraph(sdg);
		}
	}

	/**
     * Computes a context-sensitive same-level chop from <code>sourceSet</code> to <code>sinkSet</code>.
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

        // compute the initial chop
        Collection<SDGNode> currentChop = intraChopper.chop(sourceSet,sinkSet);

        // retrieve all (formal-in / formal-out) pairs that belong to a summary edge in the chop
        LinkedList<SDGNodeTuple> worklist = new LinkedList<SDGNodeTuple>();
        worklist.addAll(getSummaryEdgePairs(currentChop));
        Set<SDGNodeTuple> visitedTuples = new HashSet<SDGNodeTuple>();

        // gradually add the chops for each (formal-in / formal-out) pairs
        while(!worklist.isEmpty()) {
            SDGNodeTuple pair = worklist.pop();

            if (!visitedTuples.contains(pair)) {
                visitedTuples.add(pair);
                SDGNode m = pair.getFirstNode();
                SDGNode n = pair.getSecondNode();

                // update the worklist with new (formal-in / formal-out) pairs
                Collection<SDGNode> chop = intraChopper.chop(m, n);
                currentChop.addAll(chop);
                worklist.addAll(getSummaryEdgePairs(chop));
            }
        }

        return currentChop;
    }
}
