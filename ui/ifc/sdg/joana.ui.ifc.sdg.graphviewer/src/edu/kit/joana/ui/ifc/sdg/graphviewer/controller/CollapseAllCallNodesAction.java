/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.GraphViewerModel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.MethodGraph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.CallGraphView;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.SearchDialog;


public class CollapseAllCallNodesAction extends AbstractGVAction implements ChangeListener {
	private static final long serialVersionUID = -4756330385871999294L;

	protected SearchDialog searchDialog = null;

	protected GraphPane graphPane = null;

	protected GraphViewerModel model;

	public CollapseAllCallNodesAction(SearchDialog searchDialog, GraphPane graphPane, GraphViewerModel model) {
		super("collapseall.name", "collapseall.description");
		this.searchDialog = searchDialog;
		this.graphPane = graphPane;
		this.model = model;
		this.setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {

		List<SDGNode> toDo = new LinkedList<SDGNode>();

		for (SDGNode n : this.graphPane.getSelectedGraph().getSDG().vertexSet()) {
			if (n.getKind() == SDGNode.Kind.CALL) {
				toDo.add(n);
			}
		}

		MethodGraph mg = (MethodGraph) this.graphPane.getSelectedGraph();
		SDG g = mg.getCompleteSDG();
		SDG newSDG = g.clone();

		for (SDGNode n : toDo) {
			List<SDGNode> collapse = new LinkedList<SDGNode>();
			SDGNode call = n;

			for (SDGEdge e : g.outgoingEdgesOf(call)) {
				if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
					collapse.add(e.getTarget());
				}
			}

			List<SDGEdge> remove = new LinkedList<SDGEdge>();

			for (SDGNode col : collapse) {
				for (SDGEdge e : newSDG.incomingEdgesOf(col)) {
					if (e.getSource() != call) {
						newSDG.addEdge(e.getKind().newEdge(e.getSource(), call));
					}
					remove.add(e);
				}
				for (SDGEdge e : newSDG.outgoingEdgesOf(col)) {
					if (e.getTarget() != call) {
						newSDG.addEdge(e.getKind().newEdge(call, e.getTarget()));
					}
					remove.add(e);
				}
				newSDG.removeVertex(col);
			}
			newSDG.removeAllEdges(remove);
		}

		model.openPDG(new MethodGraph(newSDG, mg.getProcID()), mg.getProcID());

	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		if(this.graphPane.getSelectedIndex() == -1)	{
			this.setEnabled(false);
		} else if (this.graphPane.getSelectedJGraph() instanceof CallGraphView) {
			this.setEnabled(false);
		} else {
			this.setEnabled(true);
		}
	}

}
