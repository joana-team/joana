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

public class InterferenceDependencyAction extends AbstractEdgeToggleAction {
	private static final long serialVersionUID = -4416383715458486025L;

	public InterferenceDependencyAction(GraphPane pane) {
		super(pane, "interfereDep");
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.AbstractEdgeToggleAction#setShowRelevantEdges(edu.kit.joana.ui.ifc.sdg.graphviewer.view.EdgeViewSettings, boolean)
	 */
	@Override
	protected void setShowRelevantEdges(EdgeViewSettings settings, boolean show) {
		settings.setShowIF(show);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.AbstractEdgeToggleAction#isShowRelevantEdges(edu.kit.joana.ui.ifc.sdg.graphviewer.view.EdgeViewSettings)
	 */
	@Override
	protected boolean isShowRelevantEdges(EdgeViewSettings settings) {
		return settings.isShowIF();
	}
}

