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
package edu.kit.joana.ui.wala.flowless.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;

import edu.kit.joana.ui.wala.flowless.Activator;

public class CollapseNodesAction extends Action {

	private final TreeViewer tree;

	public CollapseNodesAction(final TreeViewer tree) {
		this.tree = tree;
		this.setText("Collapse view");
		this.setDescription("Collapses all annotation checker results to show only the annotated methods.");
		this.setId("joana.ui.wala.flowless.collapseNodesAction");
		this.setImageDescriptor(Activator.getImageDescriptor("icons/collapseall.gif"));
	}

	public void run() {
		tree.collapseAll();
	}

}
