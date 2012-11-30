/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper.barrier;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.BarrierManager;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.CSBarrierSlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.CSBarrierSlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.CSBarrierSlicerForward;


/** An variant of Reps' and Rosay's truncated chopper for interprocedural programs wich accepts a barrier.
 * USAGE:
 * - Create a new instance
 * - Call setBarrier(Collection<SDGNode> barrier) and pass the barrier
 * - Call one of the chop methods
 *
 * -- Created on September 6, 2005
 *
 * @author  Dennis Giffhorn
 */
public class TruncatedNonSameLevelBarrierChopper extends BarrierChopper {
    private BarrierManager barrier;

    // The employed context-sensitive barrier slicers, which share the above BarrierManager
    private CSBarrierSlicer forward;
    private CSBarrierSlicer backward;

    /**
     * Creates a new instance
     *
     * @param g     A SDG.
     */
    public TruncatedNonSameLevelBarrierChopper(SDG g) {
        super(g);
    }

    protected void onSetGraph() {
    	if (barrier == null) {
        	barrier = new BarrierManager();
    	}

    	if (forward == null) {
    		forward = new CSBarrierSlicerForward(sdg);
    		forward.setBarrier(barrier);
    	} else {
    		forward.setGraph(sdg);
    	}

    	if (backward == null) {
    		backward = new CSBarrierSlicerBackward(sdg);
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
     * Compute a chop for a set of sources and a set of sinks.
     *
     * @param sourceSet    The source of the chop.
     * @param sinkSet      The sink of the chop.
     */
    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet) {
        Collection<SDGNode> chop = new HashSet<SDGNode>();

        // compute the angular point of the chop
        Collection<SDGNode> w = w(sourceSet, sinkSet);

        // compute the left-side truncated chop and the right-side truncated chop
        Collection<SDGNode> opLeft = leftIntersection(sourceSet, w);
        Collection<SDGNode> opRight = rightIntersection(w, sinkSet);

        // unification of opLeft and opRight
        chop.addAll(opLeft);
        chop.addAll(opRight);

        return chop;
    }

    /**
     * Computes the angular point of the chop.
     *
     * @param sourceSet     The source of the chop.
     * @param sinkSet       The sink of the chop.
     * @return              The angular point of the chop; a set of nodes (can be empty).
     */
    private Collection<SDGNode> w(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet) {
        // we need two-phase slicers that only compute phase 1
        Set<SDGEdge.Kind> omitForward = EnumSet.of(SDGEdge.Kind.INTERFERENCE,
                SDGEdge.Kind.INTERFERENCE_WRITE,
                SDGEdge.Kind.FORK, SDGEdge.Kind.FORK_IN,
                SDGEdge.Kind.PARAMETER_IN, SDGEdge.Kind.CALL);

        Set<SDGEdge.Kind> omitBackward = EnumSet.of(SDGEdge.Kind.INTERFERENCE,
                SDGEdge.Kind.INTERFERENCE_WRITE,
                SDGEdge.Kind.FORK, SDGEdge.Kind.FORK_IN,
                SDGEdge.Kind.PARAMETER_OUT);

        // now let's run those slicers and intersect their result
        forward.setOmittedEdges(omitForward);
        Collection<SDGNode> forwardSlice = forward.slice(sourceSet);

        backward.setOmittedEdges(omitBackward);
        Collection<SDGNode> backwardSlice = backward.slice(sinkSet);

        forwardSlice.retainAll(backwardSlice);

        return forwardSlice;
    }

    /**
     * Compute the left-side truncated chop.
     *
     * @param sourceSet     The source of the chop.
     * @param w             The angular point of the chop.
     * @return              The left-side truncated chop.
     */
    private Collection<SDGNode> leftIntersection(Collection<SDGNode> sourceSet, Collection<SDGNode> w) {
        // we need a forward slicer that only computes phase 1,
        // and a backward slicer that only computes phase 2
        Set<SDGEdge.Kind> omitForward = EnumSet.of(SDGEdge.Kind.INTERFERENCE,
                SDGEdge.Kind.INTERFERENCE_WRITE,
                SDGEdge.Kind.FORK, SDGEdge.Kind.FORK_IN,
                SDGEdge.Kind.PARAMETER_IN, SDGEdge.Kind.CALL);

        Set<SDGEdge.Kind> omitBackward = EnumSet.of(SDGEdge.Kind.INTERFERENCE,
                SDGEdge.Kind.INTERFERENCE_WRITE,
                SDGEdge.Kind.FORK, SDGEdge.Kind.FORK_IN,
                SDGEdge.Kind.PARAMETER_IN, SDGEdge.Kind.CALL);

        // run the slicers and intersect their results
        Collection<SDGNode> result = branchChop(sourceSet, w, omitForward, omitBackward);

        return result;
    }

    /**
     * Compute the right-side truncated chop.
     *
     * @param sinkSet       The sink of the chop.
     * @param w             The angular point of the chop.
     * @return              The right-side truncated chop.
     */
    private Collection<SDGNode> rightIntersection(Collection<SDGNode> w, Collection<SDGNode> sinkSet) {
        // we need a forward slicer that only computes phase 2,
        // and a backward slicer that only computes phase 1
        Set<SDGEdge.Kind> omitForward = EnumSet.of(SDGEdge.Kind.INTERFERENCE,
                SDGEdge.Kind.INTERFERENCE_WRITE,
                SDGEdge.Kind.FORK, SDGEdge.Kind.FORK_IN,
                SDGEdge.Kind.PARAMETER_OUT);

        Set<SDGEdge.Kind> omitBackward = EnumSet.of(SDGEdge.Kind.INTERFERENCE,
                SDGEdge.Kind.INTERFERENCE_WRITE,
                SDGEdge.Kind.FORK, SDGEdge.Kind.FORK_IN,
                SDGEdge.Kind.PARAMETER_OUT);

        // run the slicers and intersect their results
        Collection<SDGNode> result = branchChop(w, sinkSet, omitForward, omitBackward);

        return result;
    }

    /**
     *
     * @param sourceSet
     * @param sinkSet
     * @param omitForward
     * @param omitBackward
     * @return
     */
    private Collection<SDGNode> branchChop (Collection<SDGNode> sourceSet,
            Collection<SDGNode> sinkSet, Set<SDGEdge.Kind> omitForward,
            Set<SDGEdge.Kind> omitBackward) {

        Collection<SDGNode> forwardSlice = null;
        forward.setOmittedEdges(omitForward);
        forwardSlice = forward.slice(sourceSet);
        backward.setOmittedEdges(omitBackward);
        return backward.subgraphSlice(sinkSet, forwardSlice);
    }
}
