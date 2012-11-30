/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerForward;


/**
 * TruncatedNonSameLevelChopper computes a context-sensitive truncated unbound chops in sequential programs.
 *
 * Implements the algorithm described in Reps' and Rosay's paper.
 * Essential for the computation of context-sensitive unbound chops.
 *
 * @author  Dennis Giffhorn
 */
public class TruncatedNonSameLevelChopper extends Chopper {
    /** A two-phase forward slicer */
    private SummarySlicer forward;
    /** A two-phase backward slicer */
    private SummarySlicer backward;

    /**
     * Instantiates a TruncatedNonSameLevelChopper with a SDG.
     *
     * @param g   A SDG. Can be null. Must not be a cSDG.
     */
    public TruncatedNonSameLevelChopper(SDG g) {
        super(g);
    }

    /**
     * Re-initializes the chopper and the two slicers.
     * Triggered by {@link Chopper#setGraph(SDG)}.
     */
    protected void onSetGraph() {
    	if (forward == null) {
    		forward = new SummarySlicerForward(sdg);

    	} else {
    		forward.setGraph(sdg);
    	}

    	if (backward == null) {
    		backward = new SummarySlicerBackward(sdg);

    	} else {
    		backward.setGraph(sdg);
    	}
    }

    /**
     * Computes a context-sensitive truncated unbound chop from <code>sourceSet</code> to <code>sinkSet</code>.
     *
     * @param sourceSet  The source criterion set. Should not contain null, should not be empty.
     * @param sinkSet    The target criterion set. Should not contain null, should not be empty.
     * @return           The chop (a HashSet).
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
     * @return              The angular point of the chop (can be empty).
     */
    private Collection<SDGNode> w(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet) {
        // we need two-phase slicers that only compute phase 1
        Set<SDGEdge.Kind> omitForward = EnumSet.of(
        		SDGEdge.Kind.INTERFERENCE,
                SDGEdge.Kind.INTERFERENCE_WRITE,
                SDGEdge.Kind.FORK,
                SDGEdge.Kind.FORK_IN,
                SDGEdge.Kind.PARAMETER_IN,
                SDGEdge.Kind.CALL);

        Set<SDGEdge.Kind> omitBackward = EnumSet.of(
        		SDGEdge.Kind.INTERFERENCE,
                SDGEdge.Kind.INTERFERENCE_WRITE,
                SDGEdge.Kind.FORK,
                SDGEdge.Kind.FORK_IN,
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
     * Computes the left-side truncated chop.
     *
     * @param sourceSet     The source of the chop.
     * @param w             The angular point of the chop.
     * @return              The left-side truncated chop.
     */
    private Collection<SDGNode> leftIntersection(Collection<SDGNode> sourceSet, Collection<SDGNode> w) {
        // we need a forward slicer that only computes phase 1,
        // and a backward slicer that only computes phase 2
        Set<SDGEdge.Kind> omitForward = EnumSet.of(
        		SDGEdge.Kind.INTERFERENCE,
                SDGEdge.Kind.INTERFERENCE_WRITE,
                SDGEdge.Kind.FORK,
                SDGEdge.Kind.FORK_IN,
                SDGEdge.Kind.PARAMETER_IN,
                SDGEdge.Kind.CALL);

        Set<SDGEdge.Kind> omitBackward = EnumSet.of(
        		SDGEdge.Kind.INTERFERENCE,
                SDGEdge.Kind.INTERFERENCE_WRITE,
                SDGEdge.Kind.FORK,
                SDGEdge.Kind.FORK_IN,
                SDGEdge.Kind.PARAMETER_IN,
                SDGEdge.Kind.CALL);

        // run the slicers and intersect their results
        Collection<SDGNode> result = branchChop(sourceSet, w, omitForward, omitBackward);

        return result;
    }

    /**
     * Computes the right-side truncated chop.
     *
     * @param sinkSet       The sink of the chop.
     * @param w             The angular point of the chop.
     * @return              The right-side truncated chop.
     */
    private Collection<SDGNode> rightIntersection(Collection<SDGNode> w, Collection<SDGNode> sinkSet) {
        // we need a forward slicer that only computes phase 2,
        // and a backward slicer that only computes phase 1
        Set<SDGEdge.Kind> omitForward = EnumSet.of(
        		SDGEdge.Kind.INTERFERENCE,
                SDGEdge.Kind.INTERFERENCE_WRITE,
                SDGEdge.Kind.FORK,
                SDGEdge.Kind.FORK_IN,
                SDGEdge.Kind.PARAMETER_OUT);

        Set<SDGEdge.Kind> omitBackward = EnumSet.of(
        		SDGEdge.Kind.INTERFERENCE,
                SDGEdge.Kind.INTERFERENCE_WRITE,
                SDGEdge.Kind.FORK,
                SDGEdge.Kind.FORK_IN,
                SDGEdge.Kind.PARAMETER_OUT);

        // run the slicers and intersect their results
        Collection<SDGNode> result = branchChop(w, sinkSet, omitForward, omitBackward);

        return result;
    }

    /**
     * Computes a chop from <code>sourceSet</code> to <code>sinkSet</code> by intersecting the forward slice and the backward slice.
     * The procedure configures the slicers exclude the given kinds of edges.
     *
     * @param sourceSet      The source of the chop.
     * @param sinkSet        The sink of the chop.
     * @param omitForward    Kinds of edges to be omitted by the forward slicer.
     * @param omitBackward   Kinds of edges to be omitted by the backward slicer.
     * @return               The chop.
     */
    private Collection<SDGNode> branchChop (Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet,
    		Set<SDGEdge.Kind> omitForward, Set<SDGEdge.Kind> omitBackward) {

        forward.setOmittedEdges(omitForward);
        backward.setOmittedEdges(omitBackward);

        Collection<SDGNode> forwardSlice = forward.slice(sourceSet);
        return backward.subgraphSlice(sinkSet, forwardSlice);
    }
}
