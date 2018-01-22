/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)PDGViewFactory.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 14.12.2004 at 15:02:05
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view.pdg;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;

import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultCellViewFactory;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.RefreshViewEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.RefreshViewListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.util.Debug;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.Adjustable;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.VertexAdjustmentsPanel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVPanel;


/**
 * This class is in charge of creating views for the vertices of the graph. It
 * constructs a view for each vertex type and associates it with the specified
 * object.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>, <a
 *         href="mailto:westerhe@fmi.uni-passau.de">Marieke Westerheide </a>
 * @version 1.1
 */
public final class PDGViewFactory extends DefaultCellViewFactory implements
        BundleConstants, Adjustable {

    // to be able to hand over the current instance of this class
    private static PDGViewFactory instance = null;

    private final static String FILE_NAME = "preferences.txt";

    /**
     * Maps each vertex type to a colour chosen by the user. The user's
     * adjustments are saved in a file.
     */
    private static Properties colors;

    /**
     * Contains an <code>AdjustableAttributeMap</code> for each vertex type
     * which holds information about colour and other design parameters.
     */
    protected Hashtable<Kind, AttributeMap> maps = new Hashtable<Kind, AttributeMap>();

	/**
	 * The title for the adjustments panel.
	 */
	protected Resource vertexKey = null;

	protected Resource edgeKey = null;

	/**
	 * The panel where the user can choose a colour for each vertex type.
	 */
	private VertexAdjustmentsPanel adjustmentsPanel = null;

	/**
	 * The listener in charge of refreshing the already displayed tabs
	 */
	private RefreshViewListener refreshListener;

	/**
	 * Creates the <code>PDGViewFactory</code> instance. It reads out the file
	 * (in the user's home directory) with the user's preferences concerning
	 * node appearance (currently merely background colour) and checks the
	 * SDGNode class for new vertex types.
	 */
	private PDGViewFactory() {
		// super();
		colors = new Properties();
		try {
			File preferences = new File(System.getProperty("user.home") + "/"
					+ FILE_NAME);
			InputStream inStream;
			if (preferences.exists())
				inStream = new FileInputStream(preferences);
			else
//				inStream = ClassLoader.getSystemResourceAsStream(FILE_NAME);
				inStream = this.getClass().getClassLoader().getResourceAsStream(FILE_NAME);
			colors.load(inStream);
			inStream.close();
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		} catch (IOException f) {
			System.out.println(f.getMessage());
		}
		// SDGNode.Kind[] types = SDGNode.Kind.values();
		// for (int i = 0; i < types.length; i++) {
		// if (!colors.containsKey(types[i].toString())) {
		// colors.setProperty(types[i].toString(), "#FF33CC");
		// }
		// }
		vertexKey = new Resource(VERTICES_BUNDLE, "vertex.label");
		edgeKey = new Resource(VERTICES_BUNDLE, "edge.label");
	}

	/**
	 * @return the current instance of <code>PDGViewFactory</code>
	 */
	public static synchronized PDGViewFactory getInstance() {
		if (instance == null) {
			instance = new PDGViewFactory();
		}
		return instance;
	}

	/**
	 * Performs colour changes on nodes. Also saves user's adjustments in a file
	 * in the user's home directory on the hard disk.
	 *
	 * @see edu.kit.joana.ui.ifc.sdg.graphviewer.view.Adjustable#adjustmentPerformed(boolean)
	 * @param valuesChanged
	 *            <code>true</code> if any changes have been made,
	 *            <code>false</code> otherwise.
	 */
	@SuppressWarnings("unchecked")
	public void adjustmentPerformed(boolean valuesChanged) {
		if (valuesChanged) {
			if (this.adjustmentsPanel != null) {
				for (Enumeration<Kind> e = maps.keys(); e.hasMoreElements();) {
					SDGNode.Kind key = e.nextElement();
					PDGAttributeMap map = (PDGAttributeMap) maps.get(key);
					map.put(GraphConstants.BACKGROUND, adjustmentsPanel.getBackgroundColor(key.toString()));
					map.put(GraphConstants.FOREGROUND, adjustmentsPanel.getForegroundColor());
					map.fireAttributeMapChanged();
					colors.setProperty(key.toString(), "" + adjustmentsPanel.getBackgroundColor(key.toString()).getRGB());
				}

				Object[] edges = SDGEdge.Kind.values();
				for (int i = 0; i < edges.length; i++) {
					colors.setProperty(edges[i].toString() + "_COL",
							""	+ adjustmentsPanel.getEdgeColor(edges[i].toString()).getRGB());
				}

				// refresh displayed graphs
				fireRefreshViewEvent(new RefreshViewEvent(colors));
				// save colours in file
				try {
					FileOutputStream outStream = new FileOutputStream(System.getProperty("user.home") + "/" + FILE_NAME);
					colors.store(outStream, "Node types' background colors");
					outStream.close();
				} catch (FileNotFoundException e) {
					System.out.println(e.getMessage());
				} catch (IOException f) {
					System.out.println(f.getMessage());
				}
			}
		} else if (this.adjustmentsPanel != null) {
			this.adjustmentsPanel.restoreValues(maps);
		}
	}

	/**
	 * Provides the panel where the user can choose a colour for each vertex
	 * type.
	 *
	 * @see edu.kit.joana.ui.ifc.sdg.graphviewer.view.Adjustable#getAdjustmentDialog()
	 * @return a panel where the user can choose a colour for each vertex type
	 */
	public GVPanel getAdjustmentDialog() {
		if (this.adjustmentsPanel == null) {
			this.adjustmentsPanel = new VertexAdjustmentsPanel(colors, maps);
		}
		this.adjustmentsPanel.restoreValues(maps);
		return this.adjustmentsPanel;
	}

	/**
	 * @see edu.kit.joana.ui.ifc.sdg.graphviewer.view.Adjustable#getKeyResource()
	 * @return the title for the adjustments panel
	 */
	public Resource getKeyResource() {
		return vertexKey;
	}

	/**
	 * This method creates a view for each single cell to be drawn. If the cell
	 * is a vertex, it is added as a
	 * {@link edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.AttributeMapAdjustmentsListener AttributeMapAdjustmentsListener}
	 * to its type's attribute map. The view defines the appearance (colour,
	 * font, label etc.) of the cell. Appearance of edges and ports is JGraph
	 * default, the appearance of vertices is defined in
	 * <code>PDGVertexView</code>.
	 *
	 * @see org.jgraph.graph.CellViewFactory#createView(org.jgraph.graph.GraphModel,
	 *      java.lang.Object)
	 * @param model
	 * @param cell
	 *            the graph cell to be drawn
	 * @return a view of the cell
	 */
	@Override
	public CellView createView(GraphModel model, Object cell) {
		if (cell instanceof DefaultGraphCell) {
			Object userObject = ((DefaultGraphCell) cell).getUserObject();
			if (userObject instanceof SDGNode) {
				PDGVertexView view = null;
				SDGNode vertex = (SDGNode) userObject;
				PDGAttributeMap map = (PDGAttributeMap) getAttributeMap(vertex
						.getKind());
				view = new PDGVertexView(cell, map);
				map.addAttributeMapListener(view);
				return view;
			}
		}
		if(cell instanceof DefaultEdge){
			GraphConstants.setRouting(((DefaultEdge)cell).getAttributes(), new MyRouting());
		}
		return super.createView(model, cell);
	}

	/**
	 * Gets the attribute map of the node type handed over. The attribute maps
	 * are managed in a hashtable in the instance of this class. If there is no
	 * attribute map yet for this node type, a new one is created.
	 *
	 * @param kind
	 *            a node type
	 * @return the <code>AttributeMap</code>
	 */
	public AttributeMap getAttributeMap(SDGNode.Kind kind) {
		AttributeMap map = this.maps.get(kind);
		if (map == null) {
			// this happens once for each node type, namely when a graph is
			// opened that contains nodes of this type
			map = new PDGAttributeMap();
			if (kind != null && colors != null) {
				if (colors.getProperty(kind.toString()) == null) {
					Debug
							.print("should not happen ... PDGViewFactory#getAttributeMap");
					GraphConstants.setBackground(map, Color.BLUE);
				} else {
					// sets the background attribute in the specified map to the
					// value specified in the file mentioned above or to a
					// default value if the file contains no value
					GraphConstants.setBackground(map, Color.decode(colors
							.getProperty(kind.toString())));
				}
				GraphConstants.setForeground(map, Color.BLACK);
				GraphConstants.setBorder(map, BorderFactory
						.createCompoundBorder(BorderFactory
								.createBevelBorder(BevelBorder.RAISED),
								BorderFactory.createEmptyBorder(2, 2, 2, 2)));
			}
			this.maps.put(kind, map);
		}
		return map;
	}

	/**
	 * currently only one refreshListener is needed
	 * @param listener
	 */
	public void addRefreshViewListener(RefreshViewListener listener) {
		this.refreshListener = listener;
	}

	/**
	 * removes the listener
	 * @param listener
	 */
	public void removeRefreshViewLsitener(RefreshViewListener listener) {
		this.refreshListener = null;
	}

	/**
	 * causes the view of the currently displayed graphs to be refreshed
	 * @param event
	 */
	private void fireRefreshViewEvent(RefreshViewEvent event) {
		refreshListener.refreshView(event);
	}

}
