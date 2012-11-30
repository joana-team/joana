/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper.barrier;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.Criterion;
import edu.kit.joana.ifc.sdg.graph.chopper.InvalidCriterionException;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.BarrierManager;


/**
 * Implements a variant of Krinke's summary-merged chopper, which additionally can
 * be given a barrier of nodes it shall not trespass.
 *
 * @author Bernd Nuernberger, Dennis Giffhorn
 * @since 1.5
 */
public class SummaryMergedBarrierChopper extends BarrierChopper {
    private BarrierManager barrier;

    // The employed intra-procedural chopper, shares the above BarrierManager
    private IntraproceduralBarrierChopper intraChopper;

    /**
     * Constructs a chopper for a given SDG.
     * @param g The SDG to operate on.
     */
    public SummaryMergedBarrierChopper(SDG g) {
        super(g);
    }

    /**
     * Sets a new SDG for the chopper. For precise chops the given SDG
     * is flattened.
     * @param g The new SDG to chop on.
     */
    protected void onSetGraph() {
    	if (barrier == null) {
        	barrier = new BarrierManager();
    	}

    	if (intraChopper == null) {
    		intraChopper = new IntraproceduralBarrierChopper(sdg);
    		intraChopper.setBarrier(barrier);
    	} else {
    		intraChopper.setGraph(sdg);
    	}
    }

	public void setBarrier(Collection<SDGNode> barrier) {
    	this.barrier.setBarrier(sdg, barrier);
	}

	public void setBarrier(Collection<SDGNode> barrier, Collection<SDGEdge> blockedSummaryEdges) {
		this.barrier.setBarrier(barrier, blockedSummaryEdges);
	}

	public void setBarrier(BarrierManager barrier) {
    	this.barrier = barrier;
		intraChopper.setBarrier(barrier);
	}

    /**
     * Computes a summary-merged chop on sets for <code>sourceSet</code> and <code>sinkSet</code>.
     *
     * It does a same-level chop, thus the source and target criteria nodes
     * must all be in the same procedure.
     *
     * @param sourceSet The source criterion set.
     * @param sinkSet The target criterion set.
     * @return Set of nodes of the corresponding chop.
     * @throws InvalidCriterionException
     *         If source and target criteria are not all in the same procedure,
     *         of if source or target set is empty.
     */
    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet) {
        // === initialization ===
        Chopper.testSameLevelSetCriteria(sourceSet, sinkSet);

        // compute an intra-procedural chop between source and sink,
        // then process the traversed summary edges
        Collection<SDGNode> currentChop = intraChopper.chop(sourceSet,sinkSet);
        doIntraChops(currentChop);

        return currentChop;
    }

    private void doIntraChops(Collection<SDGNode> chop) {
        LinkedList<Criterion> worklist = new LinkedList<Criterion>();
        Set<SDGNodeTuple> visitedTuples = new HashSet<SDGNodeTuple>();
        worklist.addAll(getSummarySites(chop));

        // === gradually add chops for each summary edge ===

        while(!worklist.isEmpty()) {
            // sets of chop criteria for the intra-procedural chop to be done
            LinkedList<SDGNode> auxSourceSet = new LinkedList<SDGNode>();
            LinkedList<SDGNode> auxSinkSet = new LinkedList<SDGNode>();

            // get set of node Pairs
            Criterion next = worklist.poll();

            for (SDGNode s : next.getSource()) {
                for (SDGNode t : next.getTarget()) {
                    SDGNodeTuple tmp = new SDGNodeTuple(s, t);

                    if (visitedTuples.add(tmp)) {
                        auxSourceSet.add(s);
                        auxSinkSet.add(t);
                    }
                }
            }

            if (auxSourceSet.isEmpty() || auxSinkSet.isEmpty()) continue;

            // do a new chop between the criteria sets just created and
            // extend chop with new chop and worklist with new node pairs
            Collection<SDGNode> newChop = intraChopper.chop(auxSourceSet, auxSinkSet);
            chop.addAll(newChop);
            worklist.addAll(getSummarySites(chop));
        }
    }
}
