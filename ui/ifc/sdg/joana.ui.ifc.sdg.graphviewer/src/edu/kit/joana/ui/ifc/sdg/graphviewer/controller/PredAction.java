/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

import org.jgraph.JGraph;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;

import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.util.VertexNode;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.SearchDialog;

/**
 * marks all predecessors for a vertex
 */
@SuppressWarnings("unchecked")
public class PredAction extends AbstractGVAction implements BundleConstants {
	private static final long serialVersionUID = 2627488872773418881L;

	protected SearchDialog searchDialog = null;

	protected GraphPane graphPane = null;

	private double xLoc = 0; // mouse click location for context menu

	private double yLoc = 0; // mouse click location for context menu

	private DefaultGraphCell defCell = null;

	private boolean dialog = false;

	/**
	 * constructor
	 * @param searchDialog
	 * 			instance of the search dialog
	 * @param graphPane
	 * 			instance of the graph pane
	 */
	public PredAction(SearchDialog searchDialog, GraphPane graphPane) {
		super("pred.name", "pred.description");
		this.searchDialog = searchDialog;
		this.graphPane = graphPane;
	}
	/**
	 * pred action performed
	 * @param event
	 */
	public void actionPerformed(ActionEvent event) {
		List<VertexNode> predList = new ArrayList<VertexNode>(); // stores predecessors
		DefaultGraphCell cell = null;
		boolean preds = false;  //true, if action called from PredsAction class

		// get cell information according to where the action was performed
		if (defCell == null) {
			if (event == null) {
				cell = searchDialog.currGraphCell;
				dialog = true;
			//action performed in context menu
			} else {
				cell = (DefaultGraphCell) this.graphPane.getSelectedJGraph()
						.getFirstCellForLocation(xLoc, yLoc);
			}
		// action performed in preds
		} else {
			cell = defCell;
			defCell = null;
			preds = true;
		}

		JGraph graph = this.graphPane.getSelectedJGraph();
		GraphModel model = graph.getModel();

		if (model.isEdge(cell)) {
			String string = cell.toString();
			String[] strings = string.split(" ");
			int cellId = Integer.parseInt(strings[0]);
			Object sourceVertex = model.getParent(model.getSource(cell));

			markCell((DefaultGraphCell) sourceVertex);
			relocation(cellId);

		} else {
			if (!graph.isCellSelected(cell)) {
				graph.addSelectionCell(cell);
			}
			searchDialog.predList = new ArrayList<VertexNode>();
			int numChildren = model.getChildCount(cell);

			/*
			 * iterate through all the children of the cell,
			 * if child is predecessor it is added to the list of predecessors
			 */
			for (int i = 0; i < numChildren; i++) {
				Object port = model.getChild(cell, i);
				if (model.isPort(port)) {
					Iterator<?> iter = model.edges(port);
					while (iter.hasNext()) {
						Object edge = iter.next();
						Object sourceVertex = model.getParent(model.getSource(edge));
//						Object targetVertex = model.getParent(model.getTarget(edge));
						if (!sourceVertex.equals(cell)) {
							searchDialog.predList.add(new VertexNode(
									(DefaultGraphCell) sourceVertex));
							predList.add(new VertexNode( //TODO should this be targetVertex?
									(DefaultGraphCell) sourceVertex));
						}
					}
				}
			}
			// make selection
			if (preds || event != null) {
				for (Iterator<VertexNode> p = predList.iterator(); p.hasNext();) {
					selectItem(p.next().getID(), true);
				}
			}
			if(dialog) {
				dialog = false;
			} else {
				//change border settings
				markCells(cell, predList);
			}
		}
	}

	private void relocation(int cellId) {
		JGraph component = this.graphPane.getSelectedJGraph();
		GraphModel model = component.getModel();
		CellView[] cellviews = component.getGraphLayoutCache().getCellViews();
		for (int i = 0; i < cellviews.length; i++) {
			int idid = 0;
			Rectangle2D rect = null;
			CellView c = cellviews[i];

			if (!model.isEdge(c) && !model.isPort(c)) {
				Object o = c.getAllAttributes().get("id");
				if (o != null) {
					idid = o.hashCode();
				}
			}
			if (idid == cellId) {
				rect = c.getBounds();
				// b = true;
				if (rect != null) {
					Point p = new Point();
					p.setLocation(rect.getCenterX(), rect.getCenterY());
					Dimension d = this.graphPane.getCenterPoint();

					component.setLocation((int) (d.width / 2 - p.getX()),
								(int) (d.height / 2 - p.getY()));
				}
			}
		}
	}

	private void markCell(DefaultGraphCell cell) {
		Border predBorder = BorderFactory.createLineBorder(Color.BLUE, 5);
		Map<DefaultGraphCell, Map<String, Border>> nested = new Hashtable<DefaultGraphCell, Map<String, Border>>();
		GraphLayoutCache view = graphPane.getSelectedJGraph()
		.getGraphLayoutCache();
		Map<String, Border> map = new Hashtable<String, Border>();
		map.put("border", BorderFactory.createLineBorder(Color.YELLOW, 5));
		// nested.put(cell, map);
		map.put("border", predBorder);
		nested.put(cell, map);
		view.edit(nested);
	}

	//select item
	private void selectItem(int id, boolean select) {
		JGraph graph = this.graphPane.getSelectedJGraph();
		GraphModel model = graph.getModel();
		Object[] cells = DefaultGraphModel.getAll(model);
		// iterate through all the cells
		for (Object o : cells) {
			if (!model.isPort(o)) {
				DefaultGraphCell cell = (DefaultGraphCell) o;
				if (!model.isEdge(cell)
						&& (cell.getAttributes()).get("kind") != null) {
					// cell is valid vertex
					int cellId = ((Integer) cell.getAttributes().get("id"))
							.intValue();
					if (id == cellId) {
						if (!graph.isCellSelected(cell)) {
							graph.addSelectionCell(cell);
							break;
						}
					}
				}
			}
		}
	}
	/**
	 * set borders
	 * @param cell
	 * @param predList
	 */
	private void markCells(DefaultGraphCell cell, List<VertexNode> predList){

		Border predBorder = BorderFactory.createLineBorder(Color.BLUE,5);
		Map<DefaultGraphCell, Map<String, Border>> nested = new Hashtable<DefaultGraphCell, Map<String, Border>>();
		GraphLayoutCache view = graphPane.getSelectedJGraph().getGraphLayoutCache();
		Map<String, Border> map = new Hashtable<String, Border>();
		map.put("border", BorderFactory.createLineBorder(Color.YELLOW,5));
		nested.put(cell, map);

		for (Iterator<VertexNode> p = predList.iterator(); p.hasNext();) {
			map = new Hashtable<String, Border>();
			DefaultGraphCell predCell = p.next().getCell();
			map.put("border", predBorder);
			nested.put(predCell, map);
		}
		view.edit(nested);
	}
	/**
	 * set x and y location for mouseclick
	 * @param x
	 * 		x location
	 * @param y
	 * 		y location
	 */
	public void setLocation(double x, double y) {
		xLoc = x;
		yLoc = y;
	}
	/**
	 * set cell
	 * @param defCell
	 */
	public void setCell(DefaultGraphCell defCell) {
		this.defCell = defCell;
	}
}
