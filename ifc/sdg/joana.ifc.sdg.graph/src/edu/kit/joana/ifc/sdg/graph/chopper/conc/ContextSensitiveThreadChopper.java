/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper.conc;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.NonSameLevelChopper;


/**
 * The ContextSensitiveThreadChopper is a context-sensitive, time-insensitive chopper for concurrent programs.
 *
 * For a description of its concrete functionality please consult the following Journal article {@link TODO}.
 * It employs the {@link edu.kit.joana.ifc.sdg.graph.chopper.NonSameLevelChopper [NonSameLevelChopper]} for computing context-sensitive
 * chops in the single threads. It might be convenient to replace it someday with the
 * {@link edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper [RepsRosayChopper]}, but at the moment the NonSameLevelChopper seems to be
 * a bit faster in practice.
 *
 * If used for a sequential program the algorithm falls back to the
 * {@link edu.kit.joana.ifc.sdg.graph.chopper.NonSameLevelChopper [NonSameLevelChopper]}.
 *
 * @author  Dennis Giffhorn
 */
public class ContextSensitiveThreadChopper extends Chopper {
	/**
	 * A simple data container that bundles source- and target nodes of a chop.
	 */
    class Criterion {
        LinkedList<SDGNode> source = new LinkedList<SDGNode>();
        LinkedList<SDGNode> sink = new LinkedList<SDGNode>();

        public String toString() {
            return "source: "+source+", sink: "+sink;
        }
    }

    /** The underlying context-sensitive chopper for single threads. */
    private NonSameLevelChopper chopper;
    /** Used for the determination of the interference edges that belong to the chop. */
    private SimpleThreadChopper sc;
    /** Stores all interference edges that are in the current cSDG. */
    private List<SDGEdge> interference;

    /**
     * Instantiates a ContextSensitiveThreadChopper with a SDG.
     *
     * @param g   A SDG or a cSDG. Can be null.
     */
    public ContextSensitiveThreadChopper(SDG g) {
    	super(g);
    }

    /**
     * Re-initializes the attributes.
     * Triggered by {@link Chopper#setGraph(SDG)}.
     */
    protected void onSetGraph() {
    	if (sdg != null) {
	    	if (chopper == null) {
		        chopper = new NonSameLevelChopper(sdg);
	    	} else {
		        chopper.setGraph(sdg);
	    	}

	    	if (sc == null) {
		        sc = new SimpleThreadChopper(sdg);
	    	} else {
		        sc.setGraph(sdg);
	    	}

	        interference = new LinkedList<SDGEdge>();
	        for (SDGEdge e : sdg.edgeSet()) {
	            if (e.getKind() == SDGEdge.Kind.INTERFERENCE
	                    ||e.getKind() == SDGEdge.Kind.FORK
	                    || e.getKind() == SDGEdge.Kind.FORK_IN) {

	                interference.add(e);
	            }
	        }
    	}
    }

    /**
     * Computes a context-sensitive, time-insensitive chop from <code>sourceSet</code> to <code>sinkSet</code>.
     *
     * @param sourceSet  The source criterion set. Should not contain null, should not be empty.
     * @param sinkSet    The target criterion set. Should not contain null, should not be empty.
     * @return           The chop (a HashSet).
     */
    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet) {
    	// determine the interference edges that belong to the chop
        Collection<SDGNode> sChop = sc.chop(sourceSet, sinkSet);

        if (!sChop.isEmpty()) {
            LinkedList<SDGEdge> edges = new LinkedList<SDGEdge>();

            // this could be optimized A LOT! (but I don't care anymore)
            for (SDGEdge i : interference) {
                if (sChop.contains(i.getSource()) && sChop.contains(i.getTarget())) {
                    edges.add(i);
                }
            }

            // build the criterion for the Reps-Rosay chopper and run the chop
            Criterion crit = computeCriterion(sourceSet, sinkSet, edges);
            Collection<SDGNode> chop = chopper.chop(crit.source, crit.sink);

            return chop;

        } else {
        	// shortcut: the chop is empty
            return sChop;
        }
    }

    /**
     * Puts all nodes in sourceSet and all target nodes of the given edges in one set,
     * and all nodes in sinkSet and all source nodes of the given edges in another set.
     *
     * @param sourceSet    The source criterion of the chop.
     * @param sinkSet      The target criterion of the chop.
     * @param interfering  A set of interference edges.
     * @return the two sets, wrapped into a Criterion object.
     */
    private Criterion computeCriterion(Collection<SDGNode> sourceSet,
            Collection<SDGNode> sinkSet, Collection<SDGEdge> interfering) {

        Criterion crit = new Criterion();

        crit.source.addAll(sourceSet);
        crit.sink.addAll(sinkSet);

        for (SDGEdge e : interfering) {
            crit.source.add(e.getTarget());
            crit.sink.add(e.getSource());
        }

        return crit;
    }
}
