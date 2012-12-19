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

public class ControllFlussAction extends AbstractGVAction implements BundleConstants, ChangeListener {
	private static final long serialVersionUID = -7330872627112676272L;

	private final GraphPane graphPane;
	private boolean showCF = true;

	public ControllFlussAction(GraphPane pane) {
		super("hide.name", "Checkmark.png", "hide.description", "hide");
		this.graphPane = pane;
		graphPane.addChangeListener(this);
		this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent event) {
		showCF = !showCF;
		setIcon(showCF);

		Graph g = graphPane.getSelectedGraph();
		g.getEdgeViewSettings().setShowCF(showCF);
		g.changed();

//			return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
//					new Resource(COMMANDS_BUNDLE, "openMethod.success.status"));
//		}
	}

	void setIcon(boolean showCF) {
		if (showCF) {
			putValue(SMALL_ICON, GVUtilities.getIcon("Checkmark.png"));
		} else {
			putValue(SMALL_ICON, GVUtilities.getIcon("Close.png"));
		}
	}

	public void setShowCF(boolean show_cf) {
		this.showCF = show_cf;
	}

	public boolean isShowCF() {
		return showCF;
	}

	public void stateChanged(ChangeEvent e) {
		if(this.graphPane.getSelectedIndex() == -1)	{
			this.setEnabled(false);

		} else if (this.graphPane.getSelectedJGraph() instanceof CallGraphView) {
			this.setEnabled(false);

		} else {
			this.setEnabled(true);
			Graph g = graphPane.getSelectedGraph();
			showCF = g.getEdgeViewSettings().isShowCF();
			setIcon(showCF);
		}
	}
}
