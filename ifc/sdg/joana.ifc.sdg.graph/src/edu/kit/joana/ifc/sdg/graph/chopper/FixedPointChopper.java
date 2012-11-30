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
 * The FixedPointChopper is an almost context-sensitive unbound chopper for sequential programs.
 *
 * It repeatedly computes two-phase backward- and forward slices, restricted to the nodes in the previous slice,
 * until reaching a fixed point. Its precision is next to context-sensitivity and it is reasonably fast
 * (it needs 2-4 iterations in practice).
 *
 * Alternatives are the context-sensitive choppers {@link NonSameLevelChopper} or {@link RepsRosayChopper},
 * which are more precise, but also slower, and the context-insensitive choppers {@link IntersectionChopper},
 * {@link DoubleIntersectionChopper} or {@link Opt1Chopper}, which are less precise, but faster.
 *
 * @author  Dennis Giffhorn
 */
public class FixedPointChopper extends Chopper {
	/** A two-phase forward slicer. */
    private SummarySlicer forward;
	/** A two-phase backward slicer. */
    private SummarySlicer backward;

    /**
     * Instantiates a FixedPointChopper with a SDG.
     *
     * @param g   A SDG. Can be null. Must not be a cSDG.
     */
    public FixedPointChopper(SDG g) {
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
    	// compute an initial chop
        Collection<SDGNode> backSlice = backward.slice(target);
        Collection<SDGNode> chop = forward.subgraphSlice(source, backSlice);

        // search for a fixed point
        boolean changed = true;

        while (changed) {
        	backSlice = backward.subgraphSlice(target, chop);
        	Collection<SDGNode> chopchop = forward.subgraphSlice(source, backSlice);

        	if (chopchop.size() >= chop.size()) {
        		changed = false;
        	}

    		chop = chopchop;
        }

        return chop;
    }
}


