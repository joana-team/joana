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
import edu.kit.joana.ifc.sdg.graph.chopper.Criterion;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.BarrierManager;



/** An implementation of the Reps-Rosay chopper for interprocedural barrier chops.
 *
 * @author  Dennis Giffhorn
 */
public class NonSameLevelBarrierChopper extends BarrierChopper {
    private BarrierManager barrier;

    // The employed barrier choppers, which share the above BarrierManager
    private TruncatedNonSameLevelBarrierChopper truncated;
    private SummaryMergedBarrierChopper smc;

    /**
     * Creates a new instance of NonSameLevelChopper
     *
     * @param g     A SDG
     */
    public NonSameLevelBarrierChopper(SDG g) {
        super(g);
    }

    protected void onSetGraph() {
    	if (barrier == null) {
        	barrier = new BarrierManager();
    	}

        if (truncated == null) {
            truncated = new TruncatedNonSameLevelBarrierChopper(sdg);
            truncated.setBarrier(barrier);
        } else {
            truncated.setGraph(sdg);
        }

        if (smc == null) {
            smc = new SummaryMergedBarrierChopper(sdg);
            smc.setBarrier(barrier);
        } else {
            smc.setGraph(sdg);
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
    	truncated.setBarrier(barrier);
        smc.setBarrier(barrier);
	}

    /**
     * Computes the context-sensitive chop from the nodes in sourceSet to the nodes in sinkSet.
     *
     * @param sourceSet     A set of nodes in the SDG.
     * @param sinkSet       A set of node in the SDG.
     * @throws              Nothing - ChoppingException never occurs.
     */
    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet) {
        Collection<SDGNode> chop = truncated.chop(sourceSet, sinkSet);
        sameLevelChopsAux(chop);
        return chop;
    }

    /**
     * Collects the non-truncated nodes that belong to the chop.
     * The nodes are added to the given chop.
     *
     * @param chop  The truncated chop.
     */
    private void sameLevelChopsAux(Collection<SDGNode> chop) {
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
            Collection<SDGNode> newChop = smc.chop(auxSourceSet, auxSinkSet);
            chop.addAll(newChop);
        }
    }
}
