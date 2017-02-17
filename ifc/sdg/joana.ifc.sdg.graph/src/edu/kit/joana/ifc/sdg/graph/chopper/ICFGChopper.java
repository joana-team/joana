/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGForward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGSlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;



/**
 * CFGChopper is a context-sensitive unbound chopper for ICFGs.
 *
 * It is a Reps-Rosay-style chopping algorithm using an alternative technique for the computation of the
 * same-level chops. Its asymptotic running time O(|node|^4) is worse than that of the original algorithm, being O(|nodes|^3).
 * However, at least for small- and middle-sized programs it shows a similar runtime behavior and is often much faster.
 *
 * @author  Dennis Giffhorn
 */
public class ICFGChopper {
	private class TruncatedCFGChopper {
	    /** A two-phase forward slicer */
	    private CFGSlicer forward;
	    /** A two-phase backward slicer */
	    private CFGSlicer backward;

	    /**
	     * Re-initializes the chopper and the two slicers.
	     * Triggered by {@link Chopper#setGraph(SDG)}.
	     */
	    private void setGraph() {
	    	if (forward == null) {
	    		forward = new CFGForward(g);

	    	} else {
	    		forward.setGraph(g);
	    	}

	    	if (backward == null) {
	    		backward = new CFGBackward(g);

	    	} else {
	    		backward.setGraph(g);
	    	}
	    }

	    /**
	     * Computes a context-sensitive truncated unbound chop from <code>sourceSet</code> to <code>sinkSet</code>.
	     *
	     * @param sourceSet  The source criterion set. Should not contain null, should not be empty.
	     * @param sinkSet    The target criterion set. Should not contain null, should not be empty.
	     * @return           The chop (a HashSet).
	     */
	    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet) {
	        Collection<SDGNode> chop = new HashSet<SDGNode>();

	        // compute the angular point of the chop
	        Collection<SDGNode> w = w(sourceSet, sinkSet);

	        // compute the left-side truncated chop and the right-side truncated chop
	        Collection<SDGNode> opLeft = leftIntersection(sourceSet, w);
	        Collection<SDGNode> opRight = rightIntersection(w, sinkSet);

	        // unification of opLeft and opRight
	        chop.addAll(opLeft);
	        chop.addAll(opRight);

	        return chop;
	    }

	    /**
	     * Computes the angular point of the chop.
	     *
	     * @param sourceSet     The source of the chop.
	     * @param sinkSet       The sink of the chop.
	     * @return              The angular point of the chop (can be empty).
	     */
	    private Collection<SDGNode> w(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet) {
	        // we need two-phase slicers that only compute phase 1
	        Set<SDGEdge.Kind> omitForward = EnumSet.of(
	        		SDGEdge.Kind.INTERFERENCE,
	                SDGEdge.Kind.INTERFERENCE_WRITE,
	                SDGEdge.Kind.FORK,
	                SDGEdge.Kind.FORK_IN,
	                SDGEdge.Kind.PARAMETER_IN,
	                SDGEdge.Kind.CALL);

	        Set<SDGEdge.Kind> omitBackward = EnumSet.of(
	        		SDGEdge.Kind.INTERFERENCE,
	                SDGEdge.Kind.INTERFERENCE_WRITE,
	                SDGEdge.Kind.FORK,
	                SDGEdge.Kind.FORK_IN,
	                SDGEdge.Kind.PARAMETER_OUT);

	        // now let's run those slicers and intersect their result
	        forward.setOmittedEdges(omitForward);
	        Collection<SDGNode> forwardSlice = forward.slice(sourceSet);

	        backward.setOmittedEdges(omitBackward);
	        Collection<SDGNode> backwardSlice = backward.slice(sinkSet);

	        forwardSlice.retainAll(backwardSlice);

	        return forwardSlice;
	    }

	    /**
	     * Computes the left-side truncated chop.
	     *
	     * @param sourceSet     The source of the chop.
	     * @param w             The angular point of the chop.
	     * @return              The left-side truncated chop.
	     */
	    private Collection<SDGNode> leftIntersection(Collection<SDGNode> sourceSet, Collection<SDGNode> w) {
	        // we need a forward slicer that only computes phase 1,
	        // and a backward slicer that only computes phase 2
	        Set<SDGEdge.Kind> omitForward = EnumSet.of(
	        		SDGEdge.Kind.INTERFERENCE,
	                SDGEdge.Kind.INTERFERENCE_WRITE,
	                SDGEdge.Kind.FORK,
	                SDGEdge.Kind.FORK_IN,
	                SDGEdge.Kind.PARAMETER_IN,
	                SDGEdge.Kind.CALL);

	        Set<SDGEdge.Kind> omitBackward = EnumSet.of(
	        		SDGEdge.Kind.INTERFERENCE,
	                SDGEdge.Kind.INTERFERENCE_WRITE,
	                SDGEdge.Kind.FORK,
	                SDGEdge.Kind.FORK_IN,
	                SDGEdge.Kind.PARAMETER_IN,
	                SDGEdge.Kind.CALL);

	        // run the slicers and intersect their results
	        Collection<SDGNode> result = branchChop(sourceSet, w, omitForward, omitBackward);

	        return result;
	    }

	    /**
	     * Computes the right-side truncated chop.
	     *
	     * @param sinkSet       The sink of the chop.
	     * @param w             The angular point of the chop.
	     * @return              The right-side truncated chop.
	     */
	    private Collection<SDGNode> rightIntersection(Collection<SDGNode> w, Collection<SDGNode> sinkSet) {
	        // we need a forward slicer that only computes phase 2,
	        // and a backward slicer that only computes phase 1
	        Set<SDGEdge.Kind> omitForward = EnumSet.of(
	        		SDGEdge.Kind.INTERFERENCE,
	                SDGEdge.Kind.INTERFERENCE_WRITE,
	                SDGEdge.Kind.FORK,
	                SDGEdge.Kind.FORK_IN,
	                SDGEdge.Kind.PARAMETER_OUT);

	        Set<SDGEdge.Kind> omitBackward = EnumSet.of(
	        		SDGEdge.Kind.INTERFERENCE,
	                SDGEdge.Kind.INTERFERENCE_WRITE,
	                SDGEdge.Kind.FORK,
	                SDGEdge.Kind.FORK_IN,
	                SDGEdge.Kind.PARAMETER_OUT);

	        // run the slicers and intersect their results
	        Collection<SDGNode> result = branchChop(w, sinkSet, omitForward, omitBackward);

	        return result;
	    }

	    /**
	     * Computes a chop from <code>sourceSet</code> to <code>sinkSet</code> by intersecting the forward slice and the backward slice.
	     * The procedure configures the slicers exclude the given kinds of edges.
	     *
	     * @param sourceSet      The source of the chop.
	     * @param sinkSet        The sink of the chop.
	     * @param omitForward    Kinds of edges to be omitted by the forward slicer.
	     * @param omitBackward   Kinds of edges to be omitted by the backward slicer.
	     * @return               The chop.
	     */
	    private Collection<SDGNode> branchChop (Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet,
	    		Set<SDGEdge.Kind> omitForward, Set<SDGEdge.Kind> omitBackward) {

	        forward.setOmittedEdges(omitForward);
	        backward.setOmittedEdges(omitBackward);

	        Collection<SDGNode> forwardSlice = forward.slice(sourceSet);
	        return backward.subgraphSlice(sinkSet, forwardSlice);
	    }
	}


	private final CFG g;
	/** A truncated unbound chopper */
	private TruncatedCFGChopper truncated;
	/** A CFGChopepr for the necessary same-level chops. */
	private CFGChopper cfgChopper;

	/**
     * Instantiates a NonSameLevelChopper with a SDG.
     *
     * @param g   A SDG. Can be null. Must not be a cSDG.
     */
    public ICFGChopper(CFG g) {
        this.g = g;
        setGraph();
    }

    /**
     * Re-initializes the two choppers.
     * Triggered by {@link Chopper#setGraph(SDG)}.
     */
    public void setGraph() {
        if (truncated == null) {
            truncated = new TruncatedCFGChopper();
        }
        truncated.setGraph();


        if (cfgChopper == null) {
        	cfgChopper = new CFGChopper(g);

        } else {
        	cfgChopper.setGraph(g);
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
        LinkedList<SDGNodeTuple> worklist = new LinkedList<SDGNodeTuple>();
        worklist.addAll(getCallReturnPairs(chop));
        Set<SDGNodeTuple> visitedTuples = new HashSet<SDGNodeTuple>();

        // gradually add chops for each summary edge
        while(!worklist.isEmpty()) {
            // get next fifo pair
            SDGNodeTuple next = worklist.poll();

            if (visitedTuples.add(next)) {
            	// the same-level chops are computed for each found summary edge, which causes the bad runtime behavior
                Collection<SDGNode> newChop = cfgChopper.chop(next.getFirstNode(), next.getSecondNode());
                chop.addAll(newChop);
                worklist.addAll(getCallReturnPairs(chop));
            }
        }
    }

    private Collection<SDGNodeTuple> getCallReturnPairs(Collection<SDGNode> chop) {
    	LinkedList<SDGNodeTuple> result = new LinkedList<SDGNodeTuple>();

    	for (SDGNode n : chop) {
    		if (n.getKind() == SDGNode.Kind.CALL) {
    			for (SDGEdge e : g.outgoingEdgesOf(n)) {
    				if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW
    						&& chop.contains(e.getTarget())) {

    					result.add(new SDGNodeTuple(n, e.getTarget()));
    				}
    			}
    		}
    	}

    	return result;
    }


    public static void main(String[] args) throws IOException {
    	for (String file : PDGs.pdgs) {
	    	SDG sdg = SDG.readFrom(file);
	    	System.out.println(file);
	    	CFG icfg = ICFGBuilder.extractICFG(sdg);
	    	CFGChopper chopper = new CFGChopper(icfg);
	    	LinkedList<SDGNode> sources = new LinkedList<SDGNode>();
	    	LinkedList<SDGNode> sinks = new LinkedList<SDGNode>();
	    	sources.addAll(icfg.getNRandomNodes(5));
	    	sinks.addAll(icfg.getNRandomNodes(5));

	    	Collection<SDGNode> chop = null;
	    	chop = chopper.chop(sources.get(0), sinks.get(0));
	    	System.out.println(chop.size());
	    	chop = chopper.chop(sources.get(1), sinks.get(1));
	    	System.out.println(chop.size());
	    	chop = chopper.chop(sources.get(2), sinks.get(2));
	    	System.out.println(chop.size());
	    	chop = chopper.chop(sources.get(3), sinks.get(3));
	    	System.out.println(chop.size());
	    	chop = chopper.chop(sources.get(4), sinks.get(4));
	    	System.out.println(chop.size());
    	}
    }
}
