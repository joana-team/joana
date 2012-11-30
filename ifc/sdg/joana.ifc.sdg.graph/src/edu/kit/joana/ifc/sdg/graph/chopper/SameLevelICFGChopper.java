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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;



/**
 * SameLevelICFGChopper computes context-sensitive same-level chops for ICFGs.
 *
 * @author  Dennis Giffhorn
 */
public class SameLevelICFGChopper {
	private CFG g;
	// The employed intra-procedural chopper
    private CFGChopper intraChopper;

    /**
     * Constructs a SMC for a given SDG.
     * @param g   The SDG to operate on. Can be null. Can be a cSDG.
     */
    public SameLevelICFGChopper(CFG g) {
        this.g = g;
    	intraChopper = new CFGChopper(g);
    }

    /**
     * Re-initializes the two choppers.
     */
    public void setGraph(CFG g) {
    	intraChopper.setGraph(g);
    }

    public Collection<SDGNode> chop(SDGNode source, SDGNode sink)
    throws InvalidCriterionException{
        return chop(Collections.singleton(source), Collections.singleton(sink));
    }

	/**
     * Computes a context-sensitive same-level chop from <code>sourceSet</code> to <code>sinkSet</code>.
     *
     * @param sourceSet  The source criterion set. Should not contain null, should not be empty.
     * @param sinkSet    The target criterion set. Should not contain null, should not be empty.
     * @return           The chop (a HashSet).
     * @throws InvalidCriterionException, if the nodes in sourceSet and targetSet do not belong to the same procedure
     * or if one of the sets is empty.
     */
    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet)
    throws InvalidCriterionException{
    	// the bouncer
    	if (!Chopper.testSameLevelSetCriteria(sourceSet, sinkSet)) {
    		// Thou shall not pass!
    		throw new InvalidCriterionException("This is not a same-level chopping criterion: "+sourceSet+", "+sinkSet);
        }

        // compute an initial intra-procedural chop between source and sink
        Collection<SDGNode> chop = intraChopper.chop(sourceSet, sinkSet);

        // collect the nodes on paths through called procedures
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
                Collection<SDGNode> newChop = intraChopper.chop(next.getFirstNode(), next.getSecondNode());
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
	    	List<CFG> procs = icfg.split();
	    	Collections.shuffle(procs);
	    	CFG proc = procs.get(0);
	    	SameLevelICFGChopper chopper = new SameLevelICFGChopper(proc);
	    	LinkedList<SDGNode> sources = new LinkedList<SDGNode>();
	    	LinkedList<SDGNode> sinks = new LinkedList<SDGNode>();
	    	sources.addAll(proc.getNRandomNodes(5));
	    	sinks.addAll(proc.getNRandomNodes(5));

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
