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
 * The IntersectionChopper (IC) is a context-insensitive unbound chopper for sequential programs.
 *
 * Similar to the {@link InsensitiveIntersectionChopper} it uses two-phase backward- and forward slicing,
 * but computes the forward slice on the backward slice. Therefore it is much faster and more precise than
 * the {@link InsensitiveIntersectionChopper}.
 *
 * Alternatives are {@link NonSameLevelChopper}, {@link RepsRosayChopper}, {@link DoubleIntersectionChopper},
 * {@link Opt1Chopper} or {@link FixedPointChopper}, which are all more precise, but also slower.
 *
 * @author Dennis Giffhorn
 */
public class IntersectionChopper extends Chopper {
	/** A two-phase forward slicer. */
    private SummarySlicer forward;
	/** A two-phase backward slicer. */
    private SummarySlicer backward;

    /**
     * Instantiates an IntersectionChopper with a SDG.
     *
     * @param g   A SDG. Can be null. Must not be a cSDG.
     */
    public IntersectionChopper(SDG g) {
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
        // compute a backward slice for target
        Collection<SDGNode> backSlice = backward.slice(target);
        // compute a forward slice for source, restricted to the nodes in backSlice
        Collection<SDGNode> chop = forward.subgraphSlice(source, backSlice);
        return chop;
    }
}


