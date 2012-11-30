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
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;

import edu.kit.joana.ui.ifc.sdg.graphviewer.model.Graph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.util.GVUtilities;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.CallGraphView;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;

import java.awt.event.ActionEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class DataDependencyAction extends AbstractGVAction implements BundleConstants, ChangeListener {
	private static final long serialVersionUID = -4416383715458486025L;

	private final GraphPane graphPane;
	private boolean showDD = true;

	public DataDependencyAction(GraphPane pane) {
		super("datenDep.name", "Checkmark.png", "datenDep.description", "datenDep");
		this.graphPane = pane;
		graphPane.addChangeListener(this);
		this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		showDD = !showDD;
		setIcon(showDD);

		Graph g = graphPane.getSelectedGraph();
		g.getEdgeViewSettings().setShowDD(showDD);
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

	public void setShowDD(boolean show_dd) {
		this.showDD = show_dd;
	}

	public boolean isShowDD() {
		return showDD;
	}

	public void stateChanged(ChangeEvent e) {
		if(this.graphPane.getSelectedIndex() == -1)	{
			this.setEnabled(false);

		} else if (this.graphPane.getSelectedJGraph() instanceof CallGraphView) {
			this.setEnabled(false);

		} else {
			this.setEnabled(true);
			Graph g = graphPane.getSelectedGraph();
			showDD = g.getEdgeViewSettings().isShowDD();
			setIcon(showDD);
		}
	}
}

