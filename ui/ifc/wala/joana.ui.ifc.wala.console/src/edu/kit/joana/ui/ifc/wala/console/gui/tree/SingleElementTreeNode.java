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
package edu.kit.joana.ui.ifc.wala.console.gui.tree;

import edu.kit.joana.api.sdg.SDGProgramPart;

public class SingleElementTreeNode extends IFCTreeNode {


	private static final long serialVersionUID = 1887164889665731334L;
	private final SDGProgramPart part;

	public SingleElementTreeNode(SDGProgramPart part, boolean allowsChildren, boolean annotatable, Kind kind) {
		super(part, allowsChildren, annotatable, kind);
		this.part = part;
	}

	@Override
	public String toStringPrefix() {
		return part.acceptVisitor(ProgramPartToString.getStandard(), null);
	}

	public boolean matchesPart(SDGProgramPart part) {
		return part.equals(this.part);
	}


}
