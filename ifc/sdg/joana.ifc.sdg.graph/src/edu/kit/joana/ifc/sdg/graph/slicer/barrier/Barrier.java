/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.barrier;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/** Interface for barrier slicers or choppers.
 * A barrier slicer or choppr receives a set of nodes - the barrier - which it will not trespass.
 */
public interface Barrier {

	/**
	 * Implementations should apply the given barrier to the slicing algorithm and
	 * should also determine the blocked summary edges.
	 *
	 * A convenient implementations is provided by class <code>BarrierManager</code>.
	 *
	 * @param barrier  The barrier.
	 */
    void setBarrier(Collection<SDGNode> barrier);

    /**
	 * Implementations should apply the given barrier to the slicing algorithm.
	 *
	 * A convenient implementations is provided by class <code>BarrierManager</code>.
	 *
	 * @param barrier  The barrier.
	 */
    void setBarrier(Collection<SDGNode> barrier, Collection<SDGEdge> blockedSummaryEdges);

	/**
	 * Allows barrier slicers to share the same BarrierManager.
	 *
	 * @param barrier  The BarrierManager to be shared.
	 */
    void setBarrier(BarrierManager barrier);
}
