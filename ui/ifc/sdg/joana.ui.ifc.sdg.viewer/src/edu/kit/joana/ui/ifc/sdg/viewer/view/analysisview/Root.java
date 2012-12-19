/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview;

import java.util.ArrayList;

class Root implements Parent<GraphView> {
	// the children
	private ArrayList<GraphView> children;

	/** Creates a new Root.
	 */
	public Root() {
		children = new ArrayList<GraphView>();
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#getChildren()
	 */
	public GraphView[] getChildren() {
		return children.toArray(new GraphView[children.size()]);
	}

	/** Adds a new child.
	 *
	 * @param child  The new child.
	 */
	public void addChild(GraphView child) {
		children.add(child);
		child.setParent(this);
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#hasChildren()
	 */
	public boolean hasChildren() {
		return children.size() > 0;
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#removeChild(edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode)
	 */
	public void removeChild(GraphView child) {
		children.remove(child);
		child.setParent(null);
	}

}
