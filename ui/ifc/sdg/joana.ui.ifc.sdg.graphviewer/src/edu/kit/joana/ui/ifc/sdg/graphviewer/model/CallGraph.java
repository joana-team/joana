/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.model;

import edu.kit.joana.ifc.sdg.graph.SDG;

public class CallGraph extends Graph {
	private static final long serialVersionUID = -1215818519955060425L;

	private SDG call;
	private SDG complete;

	public CallGraph(SDG call, SDG sdg) {
		this.call = call;
		this.complete = sdg;
	}

	public SDG getSDG() {
		return call;
	}

	public SDG getCompleteSDG() {
		return complete;
	}

	public String getName() {
		return call.getName();
	}
}
