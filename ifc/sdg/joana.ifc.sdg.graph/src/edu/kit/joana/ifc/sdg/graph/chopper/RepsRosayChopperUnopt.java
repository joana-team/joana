/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;


/**
 * RepsRosayChopperUnopt is a context-sensitive unbound chopper for sequential programs.
 *
 * The implementation omits Reps' and Rosay's optimization of the same-level chopping process. It therefore
 * has an asymptotic running time of O(|nodes|^4). Its purpose is solely to demonstrate the necessity of that optimization.
 * Do not use RepsRosayChopperUnopt in an application; use {@link NonSameLevelChopper}, {@link RepsRosayChopper}
 * or {@link FixedPointChopper} instead.
 *
 * @author  Dennis Giffhorn
 */
public class RepsRosayChopperUnopt extends Chopper {
	/** A truncated unbound chopper */
	private TruncatedNonSameLevelChopper truncated;
	/** An intra-procedural chopper. */
    private IntraproceduralChopper intraChopper;

    /**
     * Instantiates a RepsRosayChopperUnopt with a SDG.
     *
     * @param g   A SDG. Can be null. Must not be a cSDG.
     */
    public RepsRosayChopperUnopt(SDG g) {
        super(g);
    }

    /**
     * Re-initializes the two choppers.
     * Triggered by {@link Chopper#setGraph(SDG)}.
     */
    protected void onSetGraph() {
        if (truncated == null) {
            truncated = new TruncatedNonSameLevelChopper(sdg);

        } else {
            truncated.setGraph(sdg);
        }

        if (intraChopper == null) {
            intraChopper = new IntraproceduralChopper(sdg);

        } else {
            intraChopper.setGraph(sdg);
        }
    }

    /**
     * Computes a context-sensitive unbound chop from <code>sourceSet</code> to <code>sinkSet</code>.
     *
     * @param sourceSet  The source criterion set. Should not contain null, should not be empty.
     * @param sinkSet    The target criterion set. Should not contain null, should not be empty.
     * @return           The chop (a HashSet).
     */
    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet) {
        Collection<SDGNode> chop = truncated.chop(sourceSet, sinkSet);
        sameLevelChopsAux(chop);
        return chop;
    }

    /**
     * Collects the nodes lying on non-truncated paths from the source criterion to the target criterion.
     * The nodes are added to the given chop.
     *
     * @param chop  The truncated unbound chop.
     */
    private void sameLevelChopsAux(Collection<SDGNode> chop) {
        LinkedList<SDGNodeTuple> worklist = new LinkedList<SDGNodeTuple>();
        worklist.addAll(getSummaryEdgePairs(chop));
        Set<SDGNodeTuple> visitedTuples = new HashSet<SDGNodeTuple>();

        // gradually add chops for each summary edge
        while(!worklist.isEmpty()) {
            // get next fifo pair
            SDGNodeTuple next = worklist.poll();

            if (visitedTuples.add(next)) {
            	// the same-level chops are computed for each found summary edge, which causes the bad runtime behavior
                Collection<SDGNode> newChop = intraChopper.chop(next.getFirstNode(), next.getSecondNode());
                chop.addAll(newChop);
                worklist.addAll(getSummaryEdgePairs(newChop));
            }
        }
    }
}
