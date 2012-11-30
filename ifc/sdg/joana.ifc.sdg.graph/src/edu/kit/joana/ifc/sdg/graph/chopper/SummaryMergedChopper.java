/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*****************************************************************************
 *                                                                           *
 *   Summary-Merged Chopping Algorithm SMC                                   *
 *                                                                           *
 *   This file is part of the chopper package of the sdg library.            *
 *   The used chopping algorithms are desribed in Jens Krinke's PhD thesis   *
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
 * The SummaryMergedChopper (SMC) is a context-sensitive same-level chopper.
 *
 * The SMC is the most practical context-sensitive same-level chopper.
 * It computes an initial intra-procedural chop and extends it gradually by computing intra-procedural
 * chops in the procedures called by the nodes in the chop. It improves on the CSC {@link ContextSensitiveChopper}
 * by merging chopping criteria that belong to the same procedure to one single criterion
 *
 * @author Bernd Nuernberger, Dennis Giffhorn
 * @since 1.5
 */
public class SummaryMergedChopper extends Chopper {
    // The employed intra-procedural chopper
    private IntraproceduralChopper intraChopper;

    /**
     * Constructs a SMC for a given SDG.
     * @param g   The SDG to operate on. Can be null. Can be a cSDG.
     */
    public SummaryMergedChopper(SDG g) {
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
    throws InvalidCriterionException{
    	// the bouncer
    	if (!Chopper.testSameLevelSetCriteria(sourceSet, sinkSet)) {
    		// Thou shall not pass!
    		throw new InvalidCriterionException("This is not a same-level chopping criterion: "+sourceSet+", "+sinkSet);
        }

        // compute an initial intra-procedural chop between source and sink
        Collection<SDGNode> chop = intraChopper.chop(sourceSet, sinkSet);

        // retrieve all ({formal-in} / {formal-out}) pairs that belong to a procedure called in the chop
        LinkedList<Criterion> worklist = new LinkedList<Criterion>();
        worklist.addAll(getSummarySites(chop));
        Set<SDGNodeTuple> visitedTuples = new HashSet<SDGNodeTuple>();

        // gradually add intra-procedural chops for the procedures called in `chop'
        while(!worklist.isEmpty()) {
            // build the next chopping criterion
            LinkedList<SDGNode> auxSourceSet = new LinkedList<SDGNode>();
            LinkedList<SDGNode> auxSinkSet = new LinkedList<SDGNode>();

            Criterion next = worklist.poll();

            for (SDGNode s : next.source) {
                for (SDGNode t : next.target) {
                    SDGNodeTuple tmp = new SDGNodeTuple(s, t);

                    if (visitedTuples.add(tmp)) {
                        auxSourceSet.add(s);
                        auxSinkSet.add(t);
                    }
                }
            }

            if (auxSourceSet.isEmpty() || auxSinkSet.isEmpty()) continue;

            // compute a chop for the new criterion
            // update the worklist with new ({formal-in} / {formal-out}) pairs
            Collection<SDGNode> newChop = intraChopper.chop(auxSourceSet, auxSinkSet);
            chop.addAll(newChop);
            worklist.addAll(getSummarySites(newChop));
        }

        return chop;
    }
}
