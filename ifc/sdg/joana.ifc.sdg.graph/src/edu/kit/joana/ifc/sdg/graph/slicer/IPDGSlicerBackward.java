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
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.ContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.StaticContextManager.StaticContext;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/** Offers two context-based sequential slicing algorithms.
 *
 * -- Created on March 20, 2006
 *
 * @author  Dennis Giffhorn
 */
public class IPDGSlicerBackward<C extends Context<C>> extends IPDGSlicer<C> {
	/* ************** */
	/* the IPDGSlicer */
	
	public static IPDGSlicerBackward<StaticContext> newStaticIPDGSlicerBackward(SDG graph) {
		return new IPDGSlicerBackward<StaticContext>(graph, IPDGSlicer.newStaticManager);
	}
	
	public static IPDGSlicerBackward<DynamicContext> newDynamicIPDGSlicerBackward(SDG graph) {
		return new IPDGSlicerBackward<DynamicContext>(graph, IPDGSlicer.newDynamicManager);
	}
	public static IPDGSlicerBackward<? extends Context<?>> newIPDGSlicerBackward(SDG graph, boolean staticContexts) {
		if (staticContexts) {
			return newStaticIPDGSlicerBackward(graph);
		} else {
			return newDynamicIPDGSlicerBackward(graph);
		}
	}
	
    private  IPDGSlicerBackward(SDG graph, Function<SDG, ContextManager<C>> newManager) {
    	super(graph, newManager);
    }
    
	protected SDGNode getAdjacentNode(SDGEdge e) {
		return e.getSource();
	}

	protected Collection<SDGEdge> getEdges(SDGNode n) {
		return sdg.incomingEdgesOf(n);
	}

	protected boolean isAscendingEdge(Kind k) {
		return k == SDGEdge.Kind.CALL || k == SDGEdge.Kind.PARAMETER_IN;
	}

	protected boolean isDescendingEdge(Kind k) {
		return k == SDGEdge.Kind.PARAMETER_OUT;
	}
}

