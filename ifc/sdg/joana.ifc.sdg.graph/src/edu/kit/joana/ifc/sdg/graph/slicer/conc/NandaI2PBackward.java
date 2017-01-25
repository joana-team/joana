/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * This is the original iterated two-phase slicer from Nanda.
 * Use it for debugging or as a skeleton for more sophisticated algorithms.
 *
 * @author giffhorn
 */
public class NandaI2PBackward extends NandaI2PSlicer {

    public NandaI2PBackward(SDG g) {
    	super(g);
    }

    public NandaI2PBackward(SDG g, EdgeListener listener) {
    	super(g, listener);
    }

	public Collection<SDGEdge> edgesToTraverse(SDGNode n) {
		return g.incomingEdgesOf(n);
	}

	public SDGNode getAdjacentNode(SDGEdge e) {
		return e.getSource();
	}

	public boolean isAscendingEdge(Kind k) {
		return k == SDGEdge.Kind.CALL || k == SDGEdge.Kind.PARAMETER_IN;
	}

	public boolean isDescendingEdge(Kind k) {
		return k == SDGEdge.Kind.PARAMETER_OUT;
	}
}
