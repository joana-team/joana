/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper.barrier;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.BarrierManager;


/** This is a context-sensitive barrier chopper for concurrent programs.
 *
 * @author  Dennis Giffhorn
 */
public class ContextSensitiveThreadBarrierChopper extends BarrierChopper {
    class Criterion {
        LinkedList<SDGNode> source = new LinkedList<SDGNode>();
        LinkedList<SDGNode> sink = new LinkedList<SDGNode>();

        public String toString() {
            return "source: "+source+", sink: "+sink;
        }
    }

    // The employed barrier choppers, which share the same BarrierManager
    private NonSameLevelBarrierChopper chopper;
    private SimpleThreadBarrierChopper sc;
    private List<SDGEdge> interference;
    private BarrierManager barrier;

    /**
     * Creates a new instance.
     */
    public ContextSensitiveThreadBarrierChopper(SDG g) {
    	super(g);
    }

    protected void onSetGraph() {
    	if (barrier == null) {
        	barrier = new BarrierManager();
    	}

    	if (chopper == null) {
	        chopper = new NonSameLevelBarrierChopper(sdg);
	        chopper.setBarrier(barrier);
    	} else {
	        chopper.setGraph(sdg);
    	}

    	if (sc == null) {
	        sc = new SimpleThreadBarrierChopper(sdg);
	        sc.setBarrier(barrier);
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

	public void setBarrier(Collection<SDGNode> barrier) {
    	this.barrier.setBarrier(sdg, barrier);
	}

	public void setBarrier(Collection<SDGNode> barrier, Collection<SDGEdge> blockedSummaryEdges) {
		this.barrier.setBarrier(barrier, blockedSummaryEdges);
	}

	public void setBarrier(BarrierManager barrier) {
    	this.barrier = barrier;
        chopper.setBarrier(barrier);
        sc.setBarrier(barrier);
	}

    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet) {
        Collection<SDGNode> sChop = sc.chop(sourceSet, sinkSet);

        if (sChop.isEmpty()) {
            return sChop;
        }

        LinkedList<SDGEdge> edges = new LinkedList<SDGEdge>();

        for (SDGEdge i : interference) {
            if (sChop.contains(i.getSource()) && sChop.contains(i.getTarget())) {
                edges.add(i);
            }
        }

        Criterion crit = computeCriterion(sourceSet, sinkSet, edges);
        Collection<SDGNode> chop = chopper.chop(crit.source, crit.sink);

        return chop;
    }

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
