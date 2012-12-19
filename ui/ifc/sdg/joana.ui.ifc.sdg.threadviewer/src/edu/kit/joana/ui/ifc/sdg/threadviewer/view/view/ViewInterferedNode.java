/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.threadviewer.view.view;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

public class ViewInterferedNode {
	private SDGNode parent;
	private SDGNode item;

	public ViewInterferedNode(SDGNode parent, SDGNode item) {
		this.parent = parent;
		this.item = item;
	}

	public SDGNode getParent() {
		return this.parent;
	}

	public SDGNode getItem() {
		return this.item;
	}
}
