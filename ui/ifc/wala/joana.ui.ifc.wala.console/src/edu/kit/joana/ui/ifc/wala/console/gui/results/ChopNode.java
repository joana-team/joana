/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui.results;

import edu.kit.joana.api.sdg.SDGProgramPart;

public class ChopNode extends IFCResultNode {
	private static final long serialVersionUID = 188111488966573334L;

	private final SDGProgramPart source;
	private final SDGProgramPart sink;
    public boolean wasComputed = false;

	public ChopNode(SDGProgramPart source, SDGProgramPart sink) {
		super("Chop - double-click to compute", true);
		this.source = source;
        this.sink = sink;
	}

    public SDGProgramPart getSource() {
        return this.source;
    }

    public SDGProgramPart getSink() {
        return this.sink;
    }
}
