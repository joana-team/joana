/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;

import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.CallGraphView;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.HideNodeDialog;

import java.awt.event.ActionEvent;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class HideNodeAction extends AbstractGVAction implements BundleConstants, ChangeListener {
	private static final long serialVersionUID = -653706055036201073L;
	private HideNodeDialog hideNodeDialog = null;
	private GraphPane graphPane = null;

	public HideNodeAction(HideNodeDialog hideNodeDialog, GraphPane graphPane) {
		super("hidenode.name", "Search.png", "hidenode.description", "hidenode");
		this.hideNodeDialog = hideNodeDialog;
		this.graphPane = graphPane;
		graphPane.addChangeListener(this);
		this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		hideNodeDialog.showHideNodeDialog(this.graphPane);
	}

	public void stateChanged(ChangeEvent e) {
		if (this.graphPane.getSelectedIndex() == -1) {
			this.setEnabled(false);

		} else if (!(this.graphPane.getSelectedJGraph() instanceof CallGraphView)) {
			this.setEnabled(false);

		} else
			this.setEnabled(true);
	}
}
