/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)PDG.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 03.12.2004 at 17:15:54
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.ActionMap;

import org.jgraph.JGraph;
import org.jgraph.graph.BasicMarqueeHandler;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;
import org.jgraph.layout.JGraphLayoutAlgorithm;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.OpenMethodAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.AttributeMapAdjustmentsEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.AttributeMapAdjustmentsListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.Graph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVPopupMenu;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.pdg.PDGAttributeMap;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.pdg.PDGViewFactory;

/**
 * A JGraph, extended by a popup menu for the nodes and language support.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class CallGraphView extends JGraph implements AttributeMapAdjustmentsListener {
	private static final long serialVersionUID = -8428576938867581545L;

	// the menu that opens upon right mouse click on a vertex TODO: disable for
	// method graphs, add option to change vertex colour (open vertex
	// adjustments panel) in #initPopups()
	private GVPopupMenu popup = null;

	protected Translator translator = null;

	protected ActionMap actions = null;
	private final Graph call;

	/**
	 * Constructs a new <code>PDG</code> object.
	 */
	public CallGraphView(String name, GraphModel model, Translator translator,
			ActionMap actions, Graph call) {
		// draw
		super(model, new GraphLayoutCache(model, PDGViewFactory.getInstance()),
				new BasicMarqueeHandler());
		this.setName(name);
		this.translator = translator;
		this.actions = actions;
		this.call = call;
		this.initPopups();
		this.setEditable(false);
		this.addMouseListener(new PopupListener());
		((PDGAttributeMap) PDGViewFactory.getInstance().getAttributeMap(
				SDGNode.Kind.ENTRY)).addAttributeMapListener(this);
//		setAntiAliased(true);
	}

	private void initPopups() {
		popup = new GVPopupMenu(this.translator);
		popup.add(this.actions.get(OpenMethodAction.class));
	}

	/**
	 * applies a layout to the graph, whether the algorith can handle cycles and
	 * recursions or not
	 *
	 * @param layout
	 *            The layoutalgorithm the graph is to be layouted with
	 */

	public void applyLayout(JGraphLayoutAlgorithm layout) {
		// System.out.println("PDG#applyLayout");
		GraphModel model = getModel();

		// delete recursive edges
		Object[][] recEdges = removeRecursive();
		// System.out.println("PDG#removeRecursive done");

		// remove cycles
		Object[] cyclicEdges = unCycle(false);
		// System.out.println("PDG#unCycle done");
		// System.out.println("un-Algs done, beginning Layouting");

		// apply layout
		JGraphLayoutAlgorithm.applyLayout(this, layout, DefaultGraphModel.getRoots(model));

		// reapply cycles
		reCycle(cyclicEdges);

		// reapply recursive edges
		reApplyRecursives(recEdges);
	}

	/**
	 * removes recursive Edges from a graph
	 *
	 * @param model
	 *            the graphmodel that is to be made recursionfree
	 * @return an object[i][j] Array where i is the number of edges and j=0 is
	 *         the vertex of the edge and j=1 is the attribute map of the edge
	 */
	private Object[][] removeRecursive() {
		final GraphModel model = this.getModel();
		// get all rootcells (vertexes, edges)
		Object[] cells = DefaultGraphModel.getRoots(model, DefaultGraphModel.getAll(model));

		// //System.out.println(cells.length);
		List<DefaultEdge> recEdg = new ArrayList<DefaultEdge>();
		for (int i = 0; i < cells.length; i++) {
			final Object curr = cells[i];
			// choose vertex that is no edge
			if (model.isEdge(curr)) {
				Object source = DefaultGraphModel.getSourceVertex(model, curr);
				Object target = DefaultGraphModel.getTargetVertex(model, curr);
				if (target == source) {
					// System.out.println("recursive edge found on vertex: "
					// + target);
					// add egde to list of recursive edges
					DefaultEdge edge = (DefaultEdge) curr;
					recEdg.add(edge);
				}
			}
		}
		Object[][] edgData = new Object[recEdg.size()][2];
		for (int i = 0; i < recEdg.size(); i++) {
			edgData[i][0] = recEdg.get(i).getSource();
			edgData[i][1] = model.getAttributes(recEdg.get(i));
		}
		model.remove(recEdg.toArray());

		return edgData;
	}

	/**
	 * reapplies recursive Edges to a graph
	 *
	 * @param model
	 *            the graphmodel that is to be made recursionfree
	 * @param recVert
	 *            an object[i][j] Array where i is the number of edges and j=0
	 *            is the vertex of the edge and j=1 is the attribute map of the
	 *            edge
	 */

	private void reApplyRecursives(Object[][] recVert) {
		DefaultGraphModel model = (DefaultGraphModel) this.getModel();
		if (recVert.length > 0) {
			Hashtable<Object, Object> attribs = new Hashtable<Object, Object>();
			ConnectionSet cs = new ConnectionSet();
			Object[] recEdg = new Object[recVert.length];
			for (int i = 0; i < recVert.length; i++) {
				Object vert = (recVert[i][0]);
				// ////System.out.println((DefaultGraphCell)vert);
				recEdg[i] = new DefaultEdge();
				cs.connect(recEdg[i], vert, vert);
				attribs.put(recEdg[i], recVert[i][1]);

				// TODO: fuer routing sorgen, vllt mit Unterklasse
			}
			model.insert(recEdg, attribs, cs, null, null);
		}
	}

	/**
	 * turns an directed edge
	 *
	 * @param model
	 *            the graphmodel where the edge is located
	 * @param e
	 *            the edge to turn
	 */
	private void turn(DefaultEdge e) {
		DefaultGraphCell target = (DefaultGraphCell) DefaultGraphModel
				.getTargetVertex(this.getModel(), e);
		DefaultGraphCell source = (DefaultGraphCell) DefaultGraphModel
				.getSourceVertex(this.getModel(), e);
		// Buggy
		// DefaultGraphModel.setSourcePort(model,e,target.getChildAt(0));
		// DefaultGraphModel.setTargetPort(model,e,source.getChildAt(0));
		ConnectionSet newCon = new ConnectionSet(e, target.getChildAt(0),
				source.getChildAt(0));
		this.getGraphLayoutCache().edit(null, newCon, null, null);
	}

	/**
	 * reapplies cycles to the graph
	 *
	 * @param model
	 *            the graph where the cycles should be reapplied
	 * @param cycEdg
	 *            the edges to turn around to reapply the cycles
	 */
	private void reCycle(Object[] cycEdg) {
		for (int i = 0; i < cycEdg.length; i++) {
			turn((DefaultEdge) cycEdg[i]);
		}
	}

	/**
	 * turns around cyclic edges to make a graph uncyclic
	 *
	 * @param model
	 *            the graph that shoud be made uncyclic
	 * @param tree
	 *            if the should be transformed into a tree
	 * @return the edges that were turned to make the graph uncyclic
	 */
	private Object[] unCycle(boolean tree) {
		DefaultGraphModel model = (DefaultGraphModel) this.getModel();
		// get all rootcells (vertexes, edges)
		Object[] cells = DefaultGraphModel.getRoots(model, DefaultGraphModel
				.getAll(model));
		// ////System.out.println(cells.length);

		// edges in the graph
		ArrayList<Object> edg = new ArrayList<Object>();
		// visited vertexes
		ArrayList<Object> visVert = new ArrayList<Object>();
		// visited edges
		ArrayList<Object> visEdg = new ArrayList<Object>();
		// path in the graph
		ArrayList<Object> visPath = new ArrayList<Object>();
		// turned edges
		ArrayList<Object> turnEdg = new ArrayList<Object>();

		DefaultGraphCell cell = null;

		for (int i = 0; i < cells.length; i++) {
			Object x = cells[i];
			// choose a vertex not an edge
			if (model.isEdge(x)) {
				// build list of edges
				edg.add(cells[i]);
			} else {
				if (cell == null) {
					// //System.out.println("Set as first cell: "
					// + ((DefaultGraphCell) x).getUserObject());
					cell = (DefaultGraphCell) x;
				}
				// ////System.out.println(((DefaultGraphCell)x).getUserObject());
//				Object[] e = DefaultGraphModel.getOutgoingEdges(model, x);
				// show all reachable vertexes
//				for (int j = 0; j < e.length; j++) {
//					DefaultEdge t = (DefaultEdge) e[j];
//					DefaultGraphCell target = (DefaultGraphCell) DefaultGraphModel
//							.getTargetVertex(model, t);
//					System.out.println("->" + target.getUserObject());
//				}
			}
		}
		if (cell != null) {
			visPath.add(cell);
			visVert.add(cell);
		}
		// //System.out.println("PDG#uncycle edg.size = " + edg.size());
		// as long as not all egdes are visited
		// quickfix: when there are unreachable nodes in the callgraph, this lopp will never terminate.
		// so we terminate when nothing changes anymore
		int lastSize = visEdg.size() + 1;
		while (visEdg.size() < edg.size() && lastSize != visEdg.size()) {
			lastSize = visEdg.size();
			// //System.out.println("current vertex : "
			// + ((DefaultGraphCell) cell).getUserObject());
			if (cell == null) {
				// //System.out.println("ERROR! incoherent graph");
				break;
			}
			DefaultEdge edge = null;
			// ausgehende Kanten beschaffen
			Object[] edges = DefaultGraphModel.getOutgoingEdges(model, cell);
			// looking for edge that hasn't been chosen
			for (int i = 0; i < edges.length; i++) {
				DefaultEdge tmp = (DefaultEdge) edges[i];
				if (!visEdg.contains(tmp)) {
					// //System.out.println("found edge to traverse");
					// set edge as new edge and add to visited edges
					edge = tmp;
					visEdg.add(tmp);
					break;
				}
			}
			if (edge == null) {
				// //System.out.println("No edge found tracking back");
				// backtrack, if there is no not visited edge
				if (visPath.size() >= 2) {
					// go back to last path element
					int last = visPath.size() - 1;
					visPath.remove(last);
					cell = (DefaultGraphCell) visPath.get(last - 1);
				}
			} else {
				// look for cycle at the edges destination
				DefaultGraphCell target = (DefaultGraphCell) DefaultGraphModel
						.getTargetVertex(model, edge);
				if (visPath.contains(target)) {
					// //System.out.println("cycle found, turning edge");
					// undo cycle
					if (tree) {
						// remove edge if it should convert to a tree
						Object[] remove = { edge };
						model.remove(remove);
					} else {
						turnEdg.add(edge);
						turn(edge);
					}
				} else {
					// //System.out.println("edgetarget: "
					// + ((DefaultGraphCell) target).getUserObject());
					// look if vertex is allready visited
					if (visVert.contains(target)) {
						// //System.out.println("vertex visited before");
						// remove edge if it should convert to a tree
						if (tree) {
							Object[] remove = { edge };
							model.remove(remove);
						}
					} else {
						// System.out
						// .println("vertex not visited yet switching to: "
						// + ((DefaultGraphCell) target)
						// .getUserObject());
						cell = target;
						visVert.add(target);
						visPath.add(target);
					}
				}

			}

		}
		return turnEdg.toArray();
	}

	/**
	 * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.AttributeMapAdjustmentsListener#attributeMapChanged(edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.AttributeMapAdjustmentsEvent)
	 */
	public void attributeMapChanged(AttributeMapAdjustmentsEvent event) {
		if (this.isVisible()) {
			this.repaint();
		}
	}

	/**
	 * PopupListener
	 */
	private class PopupListener extends MouseAdapter {

		/**
		 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseClicked(MouseEvent event) {
			if (event.getClickCount() == 2
					&& event.getButton() == MouseEvent.BUTTON1) {
				DefaultGraphCell cell = (DefaultGraphCell) getFirstCellForLocation(
						event.getX(), event.getY());

				if (cell == null) {
					//we double-clicked at an empty space
					return;
				}
				
				final Object clicked = cell.getUserObject();
				if (clicked instanceof SDGNode) {
					int proc = ((SDGNode) clicked).getProc();

					OpenMethodAction openMethodAction = (OpenMethodAction) actions.get(OpenMethodAction.class);
					openMethodAction.setProc(proc);
					openMethodAction.setGraph(call);
					openMethodAction.actionPerformed(null);
				}
//				CombiAction.setProc(proc);
			}
		}

		@Override
		public void mousePressed(MouseEvent event) {
			this.maybeShowPopup(event);
		}

		/**
		 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseReleased(MouseEvent event) {
			this.maybeShowPopup(event);
		}

		private void maybeShowPopup(MouseEvent event) {

			if (event.isPopupTrigger()) {
				DefaultGraphCell cell = (DefaultGraphCell) getFirstCellForLocation(
						event.getX(), event.getY());
				if (cell != null && cell.getUserObject() instanceof SDGNode) {
					popup
							.show(event.getComponent(), event.getX(), event
									.getY());
					OpenMethodAction action = (OpenMethodAction) actions
							.get(OpenMethodAction.class);
					action.setProc(((SDGNode) cell.getUserObject()).getProc());
					action.setGraph(call);

				} else {
					if (cell == null) {
						// open layoutmenu
						// event.getComponent() returns the PDG

					}
				}
			}
		}
	}
}
