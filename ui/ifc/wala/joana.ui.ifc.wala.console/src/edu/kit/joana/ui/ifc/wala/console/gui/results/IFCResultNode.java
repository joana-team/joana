/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui.results;

import javax.swing.tree.DefaultMutableTreeNode;

public class IFCResultNode extends DefaultMutableTreeNode {
	private static final long serialVersionUID = -1152984674006111724L;

	public IFCResultNode(Object userObject, boolean allowsChildren) {
		setUserObject(userObject);
		setAllowsChildren(allowsChildren);
	}

}
