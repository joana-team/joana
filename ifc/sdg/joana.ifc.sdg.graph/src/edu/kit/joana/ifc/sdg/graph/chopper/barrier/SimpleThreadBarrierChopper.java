/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper.barrier;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.BarrierManager;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.conc.I2PBarrierBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.conc.I2PBarrierForward;


/** This is a chopper for concurrent programs.
 * It computes a backward slice and then a forward slice on the backward slice.
 * The algorithm has an asymptotic running time of O (edges) and is pretty precise.
 *
 * -- Created on June 28, 2007
 *
 * @author  Dennis Giffhorn
 */
public class SimpleThreadBarrierChopper extends BarrierChopper {
    private BarrierManager barrier;

    // The employed barrier choppers, which share the above BarrierManager
    private I2PBarrierBackward back;
    private I2PBarrierForward forw;

    /**
     * Creates a new instance of SimpleThreadChopper
     *
     * @param g     A SDG.
     */
    public SimpleThreadBarrierChopper(SDG g) {
    	super(g);
    }

    protected void onSetGraph() {
    	if (barrier == null) {
        	barrier = new BarrierManager();
    	}

    	if (back == null) {
	        back = new I2PBarrierBackward(sdg);
	        back.setBarrier(barrier);
    	} else {
    		back.setGraph(sdg);
    	}

    	if (forw == null) {
	        forw = new I2PBarrierForward(sdg);
	        forw.setBarrier(barrier);
    	} else {
    		forw.setGraph(sdg);
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
        back.setBarrier(barrier);
        forw.setBarrier(barrier);
	}

    /*
     * (non-Javadoc)
     * @see edu.kit.joana.ifc.sdg.graph.chopper.SDGChopper#chop(java.util.Collection, java.util.Collection)
     */
    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet) {
        Collection<SDGNode> backSlice = back.slice(sinkSet);
        return forw.subgraphSlice(sourceSet, backSlice);
    }
}
