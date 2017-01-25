/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import javax.swing.JScrollPane;

import org.jgraph.JGraph;

import edu.kit.joana.ui.ifc.sdg.graphviewer.model.Graph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.GraphObserver;

public class GraphPaneTab extends JScrollPane implements GraphObserver {
	private static final long serialVersionUID = 6343682371894913075L;

	private Graph graph;
	private GraphPane parent;

	public GraphPaneTab(GraphPane parent, JGraph jgraph, Graph graph) {
		super(jgraph);
		this.parent = parent;
		this.graph = graph;
		graph.attach(this);
	}

	public Graph getGraph() {
		return graph;
	}

	public void close() {
		parent.remove(this);
		graph = null;
	}

	public void refresh() {
		parent.refreshView(null);
	}
}
