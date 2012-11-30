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

public class ControlDependencyAction extends AbstractGVAction implements BundleConstants, ChangeListener {
	private static final long serialVersionUID = -4416383715458486025L;

	private final GraphPane graphPane;
	private boolean showCD = true;

	public ControlDependencyAction(GraphPane pane) {
		super("controlDep.name", "Checkmark.png", "controlDep.description", "controlDep");
		this.graphPane = pane;
		graphPane.addChangeListener(this);
		this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		showCD = !showCD;
		setIcon(showCD);

		Graph g = graphPane.getSelectedGraph();
		g.getEdgeViewSettings().setShowCD(showCD);
		g.changed();

//			return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
//					new Resource(COMMANDS_BUNDLE, "openMethod.success.status"));
//		}
	}

	void setIcon(boolean show_df) {
		if (show_df) {
			putValue(SMALL_ICON, GVUtilities.getIcon("Checkmark.png"));
		} else {
			putValue(SMALL_ICON, GVUtilities.getIcon("Close.png"));
		}
	}

	public void setShowCD(boolean showCD) {
		this.showCD = showCD;
	}

	public boolean isShowCD() {
		return showCD;
	}

	public void stateChanged(ChangeEvent e) {
		if(this.graphPane.getSelectedIndex() == -1)	{
			this.setEnabled(false);

		} else if (this.graphPane.getSelectedJGraph() instanceof CallGraphView) {
			this.setEnabled(false);

		} else {
			this.setEnabled(true);
			Graph g = graphPane.getSelectedGraph();
			showCD = g.getEdgeViewSettings().isShowCD();
			setIcon(showCD);
		}
	}
}

