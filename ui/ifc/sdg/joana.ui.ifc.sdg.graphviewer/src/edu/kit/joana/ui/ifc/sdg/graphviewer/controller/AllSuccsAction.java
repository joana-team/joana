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
 * this class is called if a "all successors" action is called
 */
public class AllSuccsAction extends AbstractGVAction implements BundleConstants, ChangeListener {
	private static final long serialVersionUID = 7464281738618021474L;
	private final GraphPane graphPane;
	private final SuccAction succ;
	/**
	 * constructor
	 * @param graphPane
	 * 			instance of the graph pane
	 * @param pred
	 * 			instance of SuccAction
	 */
	public AllSuccsAction(GraphPane graphPane, SuccAction succ) {
		super("allSuccs.name", "AllSuccs.png", "allSuccs.description", "allSuccs");
		this.graphPane = graphPane;
		this.succ = succ;
		graphPane.addChangeListener(this);
		this.setEnabled(false);
	}
	/**
	 * called if an action is performed
	 * iterates through all selected cells and calls the successor action
	 * for each of them
	 * @param event
	 */
	public void actionPerformed(ActionEvent event) {
		JGraph graph = graphPane.getSelectedJGraph();
		Object[] selection = graph.getSelectionCells();

		DefaultGraphCell cell = (DefaultGraphCell) selection[0];
		succ.setCell(cell);


		for (int i = 0; i < selection.length; i++) {
			succ.setCell((DefaultGraphCell) selection[i]);
			succ.actionPerformed(null);
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
