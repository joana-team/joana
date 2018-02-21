/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.slicer.IntraproceduralSlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.IntraproceduralSlicerForward;



/**
 * RepsRosayChopper is a context-sensitive unbound chopper for sequential programs.
 *
 * Implements Reps' and Rosay's chopping algorithm, including all optimizations.
 * It is asymptotically the fastest of all context-sensitive unbound choppers. However, for small-
 * and middle-sized programs {@link NonSameLevelChopper} often proved to be much faster.
 *
 * @author  Dennis Giffhorn
 */
public class RepsRosayChopper extends Chopper {
	/** A truncated unbound chopper */
    private TruncatedNonSameLevelChopper truncated;
    /** An intra-procedural forward slicer */
    private IntraproceduralSlicerForward fSlicer;
    /** An intra-procedural backward slicer */
    private IntraproceduralSlicerBackward bSlicer;

    /**
     * Instantiates a RepsRosayChopper with a SDG.
     *
     * @param g   A SDG. Can be null. Must not be a cSDG.
     */
    public RepsRosayChopper(SDG g) {
    	super(g);
    }

    /**
     * Re-initializes the chopper and the two slicers.
     * Triggered by {@link Chopper#setGraph(SDG)}.
     */
    protected void onSetGraph() {
        if (truncated == null) {
            truncated = new TruncatedNonSameLevelChopper(sdg);

        } else {
            truncated.setGraph(sdg);
        }

        if (fSlicer == null) {
            fSlicer = new IntraproceduralSlicerForward(sdg);

        } else {
            fSlicer.setGraph(sdg);
        }

        if (bSlicer == null) {
            bSlicer = new IntraproceduralSlicerBackward(sdg);

        } else {
            bSlicer.setGraph(sdg);
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
     * The nodes are added to the given chop. Uses Reps' and Rosay's optimized same-level chopper.
     *
     * @param chop  The truncated unbound chop.
     */
    private void sameLevelChopsAux(Collection<SDGNode> chop) {
    	/* see Reps' and Rosay's paper for a description of this routine */
        LinkedList<SDGNodeTuple> worklist = new LinkedList<SDGNodeTuple>();
        worklist.addAll(getSummaryEdgePairs(chop));

        Set<SDGNodeTuple> visitedTuples = new HashSet<SDGNodeTuple>();
        HashMap<SDGNode, Collection<SDGNode>> bCandidates = new HashMap<SDGNode, Collection<SDGNode>>();
        HashMap<SDGNode, Collection<SDGNode>> fCandidates = new HashMap<SDGNode, Collection<SDGNode>>();
        HashMap<SDGNode, Collection<SDGNode>> bActOut = new HashMap<SDGNode, Collection<SDGNode>>();

        // === gradually add chops for each summary edge ===

        while(!worklist.isEmpty()) {
            // get next fifo pair
            SDGNodeTuple next = worklist.poll();

            if (visitedTuples.add(next)) {
            	SDGNode fi = next.getFirstNode();
            	SDGNode fo = next.getSecondNode();

            	// b-candidates
            	if (bCandidates.get(fo) == null) {
            		Collection<SDGNode> slice = bSlicer.slice(fo);
            		bCandidates.put(fo, slice);

            		HashSet<SDGNode> actOuts = new HashSet<SDGNode>();
                    for (SDGNode n : slice) {
                    	if (n.getKind() == SDGNode.Kind.ACTUAL_OUT) {
                    		actOuts.add(n);
                    	}
                    }
            		bActOut.put(fo, actOuts);
            	}

            	// f-candidates
            	if (fCandidates.get(fi) == null) {
            		Collection<SDGNode> slice = fSlicer.slice(fi);
            		fCandidates.put(fi, slice);
            	}


            	LinkedList<SDGNode> remove = new LinkedList<SDGNode>();
            	for (SDGNode x : bCandidates.get(fo)) {
            		if (fCandidates.get(fi).contains(x)) {
            			chop.add(x);
            			remove.add(x);

            			if (x.getKind() == SDGNode.Kind.ACTUAL_IN) {
            				for (SDGEdge e : sdg.getOutgoingEdgesOfKindUnsafe(x, SDGEdge.Kind.SUMMARY)) {
            					if (bActOut.get(fo).contains(e.getTarget())) {
            						for (SDGNodeTuple fifo : sdg.getAllFormalPairs(x, e.getTarget())) {
	            						if (fifo != null) {
	            							worklist.add(fifo);
	            						}
            						}
            					}
            				}

            			} else if (x.getKind() == SDGNode.Kind.CALL) {
            				// treat auxiliary summary edges between call and actual-out nodes
            				for (SDGNode n : sdg.getParametersFor(x)) {
            					if (n.getKind() != SDGNode.Kind.ACTUAL_OUT) continue;

            					if (bActOut.get(fo).contains(n)) {
            						for (SDGNodeTuple fifo : sdg.getAllFormalPairs(x, n)) {
	            						if (fifo != null) {
	            							worklist.add(fifo);
	            						}
            						}
            					}
            				}
            			}
            		}
            	}

    			bCandidates.get(fo).removeAll(remove);
            }
        }
    }
}
