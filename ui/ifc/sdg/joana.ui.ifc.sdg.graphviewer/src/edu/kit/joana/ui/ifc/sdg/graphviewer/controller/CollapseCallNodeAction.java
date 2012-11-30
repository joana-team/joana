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

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.GraphViewerModel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.MethodGraph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.SearchDialog;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;


import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;

public class CollapseCallNodeAction extends AbstractGVAction {
	private static final long serialVersionUID = 7049061345325288938L;

	protected SearchDialog searchDialog = null;

	protected GraphPane graphPane = null;

	protected GraphViewerModel model;

	public CollapseCallNodeAction(SearchDialog searchDialog, GraphPane graphPane, GraphViewerModel model) {
		super("collapse.name", "collapse.description");
		this.searchDialog = searchDialog;
		this.graphPane = graphPane;
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		JGraph graph = this.graphPane.getSelectedJGraph();
		DefaultGraphCell cell = (DefaultGraphCell) graph.getSelectionCell();

		if (!cell.getAttributes().get("kind").equals("CALL")) {
			return;
		}

		MethodGraph mg = (MethodGraph) this.graphPane.getSelectedGraph();
		SDG g = mg.getCompleteSDG();

		int id = (Integer) cell.getAttributes().get("id");

		List<SDGNode> collapse = new LinkedList<SDGNode>();

		SDG newSDG = g.clone();

		SDGNode call = g.getNode(id);

		for (SDGEdge e : g.outgoingEdgesOf(call)) {
			if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
				collapse.add(e.getTarget());
			}
		}

		List<SDGEdge> remove = new LinkedList<SDGEdge>();

		for (SDGNode n : collapse) {
			for (SDGEdge e : newSDG.incomingEdgesOf(n)) {
				if (e.getSource() != call) {
					newSDG.addEdge(new SDGEdge(e.getSource(), call, e.getKind()));
				}
				remove.add(e);
			}
			for (SDGEdge e : newSDG.outgoingEdgesOf(n)) {
				if (e.getTarget() != call) {
					newSDG.addEdge(new SDGEdge(call, e.getTarget(), e.getKind()));
				}
				remove.add(e);
			}
			newSDG.removeVertex(n);
		}
		newSDG.removeAllEdges(remove);

		model.openPDG(new MethodGraph(newSDG, mg.getProcID()), mg.getProcID());

	}

}
