/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper.conc;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PForward;


/**
 * The FixedPointChopper is a context- and time-insensitive chopper for concurrent programs.
 *
 * It repeatedly computes two-phase backward- and forward slices, restricted to the nodes in the previous slice,
 * until reaching a fixed point. Its precision is next to context-sensitivity and it is reasonably fast
 * (it needs 2-4 iterations in practice).
 *
 * If used for a sequential program the algorithm falls back to the
 * {@link edu.kit.joana.ifc.sdg.graph.chopper.FixedPointChopper [FixedPointChopper]} for sequential programs.
 *
 * @author  Dennis Giffhorn
 */
public class FixedPointChopper extends Chopper {
	/** A backward slicer. */
    private I2PBackward back;
	/** A forward slicer. */
    private I2PForward forw;

    /**
     * Instantiates a FixedPointChopper with a SDG.
     *
     * @param g   A SDG or a cSDG. Can be null.
     */
    public FixedPointChopper(SDG g) {
    	super(g);
    }

    /**
     * Re-initializes the two slicers.
     * Triggered by {@link Chopper#setGraph(SDG)}.
     */
    protected void onSetGraph() {
    	if (back == null) {
    		back = new I2PBackward(sdg);
    	} else {
    		back.setGraph(sdg);
    	}

        if (forw == null) {
        	forw = new I2PForward(sdg);
        } else {
        	forw.setGraph(sdg);
        }
    }

    /**
     * Computes a context- and time-insensitive chop from <code>sourceSet</code> to <code>sinkSet</code>.
     *
     * @param sourceSet  The source criterion set. Should not contain null, should not be empty.
     * @param sinkSet    The target criterion set. Should not contain null, should not be empty.
     * @return           The chop (a HashSet).
     */
    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet) {
    	// compute an initial chop
    	Collection<SDGNode> backSlice = back.slice(sinkSet);
    	Collection<SDGNode> chop = forw.subgraphSlice(sourceSet, backSlice);

    	// search for a fixed point
    	boolean changed = true;

    	while (changed) {
    		backSlice = back.subgraphSlice(sinkSet, chop);
    		Collection<SDGNode> chopchop = forw.subgraphSlice(sourceSet, backSlice);

    		if (chopchop.size() >= chop.size()) {
    			changed = false;
    		}

    		chop = chopchop;
    	}

    	return chop;
    }
}
