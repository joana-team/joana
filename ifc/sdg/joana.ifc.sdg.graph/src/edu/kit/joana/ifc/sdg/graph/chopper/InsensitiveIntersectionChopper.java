/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerForward;


/**
 * The InsensitiveIntersectionChopper (IIC) is a context-insensitive unbound chopper for sequential programs.
 *
 * It computes a two-phase backward slice for the target criterion, a two-phase forward slice
 * for the source criterion and intersects the results.
 *
 * This algorithm is not much precise and not much fast and should not be used by an application.
 * Use {@link NonSameLevelChopper}, {@link RepsRosayChopper}, {@link IntersectionChopper},
 * {@link DoubleIntersectionChopper}, {@link Opt1Chopper} or {@link FixedPointChopper} instead.
 *
 * @author  Dennis Giffhorn
 */
public class InsensitiveIntersectionChopper extends Chopper {
	/** A two-phase forward slicer. */
    private SummarySlicer forward;
	/** A two-phase backward slicer. */
    private SummarySlicer backward;

    /**
     * Instantiates an InsensitiveIntersectionChopper with a SDG.
     *
     * @param g   A SDG. Can be null. Must not be a cSDG.
     */
    public InsensitiveIntersectionChopper(SDG g) {
        super(g);
    }

    /**
     * Re-initializes the two slicers.
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
     * Computes a context-insensitive unbound chop from <code>sourceSet</code> to <code>sinkSet</code>.
     *
     * @param sourceSet  The source criterion set. Should not contain null, should not be empty.
     * @param sinkSet    The target criterion set. Should not contain null, should not be empty.
     * @return           The chop (a HashSet).
     */
    public Collection<SDGNode> chop(Collection<SDGNode> source, Collection<SDGNode> target) {
        // forward and backward slice
        Collection<SDGNode> forwardSlice = forward.slice(source);
        Collection<SDGNode> backwardSlice = backward.slice(target);

        // intersect
        forwardSlice.retainAll(backwardSlice);

        return forwardSlice;
    }
}


