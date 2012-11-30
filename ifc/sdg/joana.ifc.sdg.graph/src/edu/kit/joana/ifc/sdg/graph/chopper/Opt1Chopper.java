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
 * The Opt1Chopper is an almost context-sensitive unbound chopper for sequential programs.
 *
 * It improves on the {@link DoubleIntersectionChopper} by computing a two-phase backward slice, a two-phase forward slice restricted
 * to the nodes in the backward slice, another two-phase backward slice restricted to the nodes in that forward slice, and then
 * another two-phase forward slice restricted to the nodes in the last backward slice.
 *
 * The additional forward slice improves precision, which is almost context-sensitive, but also reduces computation speed.
 *
 * Alternatives are {@link NonSameLevelChopper}, {@link RepsRosayChopper} or {@link FixedPointChopper},
 * which are more precise, but also slower, and {@link IntersectionChopper} or {@link DoubleIntersectionChopper},
 * which are less precise, but faster.
 *
 * @author  Dennis Giffhorn
 */
public class Opt1Chopper extends Chopper {
	/** A two-phase forward slicer. */
    private SummarySlicer forward;
	/** A two-phase backward slicer. */
    private SummarySlicer backward;

    /**
     * Instantiates an Opt1Chopper with a SDG.
     *
     * @param g   A SDG. Can be null. Must not be a cSDG.
     */
    public Opt1Chopper(SDG g) {
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

        // compute a forward slice for source, which is restricted to the nodes in backSlice
        Collection<SDGNode> chop = forward.subgraphSlice(source, backSlice);

        // compute another backward slice for target, which is restricted to the nodes in chop
        backSlice = backward.subgraphSlice(target, chop);

        // compute a last forward slice for source, which is restricted to the nodes in backSlice
        chop = forward.subgraphSlice(source, backSlice);

        return chop;
    }
}


