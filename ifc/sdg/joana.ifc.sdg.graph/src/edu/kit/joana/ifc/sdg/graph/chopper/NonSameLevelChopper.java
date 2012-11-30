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
 * NonSameLevelChopper is a context-sensitive unbound chopper for sequential programs.
 *
 * It is a Reps-Rosay-style chopping algorithm using an alternative technique for the computation of the
 * same-level chops. Its asymptotic running time O(|node|^4) is worse than that of the original algorithm, being O(|nodes|^3).
 * However, at least for small- and middle-sized programs it shows a similar runtime behavior and is often much faster.
 *
 * @author  Dennis Giffhorn
 */
public class NonSameLevelChopper extends Chopper {
	/** A truncated unbound chopper */
	private TruncatedNonSameLevelChopper truncated;
	/** A SMC for the necessary same-level chops. */
	private SummaryMergedChopper smc;

	/**
     * Instantiates a NonSameLevelChopper with a SDG.
     *
     * @param g   A SDG. Can be null. Must not be a cSDG.
     */
    public NonSameLevelChopper(SDG g) {
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

        if (smc == null) {
            smc = new SummaryMergedChopper(sdg);

        } else {
            smc.setGraph(sdg);
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
     * The nodes are added to the given chop. Uses the {@link SummaryMergedChopper} to compute the same-level chops.
     *
     * @param chop  The truncated unbound chop.
     */
    private void sameLevelChopsAux(Collection<SDGNode> chop) {
        LinkedList<Criterion> worklist = new LinkedList<Criterion>();
        Set<SDGNodeTuple> visitedTuples = new HashSet<SDGNodeTuple>();
        worklist.addAll(getSummarySites(chop));

        // === gradually add chops for each summary edge ===
//        System.out.println("****************");
        while(!worklist.isEmpty()) {
            // sets of chop criteria for the intraprocedural chop to be done
        	HashSet<SDGNode> auxSourceSet = new HashSet<SDGNode>();
        	HashSet<SDGNode> auxSinkSet = new HashSet<SDGNode>();

            // get set of node Pairs
            Criterion next = worklist.poll();
//            System.out.println(next);

            for (SDGNode s : next.source) {
                for (SDGNode t : next.target) {
                    SDGNodeTuple tmp = new SDGNodeTuple(s, t);

                    if (visitedTuples.add(tmp)) {
                        auxSourceSet.add(s);
                        auxSinkSet.add(t);
                    }
                }
            }

            if (auxSourceSet.isEmpty() || auxSinkSet.isEmpty()) continue;

            // do a new chop between the criteria sets just created and
            // extend chop with new chop and worklist with new node pairs
            Collection<SDGNode> newChop = smc.chop(auxSourceSet, auxSinkSet);
            chop.addAll(newChop);
        }
    }
}
