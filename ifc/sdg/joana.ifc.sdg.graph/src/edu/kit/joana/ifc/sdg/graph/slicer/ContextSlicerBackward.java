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
import java.util.function.Function;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.ContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.StaticContextManager.StaticContext;


/** Offers two context-based sequential slicing algorithms.
 *
 * -- Created on March 20, 2006
 *
 * @author  Dennis Giffhorn
 */
public class ContextSlicerBackward<C extends Context<C>> extends ContextSlicer<C> {

	/* ***************** */
	/* the ContextSlicer */

	
	public static ContextSlicerBackward<StaticContext> newStaticContextSlicerBackward(SDG graph) {
		return new ContextSlicerBackward<StaticContext>(graph, ContextSlicer.newStaticManager);
	}
	
	public static ContextSlicerBackward<DynamicContext> newDynamicContextSlicerBackward(SDG graph) {
		return new ContextSlicerBackward<DynamicContext>(graph, ContextSlicer.newDynamicManager);
	}
	public static ContextSlicerBackward<? extends Context<?>> newContextSlicerBackward(SDG graph, boolean staticContexts) {
		if (staticContexts) {
			return newStaticContextSlicerBackward(graph);
		} else {
			return newDynamicContextSlicerBackward(graph);
		}
	}
	
    private  ContextSlicerBackward(SDG graph, Function<SDG, ContextManager<C>> newManager) {
    	super(graph, newManager);
    }

	protected boolean isAscendingEdge(Kind k) {
		return k == SDGEdge.Kind.CALL || k == SDGEdge.Kind.PARAMETER_IN;
	}

	protected boolean isDescendingEdge(Kind k) {
		return k == SDGEdge.Kind.PARAMETER_OUT;
	}

	protected SDGNode getAdjacentNode(SDGEdge e) {
		return e.getSource();
	}

	protected Collection<SDGEdge> getEdges(SDGNode n) {
		return sdg.incomingEdgesOf(n);
	}
}

