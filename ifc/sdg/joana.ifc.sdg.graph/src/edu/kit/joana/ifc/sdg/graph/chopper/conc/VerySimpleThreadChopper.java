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
 * The VerySimpleThreadChopper is a context- and time-insensitive chopper for concurrent programs.
 *
 * It intersects a forward slice and a backward slice computed by the iterated 2-phase slicer.
 * Due to the naive intersection the algorithm is not only very imprecise but also comparatively slow.
 * It should not be used in an application.
 *
 * If used for a sequential program the algorithm falls back to the
 * {@link edu.kit.joana.ifc.sdg.graph.chopper.InsensitiveIntersectionChopper [InsensitiveIntersectionChopper]}.
 *
 * @author  Dennis Giffhorn
 */
public class VerySimpleThreadChopper extends Chopper {
	/** A backward slicer. */
    private I2PBackward back;
	/** A forward slicer. */
    private I2PForward forw;

    /**
     * Instantiates a VerySimpleThreadChopper with a SDG.
     *
     * @param g   A SDG or a cSDG. Can be null.
     */
    public VerySimpleThreadChopper(SDG g) {
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
        Collection<SDGNode> backSlice = back.slice(sinkSet);
        Collection<SDGNode> forwSlice = forw.slice(sourceSet);

        backSlice.retainAll(forwSlice);

        return backSlice;
    }
}
