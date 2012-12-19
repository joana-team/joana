/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;

import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.LookupDialog;

import java.awt.event.ActionEvent;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class LookupAction extends AbstractGVAction implements BundleConstants, ChangeListener {
	private static final long serialVersionUID = 612904713808421341L;
	private LookupDialog lookupDialog;
	private GraphPane graphPane;

	public LookupAction(LookupDialog lookupDialog, GraphPane graphPane) {
		super("lookup.name", "Search.png", "lookup.description", "lookup");
		this.lookupDialog = lookupDialog;
		this.graphPane = graphPane;
		graphPane.addChangeListener(this);
		this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent event) {
		lookupDialog.showLookforDialog();
	}

	public void stateChanged(ChangeEvent e) {
		if (this.graphPane.getSelectedIndex() == -1) {
			this.setEnabled(false);
		} else {
			this.setEnabled(true);
		}
	}
}
