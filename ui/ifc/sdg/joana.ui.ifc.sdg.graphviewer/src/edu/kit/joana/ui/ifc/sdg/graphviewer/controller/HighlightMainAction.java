/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;

import edu.kit.joana.ui.ifc.sdg.graphviewer.model.CallGraph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.Graph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.CallGraphView;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.MainFrame;

import java.awt.event.ActionEvent;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class HighlightMainAction extends AbstractGVAction implements BundleConstants, ChangeListener {
	private static final long serialVersionUID = -653706055036201073L;
	private GraphPane graphPane;
	private MainFrame parent;

	public HighlightMainAction(MainFrame parent, GraphPane graphPane) {
		super("highlightmain.name", "Search.png", "highlightmain.description", "highlightmain");
		this.graphPane = graphPane;
		this.parent = parent;
		graphPane.addChangeListener(this);
		this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		Graph g = parent.getGraphPane().getSelectedGraph();
		parent.getModel().hideNode((CallGraph) g, ".*<clinit>.*");

//		this.parent.getCommandManager().invoke(
//				new HideNodeCommand(openAction, this.parent.model, packageName, this.parent));

//		return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
//				new Resource(COMMANDS_BUNDLE, "open.success.status"));
//		return new CommandStatusEvent(this, CommandStatusEvent.FAILURE,
//				new Resource(COMMANDS_BUNDLE, "open.failure.status"));

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
