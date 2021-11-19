/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ui.ifc.sdg.graphviewer.layout.PDGConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.ClonedSDGNode;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.MethodGraph;
import org.jgraph.JGraph;
import org.jgraph.graph.*;
import org.jgrapht.ext.JGraphModelAdapter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class MethodGraphView extends JGraph {
	private static final long serialVersionUID = -3048446505658986252L;
	static int ctr = -1;

	private MethodGraphView(GraphModel model) {
		super(model);
	}

	public String getToolTipText(java.awt.event.MouseEvent e) {
		GraphCell firstCell = (GraphCell) getFirstCellForLocation(e
				.getX(), e.getY());

		if (firstCell != null) {
			String toolTip = PDGConstants.getToolTip(firstCell
					.getAttributes());
			GraphCell cell = firstCell;
			boolean allVisited = false;
			while (toolTip == null && !allVisited) {
				cell = (GraphCell) getNextCellForLocation(cell, e
						.getX(), e.getY());
				if (cell == firstCell)
					allVisited = true;
				toolTip = PDGConstants.getToolTip(cell.getAttributes());
			}
			if (toolTip != null)
				return toolTip;
		}
		return super.getToolTipText(e);
	}


	/* FACTORY */

	/**
	 * Factory
	 */
	public static MethodGraphView createMethodGraphView(MethodGraph mg) {
		JGraphModelAdapter<SDGNode, SDGEdge> model = new JGraphModelAdapter<SDGNode, SDGEdge>(mg.getSDG());
		MethodGraphView view = new MethodGraphView(model);
		cloneLinkNodes(view, mg.getProcID());
		formatCells(view, mg);

		// add MethodGraphView to ToolTipManager
		ToolTipManager toolTipMgr = ToolTipManager.sharedInstance();
		toolTipMgr.registerComponent(view);
		toolTipMgr.setInitialDelay(0);

		// set MethodGraphView behaviour
		view.setCloneable(false);
		view.setConnectable(false);
		view.setDisconnectable(false);
		view.setGridEnabled(false);
		view.setTolerance(1);
		view.setName(mg.getName());

		return view;
	}


	/**
	 * Clones link nodes so that the nodes have not to share the link nodes.
	 *
	 * @param component
	 *            the JGraph component
	 */
	private static void cloneLinkNodes(MethodGraphView component, int procID) {
		GraphModel model = component.getModel();
		GraphLayoutCache view = component.getGraphLayoutCache();
		view.setSelectsAllInsertedCells(false);
		ConnectionSet conn = new ConnectionSet();
		List<DefaultGraphCell> clones = new LinkedList<DefaultGraphCell>();
		Object[] cells = DefaultGraphModel.getAll(model);

		for (Object o : cells) {
			if (!model.isEdge(o) && !model.isPort(o)) {
				DefaultGraphCell cell = (DefaultGraphCell) o;
				SDGNode sdgNode = (SDGNode) cell.getUserObject();
				if (sdgNode.getProc() != procID) {
					boolean origCellInUse = cloneLinkNode(cell, model, true, false, sdgNode, clones, conn);
					cloneLinkNode(cell, model, false, origCellInUse, sdgNode, clones, conn);
				}
			}
		}

		view.insert(clones.toArray(), null, conn, null);
	}


	/**
	 * Clones a link node.
	 *
	 * @param cell
	 *            the link node as JGraph cell
	 * @param model
	 *            the JGraph model
	 * @param incoming
	 *            true if the edge targets to the link node
	 * @param origCellInUse
	 *            true if the link node is already linked to a node
	 * @param sdgNode
	 *            the link node
	 * @param clones
	 *            the clones of the link node
	 * @param conn
	 *            a set of connections from edges to ports
	 * @return true if the first link node is already in use
	 */
	private static boolean cloneLinkNode(Object cell, GraphModel model,
			boolean incoming, boolean origCellInUse, SDGNode sdgNode,
			List<DefaultGraphCell> clones, ConnectionSet conn) {

		Object[] edges;
		if (incoming) {
			edges = DefaultGraphModel.getIncomingEdges(model, cell);

		} else {
			edges = DefaultGraphModel.getOutgoingEdges(model, cell);
		}

		for (Object edge : edges) {
			if (origCellInUse) {
				ClonedSDGNode sdgCopy = new ClonedSDGNode(sdgNode, ctr);
				ctr--;
				DefaultGraphCell clone = new DefaultGraphCell(sdgCopy);
				DefaultPort port = new DefaultPort();
				clone.add(port);
				conn.connect(edge, port, !incoming);
				clones.add(clone);

			} else {
				origCellInUse = true;
			}
		}

		return origCellInUse;
	}



	/**
	 * Formats all vertices and edges in the specified JGraph component.
	 *
	 * @param component
	 *            a JGraph component
	 */
	private static void formatCells(MethodGraphView component, MethodGraph mg) {
		GraphModel model = component.getModel();
		GraphLayoutCache view = component.getGraphLayoutCache();

		// set font
		Font font = new Font("Dialog", Font.PLAIN, 9);
		Properties colors = readColors();
		Object[] cells = DefaultGraphModel.getAll(model);
		Map nested = new Hashtable();
		for (Object o : cells) {
			if (!model.isPort(o)) {
				DefaultGraphCell cell = (DefaultGraphCell) o;
				Map attributeMap;
				if (model.isEdge(cell)) {
					// cell is edge
					attributeMap = getEdgeAttributes(cell, font, colors);
				} else {
					// cell is vertex
					attributeMap = getVertexAttributes(cell, font, colors, mg);
				}
				GraphConstants.setEditable(attributeMap, false);
				nested.put(cell, attributeMap);
			}
		}
		view.edit(nested);
	}

	/**
	 * Returns an attribute map with set edge attributes for line colors and
	 * font.
	 *
	 * @param cell
	 *            an edge
	 * @param font
	 *            a font
	 * @param colors
	 *            color properties
	 * @return an attribute map
	 */
	private static Map getEdgeAttributes(DefaultGraphCell cell, Font font,
			Properties colors) {
		Map attributeMap = new Hashtable();
		SDGEdge sdgEdge = (SDGEdge) cell.getUserObject();
		String kind = sdgEdge.getKind().toString();
		PDGConstants.setKind(attributeMap, kind);
		GraphConstants.setEndSize(attributeMap, 6);
		GraphConstants.setFont(attributeMap, font);
		Color color = Color.decode(colors.getProperty(kind.toString() + "_COL",
				"#000000"));
		GraphConstants.setLineColor(attributeMap, color);
		String pattern = colors.getProperty(kind.toString() + "_PAT", "0");
		if ("1".equals(pattern)) {
			GraphConstants.setDashPattern(attributeMap, new float[] { 5f });
		} else if ("2".equals(pattern)) {
			GraphConstants.setDashPattern(attributeMap, new float[] { 1f, 3f });
		}
		return attributeMap;
	}

	/**
	 * Returns an attribute map with set vertex attributes.
	 *
	 * @param cell
	 *            the vertex
	 * @param font
	 *            a font
	 * @param colors
	 *            color properties
	 * @return an attribute map
	 */
	private static Map getVertexAttributes(DefaultGraphCell cell, Font font, Properties colors, MethodGraph mg) {
		final Map attributeMap = new Hashtable<String, Object>();
		final SDGNode sdgNode;
		final Object userObject = cell.getUserObject();

		if (userObject instanceof SDGNode) {
			sdgNode = (SDGNode) userObject;
		} else {
			sdgNode = ((SDGNode[]) userObject)[0];
		}

		final String kind = sdgNode.getKind().toString();
		PDGConstants.setKind(attributeMap, kind);
		final int cellProc = sdgNode.getProc();
		PDGConstants.setProc(attributeMap, cellProc);

		// Ein ziemlicher Hack...
		final int id = Integer.parseInt(sdgNode.toString());
		// int id = sdgNode.getId();
		// ...Hack vorbei
		PDGConstants.setID(attributeMap, id);

		String value = escapeHTML(id + " " + sdgNode.getOperation().toString());
		final String label = sdgNode.getLabel();
		if (label != null) {
			value += "<br>" + escapeHTML(label);
		}

		if (cellProc == mg.getProcID()) {
			formatMethodNode(attributeMap, value);
		} else {
			final String methodTitle = escapeHTML(mg.getTitle(cellProc));
			formatLinkNode(attributeMap, value, methodTitle);
		}
		// set common attributes
		Color color = Color.decode(colors.getProperty(kind, "#FFFFFF"));
		GraphConstants.setBackground(attributeMap, color);
		GraphConstants.setForeground(attributeMap, Color.BLACK);
		GraphConstants.setFont(attributeMap, font);
		GraphConstants.setBorder(attributeMap, BorderFactory.createLineBorder(Color.BLACK));
		GraphConstants.setOpaque(attributeMap, true);
		return attributeMap;
	}



	/**
	 * Formats a node with the specified attributes and text.
	 *
	 * @param attributeMap
	 *            an attribute map
	 * @param value
	 *            a text
	 */
	private static void formatMethodNode(Map attributeMap, String value) {
		GraphConstants.setValue(attributeMap, "<html>" + value + "</html>");
		GraphConstants.setBounds(attributeMap, new Rectangle(new Point(0, 0)));
		GraphConstants.setAutoSize(attributeMap, true);
	}

	/**
	 * Formats a link node with the specified method name and text.
	 *
	 * @param attributeMap
	 *            an attribute map
	 * @param value
	 *            a text
	 * @param methodTitle
	 *            a method name
	 */
	private static void formatLinkNode(Map attributeMap, String value, String methodTitle) {
		GraphConstants.setValue(attributeMap, "");
		GraphConstants.setBounds(attributeMap, new Rectangle(0, 0, 8, 8));
		GraphConstants.setSizeable(attributeMap, false);
		PDGConstants.setLinkNode(attributeMap, true);
		String toolTip = "<html><b>" + methodTitle + "</b><br>" + value	+ "</html>";
		PDGConstants.setToolTip(attributeMap, toolTip);
	}

	/**
	 * Reads the color properties from a file. This is redundant to
	 * PDGViewFactory.
	 *
	 * @return the color properties
	 */
	private static Properties readColors() {
		Properties colors = new Properties();
		try {
			String filename = "preferences.txt";
			File preferences = new File(System.getProperty("user.home") + "/"
					+ filename);
			InputStream inStream;
			if (preferences.exists()) {
				inStream = new FileInputStream(preferences);
			} else {
				inStream = ClassLoader.getSystemResourceAsStream(filename);
			}
			colors.load(inStream);
			inStream.close();
		} catch (Exception e) {
		}
		return colors;
	}

	/**
	 * Escapes special characters in HTML.
	 *
	 * @param in
	 *            the string to escape
	 * @return the escaped string
	 */
	private static String escapeHTML(String in) {
		if (in == null) {
			System.err.println("hey");
		}
		String out = in.replace("&", "&amp;");
		out = out.replace("<", "&lt;");
		return out.replace(">", "&gt;");
	}
}
