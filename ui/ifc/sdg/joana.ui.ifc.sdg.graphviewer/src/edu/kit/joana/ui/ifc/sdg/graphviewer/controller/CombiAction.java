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
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.GraphViewerModel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.MethodGraph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.CallGraphView;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;

public class CombiAction extends AbstractGVAction implements ChangeListener {
	private static final long serialVersionUID = 2050067496603903127L;

	private final GraphViewerModel gvm;
	private final GraphPane graphPane;

	public CombiAction(GraphPane pane, GraphViewerModel gvm) {
		super("combi.name", "About.png", "combi.description", "combi");
		this.graphPane = pane;
		graphPane.addChangeListener(this);
		this.gvm = gvm;
		this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		Graph g = graphPane.getSelectedGraph();
		gvm.createPDG((MethodGraph)g);
//		return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
//				new Resource(COMMANDS_BUNDLE, "openMethod.success.status"));
	}

	public void stateChanged(ChangeEvent e) {
		if (graphPane.getSelectedIndex() == -1 || graphPane.getComponentCount() == 0) {
			setEnabled(false);
		} else if (graphPane.getSelectedJGraph() instanceof CallGraphView) {
			setEnabled(false);

		} else {
			setEnabled(true);
		}
	}
}
