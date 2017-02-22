/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;

import edu.kit.joana.ui.ifc.sdg.graphviewer.view.EdgeViewSettings;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;

public class ControlFlowAction extends AbstractEdgeToggleAction {
	private static final long serialVersionUID = -7330872627112676272L;

	public ControlFlowAction(GraphPane pane) {
		super(pane, "controlFlow");
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.AbstractEdgeToggleAction#setShowRelevantEdges(edu.kit.joana.ui.ifc.sdg.graphviewer.view.EdgeViewSettings, boolean)
	 */
	@Override
	protected void setShowRelevantEdges(EdgeViewSettings settings, boolean show) {
		settings.setShowCF(show);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.AbstractEdgeToggleAction#isShowRelevantEdges(edu.kit.joana.ui.ifc.sdg.graphviewer.view.EdgeViewSettings)
	 */
	@Override
	protected boolean isShowRelevantEdges(EdgeViewSettings settings) {
		return settings.isShowCF();
	}
}
