/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;

import edu.kit.joana.ui.wala.easyifc.Activator;

public class ExpandNodesAction extends Action {

	private final TreeViewer tree;

	public ExpandNodesAction(final TreeViewer tree) {
		this.tree = tree;
		this.setText("Expand view");
		this.setDescription("Expands all information flow checker results.");
		this.setId("joana.ui.wala.easyifc.expandNodesAction");
		this.setImageDescriptor(Activator.getImageDescriptor("icons/expandall.gif"));
	}

	public void run() {
		tree.expandToLevel(2);
	}

}
