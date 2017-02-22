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

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;

import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.CallGraphView;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;

/**
 * this class is called if a "all predecessors" action is called
 */
public class AllPredsAction extends AbstractGVAction implements BundleConstants, ChangeListener {
	private static final long serialVersionUID = 4942265703549807306L;
	private final GraphPane graphPane;
	private final PredAction pred;

	/**
	 * constructor
	 * @param graphPane
	 * 			instance of the graph pane
	 * @param pred
	 * 			instance of PredAction
	 */
	public AllPredsAction(GraphPane graphPane, PredAction pred) {
		super("allPreds.name", "AllPreds.png", "allPreds.description", "allPreds");
		this.graphPane = graphPane;
		this.pred = pred;
		graphPane.addChangeListener(this);
		this.setEnabled(false);
	}

	/**
	 * called if an action is performed
	 * iterates through all selected cells and calls the predecessor action
	 * for each of them
	 * @param event
	 */
	public void actionPerformed(ActionEvent event) {
		JGraph graph = graphPane.getSelectedJGraph();
		Object[] selection = graph.getSelectionCells();
		DefaultGraphCell cell = (DefaultGraphCell) selection[0];
		pred.setCell(cell);
		for (int i = 0; i < selection.length; i++) {
			pred.setCell((DefaultGraphCell) selection[i]);
			pred.actionPerformed(null);
		}
	}

	public void stateChanged(ChangeEvent e) {
		if (graphPane.getSelectedIndex() == -1) {
			setEnabled(false);

		} else if (graphPane.getSelectedJGraph() instanceof CallGraphView) {
			setEnabled(false);

		} else {
			setEnabled(true);
		}
	}
}
