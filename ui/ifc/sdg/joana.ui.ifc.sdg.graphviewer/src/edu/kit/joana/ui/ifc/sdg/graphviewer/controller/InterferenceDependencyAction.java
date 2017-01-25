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
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;

public class InterferenceDependencyAction extends AbstractGVAction implements BundleConstants, ChangeListener {
	private static final long serialVersionUID = -4416383715458486025L;

	private final GraphPane graphPane;
	private boolean showIF = true;

	public InterferenceDependencyAction(GraphPane pane) {
		super("interfereDep.name", "Checkmark.png", "interfereDep.description", "interfereDep");
		this.graphPane = pane;
		graphPane.addChangeListener(this);
		this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		showIF = !showIF;
		setIcon(showIF);

		Graph g = graphPane.getSelectedGraph();
		g.getEdgeViewSettings().setShowIF(showIF);
		g.changed();

////			return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
////					new Resource(COMMANDS_BUNDLE, "openMethod.success.status"));
////		}
	}

	void setIcon(boolean show_df) {
		if (show_df) {
			putValue(SMALL_ICON, GVUtilities.getIcon("Checkmark.png"));
		} else {
			putValue(SMALL_ICON, GVUtilities.getIcon("Close.png"));
		}
	}

	public void setShowIF(boolean show_if) {
		this.showIF = show_if;
	}

	public boolean isShowIF() {
		return showIF;
	}

	public void stateChanged(ChangeEvent e) {
		if(this.graphPane.getSelectedIndex() == -1)	{
			this.setEnabled(false);

		} else if (this.graphPane.getSelectedJGraph() instanceof CallGraphView) {
			this.setEnabled(false);

		} else {
			this.setEnabled(true);
			Graph g = graphPane.getSelectedGraph();
			showIF = g.getEdgeViewSettings().isShowIF();
			setIcon(showIF);
		}
	}
}

