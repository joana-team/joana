/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper.barrier;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.InvalidCriterionException;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.BarrierManager;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.IntraproceduralBarrierSlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.IntraproceduralBarrierSlicerForward;


/** This class implements a truncated same-level barrier chop.
 * Or more intuitive, it computes intra-procedural chops and can
 * additionally be given a barrier of nodes, which it will not trespass.
 *
 * @author Bernd Nuernberger, Kai Brueckner, Dennis Giffhorn
 *
 */
public class IntraproceduralBarrierChopper extends BarrierChopper {
    private BarrierManager barrier;

    // two slicers who share the above BarrierManager
    private IntraproceduralBarrierSlicerForward forward;
    private IntraproceduralBarrierSlicerBackward backward;

    /**
     * Constructs a chopper for a given SDG.
     * @param g The SDG to operate on.
     */
    public IntraproceduralBarrierChopper(SDG g) {
        super(g);
    }

    protected void onSetGraph() {
    	if (barrier == null) {
        	barrier = new BarrierManager();
    	}

    	if (forward == null) {
    		forward = new IntraproceduralBarrierSlicerForward(sdg);
    		forward.setBarrier(barrier);
    	} else {
    		forward.setGraph(sdg);
    	}

    	if (backward == null) {
    		backward = new IntraproceduralBarrierSlicerBackward(sdg);
    		backward.setBarrier(barrier);
    	} else {
    		backward.setGraph(sdg);
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
		forward.setBarrier(barrier);
		backward.setBarrier(barrier);
	}

    /**
     * Computes an intra-procedural chop between the nodes of <code>source</code> and <code>sink</code>.
     * An optionally passed barrier is respected by this chopper.
     *
     * @param sourceSet  The source criteria set.
     * @param sinkSet    The target criteria set.
     * @throws InvalidCriterionException If source or sink set is empty or
     * nodes of source and sink are not all of the same procedure.
     *
     */
    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet) {
    	// throws an InvalidCriterionException if the chopping criterion is not same-level
        Chopper.testSameLevelSetCriteria(sourceSet, sinkSet);

        Collection<SDGNode> back = backward.slice(sinkSet);
        Collection<SDGNode> forw = forward.slice(sourceSet);
        back.retainAll(forw);

        return back;
    }
}
