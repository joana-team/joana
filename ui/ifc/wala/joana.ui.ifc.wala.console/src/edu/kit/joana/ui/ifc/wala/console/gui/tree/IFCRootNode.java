/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui.tree;

import edu.kit.joana.api.sdg.SDGProgramPart;

public class IFCRootNode extends IFCTreeNode {

	/**
	 *
	 */
	private static final long serialVersionUID = 648154741394793815L;

	public IFCRootNode(String label) {
		super(label, true, false, Kind.ROOT);
		setUserObject(label);
		setAllowsChildren(true);
	}

	@Override
	public String toStringPrefix() {
		final Object obj = getUserObject();
		return (obj != null ? obj.toString() : "No object set.");
	}

	protected boolean matchesPart(SDGProgramPart part) {
		return false;
	}

}
