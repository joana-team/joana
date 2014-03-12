/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui.results;


public class IFCRootNode extends IFCResultNode {
	private static final long serialVersionUID = 648154741391113815L;

	public IFCRootNode(String label) {
		super(label, true);
		setUserObject(label);
		setAllowsChildren(true);
	}
}
