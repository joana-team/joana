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
import edu.kit.joana.ifc.sdg.graph.slicer.ContextSlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.ContextSlicerForward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;


/**
 * An unbound chopper for sequential programs that works with contexts to gain a context-sensitive result.
 *
 * Determines all contexts in the backward slice of the target criterion, all contexts in the forward
 * slice of the source criterion and intersects those sets.
 *
 * This algorithm served as a comparator for the development of other context-sensitive chopping algorithms.
 * Due to its poor runtime performance it should not be used in a real application. Use {@link RepsRosayChopper}
 * or {@link NonSameLevelChopper} instead.
 *
 * @author  Dennis Giffhorn
 */
public class ContextBasedChopper extends Chopper {
	/** A backward slicer that returns contexts instead of nodes. */
	private ContextSlicerBackward back;
	/** A forward slicer that returns contexts instead of nodes. */
	private ContextSlicerForward forw;

	/**
	 * Creates a new ContextBasedChopper.
	 * @param sdg  The SDG of a sequential program. Must not be null.
	 */
    public ContextBasedChopper(SDG sdg) {
        super(sdg);
    }

    /**
     * Re-initializes the two slicers.
     * Triggered by {@link Chopper#setGraph(SDG)}.
     */
	protected void onSetGraph() {
        back = new ContextSlicerBackward(sdg, true);
        forw = new ContextSlicerForward(sdg, true);
	}

	/**
	 * Computes a context-sensitive chop by intersecting context-slices.
	 * @param sourceSet   The source of the chop.
	 * @param targetSet   The sink of the chop.
	 */
    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> targetSet) {
    	Collection<Context> slice = back.contextSliceNodes(targetSet);
    	Collection<SDGNode> chop = forw.contextSubgraphSlice(sourceSet, slice);
    	return chop;
    }
}
