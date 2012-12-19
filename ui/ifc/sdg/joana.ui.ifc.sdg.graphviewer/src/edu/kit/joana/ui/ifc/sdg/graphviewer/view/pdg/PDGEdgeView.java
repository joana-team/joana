/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view.pdg;

import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;

public class PDGEdgeView extends EdgeView {
	private static final long serialVersionUID = 4695351581942744643L;

	PDGEdgeView(Object cell, PDGAttributeMap map){
		super(cell);
		GraphConstants.setRouting(map, new MyRouting());
	}
}
