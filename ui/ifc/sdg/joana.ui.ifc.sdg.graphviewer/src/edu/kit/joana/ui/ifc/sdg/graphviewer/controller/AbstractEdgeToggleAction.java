/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;

import java.awt.event.ActionEvent;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.kit.joana.ui.ifc.sdg.graphviewer.model.Graph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.util.GVUtilities;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.CallGraphView;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.EdgeViewSettings;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;

public abstract class AbstractEdgeToggleAction extends AbstractGVAction implements BundleConstants, ChangeListener {

	private static final long serialVersionUID = 823085006192020998L;

	private final GraphPane graphPane;

	public AbstractEdgeToggleAction(GraphPane pane, String baseName) {
		super(baseName + ".name", "Checkmark.png", baseName + ".description", baseName);
		this.graphPane = pane;
		graphPane.addChangeListener(this);
		this.setEnabled(false);
	}

	protected abstract void setShowRelevantEdges(EdgeViewSettings settings, boolean show);
	protected abstract boolean isShowRelevantEdges(EdgeViewSettings settings);

	private void setShowRelevantEdges(boolean show) {
		setShowRelevantEdges(graphPane.getSelectedGraph().getEdgeViewSettings(), show);
	}

	private boolean isShowRelevantEdges() {
		return isShowRelevantEdges(graphPane.getSelectedGraph().getEdgeViewSettings());
	}

	public final void actionPerformed(ActionEvent e) {
		boolean showEdges = !isShowRelevantEdges();
		setIcon(showEdges);
		setShowRelevantEdges(showEdges);
		
		Graph g = graphPane.getSelectedGraph();
		g.changed();
	}

	protected final void setIcon(boolean show) {
		String iconString = show ? "Checkmark.png" : "Close.png";
		putValue(SMALL_ICON, GVUtilities.getIcon(iconString));
	}

	public final void stateChanged(ChangeEvent e) {
		if(this.graphPane.getSelectedIndex() == -1)	{
			this.setEnabled(false);

		} else if (this.graphPane.getSelectedJGraph() instanceof CallGraphView) {
			this.setEnabled(false);

		} else {
			this.setEnabled(true);
			setIcon(isShowRelevantEdges());
		}
	}
}

