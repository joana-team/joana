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

import java.awt.Color;
import java.awt.Font;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ui.ifc.sdg.graphviewer.layout.PDGConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;


import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.swing.JScrollPane;

import org.jgraph.JGraph;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;


public class HideEdgesAction extends AbstractGVAction implements BundleConstants {
	private static final long serialVersionUID = -6254557463267167011L;
	private boolean show = true;
	private GraphPane pane = null;

	/**
	 * Constructs a new <code>HideEdgesAction</code> object.
	 */
	public HideEdgesAction(GraphPane pane) {
		super("hide.name", "Checkmark.png", "hide.description", "hide");
		this.pane = pane;
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event) {
		show = !show;
		if (!show) {
			otherEdgeHide();
		} else {
			resetOtherEdge();
		}

	}

	private void otherEdgeHide() {
		GraphLayoutCache view;
		Properties colors = readColors();
		Font font = new Font("Dialog", Font.PLAIN, 9);
		int i = pane.getTabCount() - 1;
		JGraph currGraph = (JGraph) ((JScrollPane) pane.getComponentAt(i))
				.getViewport().getComponent(0);
		view = currGraph.getGraphLayoutCache();
		GraphModel model = currGraph.getModel();
		Object[] cells = DefaultGraphModel.getAll(model);
		Map<DefaultGraphCell, Map<String, String>> nested = new Hashtable<DefaultGraphCell, Map<String, String>>();
		Map<String, String> attributeMap = new Hashtable<String, String>();
		for (Object o : cells) {
			if (!model.isPort(o)) {
				DefaultGraphCell cell = (DefaultGraphCell) o;

				String key = "" + ((String) (cell.getAttributes()).get("kind"));
				if (model.isEdge(cell)) {
					attributeMap = getEdgeAttributes(cell, font, colors);

				}
				if (model.isEdge(cell)
						&& !key.equals(SDGEdge.Kind.CONTROL_FLOW.toString())) {
					GraphConstants.setLineColor(attributeMap, Color.WHITE);
				}
				nested.put(cell, attributeMap);

			}

		}
		view.edit(nested);

	}

	private void resetOtherEdge() {
		int i = pane.getTabCount() - 1;
		JGraph currGraph = (JGraph) ((JScrollPane) pane.getComponentAt(i))
				.getViewport().getComponent(0);
		GraphLayoutCache view = currGraph.getGraphLayoutCache();
		GraphModel model = currGraph.getModel();
		Properties colors = readColors();
		Font font = new Font("Dialog", Font.PLAIN, 9);
		Object[] cells = DefaultGraphModel.getAll(model);
		Map<DefaultGraphCell, Map<String, String>> nested = new Hashtable<DefaultGraphCell, Map<String, String>>();
		Map<String, String> attributeMap = new Hashtable<String, String>();
		for (Object o : cells) {

			if (!model.isPort(o)) {
				DefaultGraphCell cell = (DefaultGraphCell) o;

				String key = "" + ((String) (cell.getAttributes()).get("kind"));
				if (model.isEdge(cell)) {
					attributeMap = getEdgeAttributes(cell, font, colors);

				}
				if (model.isEdge(cell)
						&& key.equals(SDGEdge.Kind.CONTROL_FLOW.toString())) {
					GraphConstants.setLineColor(attributeMap, Color.WHITE);
				}
				nested.put(cell, attributeMap);
			}
		}
		view.edit(nested);

	}

	private Map<String, String> getEdgeAttributes(DefaultGraphCell cell, Font font, Properties colors) {
		Map<String, String> attributeMap = new Hashtable<String, String>();
		SDGEdge sdgEdge = (SDGEdge) cell.getUserObject();
		String kind = sdgEdge.getKind().toString();
		PDGConstants.setKind(attributeMap, kind);
		GraphConstants.setEndSize(attributeMap, 6);
		GraphConstants.setFont(attributeMap, font);
		Color color = Color.decode(colors.getProperty(kind.toString() + "_COL", "#000000"));
		GraphConstants.setLineColor(attributeMap, color);
		String pattern = colors.getProperty(kind.toString() + "_PAT", "0");
		if ("1".equals(pattern)) {
			GraphConstants.setDashPattern(attributeMap, new float[] { 5f });
		} else if ("2".equals(pattern)) {
			GraphConstants.setDashPattern(attributeMap, new float[] { 1f, 3f });
		}
		return attributeMap;
	}

	private Properties readColors() {
		Properties colors = new Properties();
		try {
			String filename = "preferences.txt";
			File preferences = new File(System.getProperty("user.home") + "/" + filename);
			InputStream inStream;
			if (preferences.exists())
				inStream = new FileInputStream(preferences);
			else
//				inStream = ClassLoader.getSystemResourceAsStream(filename);
				inStream = this.getClass().getClassLoader().getResourceAsStream(filename);
			colors.load(inStream);
		} catch (Exception e) { }
		return colors;
	}
}
