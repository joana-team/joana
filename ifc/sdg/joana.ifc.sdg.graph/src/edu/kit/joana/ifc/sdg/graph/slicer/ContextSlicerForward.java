/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.util.Collection;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;


/** Offers two context-based sequential slicing algorithms.
 *
 * -- Created on March 20, 2006
 *
 * @author  Dennis Giffhorn
 */
public class ContextSlicerForward extends ContextSlicer {
	/* ***************** */
	/* the ContextSlicer */

    public ContextSlicerForward(SDG graph, boolean staticContexts) {
    	super(graph, staticContexts);
    }

    public ContextSlicerForward(SDG graph, Set<SDGEdge.Kind> omit, boolean staticContexts) {
    	super(graph, omit, staticContexts);
    }

	protected boolean isAscendingEdge(Kind k) {
		return k == SDGEdge.Kind.PARAMETER_OUT;
	}

	protected boolean isDescendingEdge(Kind k) {
		return k == SDGEdge.Kind.CALL || k == SDGEdge.Kind.PARAMETER_IN;
	}

	protected SDGNode getAdjacentNode(SDGEdge e) {
		return e.getTarget();
	}

	protected Collection<SDGEdge> getEdges(SDGNode n) {
		return sdg.outgoingEdgesOf(n);
	}
}

