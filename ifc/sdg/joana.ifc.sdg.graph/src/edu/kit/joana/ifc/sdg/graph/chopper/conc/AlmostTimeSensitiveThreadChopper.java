/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper.conc;

import java.util.Collection;
import java.util.HashSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaForward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaMode;


/**
 * The AlmostTimeSensitiveThreadChopper is a context-sensitive and almost time-sensitive chopper for concurrent programs.
 *
 * For a description of its concrete functionality please consult the following Journal article {@link TODO}.
 * It employs Nanda's time-sensitive slicer {@link edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda [Nanda]} for computing context- and
 * time-sensitive forward and backward slices and intersects them. Due to this intersection it is not completely
 * time-sensitive.
 *
 * The chopper serves only for evaluation purposes. It should not be used in an application, as the {@link ThreadChopper} is
 * both faster and more precise.
 *
 * @author  Dennis Giffhorn
 */
public class AlmostTimeSensitiveThreadChopper extends Chopper {
	/** A Nanda-style forward slicer. */
    private SubgraphSlicer forw;
	/** A Nanda-style backward slicer. */
    private SubgraphSlicer back;
	/** A canary for quick-checking if a chop is empty. */
    private SimpleThreadChopper canary;

    /**
     * Instantiates an AlmostTimeSensitiveThreadChopper with a SDG.
     *
     * @param g   A SDG or a cSDG. Can be null.
     */
    public AlmostTimeSensitiveThreadChopper(SDG g) {
    	super(g);
    }

    /**
     * Re-initializes the attributes.
     * Triggered by {@link Chopper#setGraph(SDG)}.
     */
    protected void onSetGraph() {
    	if (forw == null) {
    		forw = new SubgraphSlicer(sdg, new NandaForward());
    	} else {
    		forw.setGraph(sdg);
    	}

    	if (back == null) {
    		back = new SubgraphSlicer(sdg, new NandaBackward());
    	} else {
    		back.setGraph(sdg);
    	}

    	if (canary == null) {
    		canary = new SimpleThreadChopper(sdg);
    	} else {
    		canary.setGraph(sdg);
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
        Collection<SDGNode> canaryChop = canary.chop(sourceSet, sinkSet);

        if (canaryChop.isEmpty()) {
            return canaryChop;

        } else {
            Collection<SDGNode> backSlice = back.subgraphSlice(sinkSet, canaryChop);
            return forw.subgraphSlice(sourceSet, backSlice);
        }
    }

    /**
     * Subclass of {@link edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda}.
     * Overwrites method {@link edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda#omit(SDGEdge)} in order to restrict a slice to a given sub-graph.
     * @author giffhorn
     */
	static class SubgraphSlicer extends Nanda {
		/** A set of nodes that defines a sub-graph in a cSDG. */
    	private Collection<SDGNode> subGraph;

        /**
         * Creates a new instance of this algorithm.
         * @param graph  A cSDG.
         */
        public SubgraphSlicer(SDG graph, NandaMode mode) {
            super(graph, mode);
        }

        /**
         * @return `true' if the edge belongs to the sub-graph spanned by the nodes in <code>subGraph</code>.
         */
        protected boolean omit(SDGEdge edge) {
        	return !subGraph.contains(mode.adjacentNode(edge));
        }

        /**
         * Executes Nanda's slicing algorithm for a given set of slicing criterion.
         * Restricts the slice to the sub-graph spanned by the nodes in <code>sub</code>.
         *
         * @param crit  The slicing criterion.
         * @param sub   The nodes of a sub-graph.
         * @return      The slice.
         */
        public Collection<SDGNode> subgraphSlice(Collection<SDGNode> crit, Collection<SDGNode> sub) {
        	subGraph = sub;

        	HashSet<SDGNode> refined = new HashSet<SDGNode>();
        	for (SDGNode n : crit) { // remove all nodes from the criteria that are not in the sub-graph
                if (sub.contains(n)) {
                    refined.add(n);
                }
            }

        	return nandaSlice(refined);
        }
    }
}
