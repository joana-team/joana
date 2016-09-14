/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)GraphPane.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 27.11.2004 at 12:18:09
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jgraph.JGraph;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;
import org.jgraph.layout.JGraphLayoutAlgorithm;
import org.jgraph.layout.SugiyamaLayoutAlgorithm;
import org.jgraph.util.JGraphUtilities;
import org.jgrapht.ext.JGraphModelAdapter;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.CollapseCallNodeAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.OpenMethodAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.PredAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.SuccAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ZoomInAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ZoomOutAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.GraphViewerModelEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.GraphViewerModelListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.RefreshViewEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.RefreshViewListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.layout.PDGConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.layout.PDGLayoutAlgorithm;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.Graph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.MethodGraph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.util.GVUtilities;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVMenuItem;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVPopupMenu;

/**
 * A JTabbedPane where the different graphs can be displayed.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>, <a
 *         href="mailto:westerhe@fmi.uni-passau.de">Marieke Westerheide </a>,
 *         Siegfried Weber
 * @version 1.1
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class GraphPane extends JTabbedPane implements GraphViewerModelListener, RefreshViewListener {
	private static final long serialVersionUID = 7806224084215051672L;
	private static final Color TRANSPARENT = new Color(0, true);

	/**
	 * The JFrame the user is working with.
	 */
	protected MainFrame owner = null;

	protected AdjustmentsDialog adjustmentsDialog;

	protected int centerID = -1;

	protected SearchDialog searchDialog;
	protected LookupDialog lookupDialog;
	protected HideNodeDialog hideNodeDialog;
	private Timer timer;

	/**
	 * appears on right click
	 */
	private GVPopupMenu popup;

	/**
	 * Constructs a new <code>GraphPane</code> object.
	 *
	 * @param lookupDialog
	 * @param hideNodeDialog
	 */
	public GraphPane(MainFrame owner, AdjustmentsDialog adjustmentsDialog,
			SearchDialog searchDialog, LookupDialog lookupDialog, HideNodeDialog hideNodeDialog) {
		super();
		this.owner = owner;
		this.adjustmentsDialog = adjustmentsDialog;
		this.searchDialog = searchDialog;
		this.lookupDialog = lookupDialog;
		this.hideNodeDialog=hideNodeDialog;
		this.addContainerListener(this.owner);
	}

	public void initPopups() {
		popup = new GVPopupMenu(this.owner.getTranslator());
		popup.add(this.owner.getActions().get(PredAction.class));
		popup.add(this.owner.getActions().get(SuccAction.class));
		popup.add(this.owner.getActions().get(CollapseCallNodeAction.class));
	}

	/**
	 * Gets the graph that is displayed in the currently selected pane.
	 *
	 * @return the selected graph
	 */
	public Graph getSelectedGraph() {
		int selected = this.getSelectedIndex();
		if (selected < 0) {
			selected = 0;
		}

		return  ((GraphPaneTab) this.getComponentAt(selected)).getGraph();
	}

	public JGraph getSelectedJGraph() {
		int selected = this.getSelectedIndex();
		if (selected < 0) {
			selected = 0;
		}

		return (JGraph) ((GraphPaneTab) this.getComponentAt(selected)).getViewport().getComponent(0);
	}

	/**
	 * Lays the currently selected graph out according to the indicated layout
	 * algorithm.
	 *
	 * @param layout
	 *            the graph layout algorithm to be executed on the graph
	 */
	public void applyLayout(JGraphLayoutAlgorithm layout) {
		JGraphUtilities.applyLayout(getSelectedJGraph(), layout);
	}

	/**
	 * Causes the graph to be displayed and laid out or to be closed by adding
	 * or removing the respective JScrollPane.
	 *
	 * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.GraphViewerModelListener#graphViewerModelChanged(edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.GraphViewerModelEvent)
	 * @param event
	 *            the event that was fired
	 */
	public void graphViewerModelChanged(GraphViewerModelEvent event) {
		switch (event.getId()) {
		case GraphViewerModelEvent.CALL_GRAPH_ADDED:
			// create CallGraphView: see description on jgrapht.sourceforge.net: JGraphT -> JGraph
			Graph callGraph = event.getGraph();
			JGraphModelAdapter pdgAadapter = new JGraphModelAdapter(callGraph.getSDG());
			CallGraphView v = new CallGraphView(callGraph.getName(), pdgAadapter, owner.getTranslator(), owner.getActions(), callGraph);
			v.addPropertyChangeListener(JGraph.SCALE_PROPERTY, (ZoomInAction) this.owner.getActions().get(ZoomInAction.class));
			v.addPropertyChangeListener(JGraph.SCALE_PROPERTY, (ZoomOutAction) this.owner.getActions().get(ZoomOutAction.class));

			// layout and draw the call graph
			GraphPaneTab scrollPane = new GraphPaneTab(this, v, callGraph);
			addTabVersion(callGraph.getName(), "PDG.png", scrollPane);
			v.applyLayout(new SugiyamaLayoutAlgorithm());
			setSelectedComponent(scrollPane);
			break;

		case GraphViewerModelEvent.METHOD_GRAPH_ADDED:
			MethodGraph methodGraph = (MethodGraph) event.getGraph();
			addMethodGraph(methodGraph);
			break;

		case GraphViewerModelEvent.ALL_GRAPHS_REMOVED:
			removeAll();
			break;
		}
	}

	private void addMethodGraph(final MethodGraph methodGraph) {
		final MethodGraphView component = MethodGraphView.createMethodGraphView(methodGraph);
		final int procID = methodGraph.getProcID();
		component.addMouseListener(new MouseAdapter() {

			// add link to another method graph
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					DefaultGraphCell cell = (DefaultGraphCell) component
							.getFirstCellForLocation(e.getX(), e.getY());
					if (cell != null) {
						((PredAction) ((GVMenuItem) popup.getComponent(0))
								.getAction()).setLocation(e.getX(), e.getY());
						((SuccAction) ((GVMenuItem) popup.getComponent(1))
								.getAction()).setLocation(e.getX(), e.getY());
						popup.show(e.getComponent(), e.getX(), e.getY());

					}
				} else {
					if (e.getClickCount() == 2) {
						DefaultGraphCell cell = (DefaultGraphCell) component
								.getFirstCellForLocation(e.getX(), e.getY());
						if (cell != null) {
							int proc = PDGConstants.getProc(cell
									.getAttributes());
							if (proc >= 0 && proc != procID) {
								centerID = PDGConstants.getID(cell
										.getAttributes());
								ActionMap actions = owner.getActions();
								OpenMethodAction action = (OpenMethodAction) actions
										.get(OpenMethodAction.class);
								action.setProc(proc);
								action.setGraph(methodGraph);
								action.actionPerformed(null);
							}
						}
					}
					resetBorders();
				}
			}
		});
		layoutGraph(component, methodGraph);
		// draw/hide edges as in default setting.
		methodGraph.changed();
	}

	private void layoutGraph(final JGraph graph, final MethodGraph mg) {
		final PDGLayoutAlgorithm layoutAlgorithm = new PDGLayoutAlgorithm();

		// set up progress monitor
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				final ProgressMonitor progress = new ProgressMonitor(owner,
						"layout graph...", "", 0, 100);
				progress.setMillisToDecideToPopup(0);
				progress.setMillisToPopup(0);
				timer = new Timer(100, new ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent e) {
						int percent = layoutAlgorithm.getProgress();
						progress.setProgress(percent);
						if (percent >= 100) {
							timer.stop();
							progress.close();
						}
					}
				});
				timer.start();
			}
		});

		final GraphPane pane = this;
		// start layout
//		new Thread() {
		{
			final GraphPaneTab scrollPane = new GraphPaneTab(pane, graph, mg);
//			@Override
//			public void run() {
				JGraphUtilities.applyLayout(graph, layoutAlgorithm);
				GraphModel model = graph.getModel();
				CellView[] cellviews = graph.getGraphLayoutCache()
						.getCellViews();

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
					if (idid == centerID) {
						rect = c.getBounds();

						if (rect != null) {
							Point p = new Point();
							p.setLocation(rect.getCenterX(), rect.getCenterY());
							Dimension d = getCenterPoint();
							graph.setLocation(
									(int) (d.width / 2 - p.getX()),
									(int) (d.height / 2 - p.getY()));
						}
					}
				}

				// add tab to GraphPane
				addTabVersion(mg.getName(), "MethodGraph.png", scrollPane);

				setSelectedComponent(scrollPane);
		}
	//		}
//		}.start();
	}

	public int getCenterID() {
		return centerID;
	}

	public void setCenterID(int centerID) {
		this.centerID = centerID;
	}

	/**
	 * implements a close button functionality for the tabs that is supported
	 * from java version 6 on
	 *
	 * @param graphName
	 *            name of the graph to be displayed
	 * @param iconName
	 *            either MethodGraph or PDG
	 * @param scrollPane
	 *            scroll pane to be added
	 */
	public void addTabVersion(final String graphName, String iconName,
			JScrollPane scrollPane) {

		addTab(graphName, GVUtilities.getIcon(iconName), scrollPane);

		/**
		 * from java 6 on the following code can be used instead of the line
		 * above so that a close button will be displayed on each tab
		 */

		/*
		 * JPanel tabTitle = new JPanel(new BorderLayout());
		 * tabTitle.setOpaque(false); ImageIcon closeIcon =
		 * GVUtilities.getIcon("Close.png"); JButton closeButton = new
		 * JButton(closeIcon); JLabel title = new JLabel(" " + graphName + " ");
		 * closeButton
		 * .addActionListener(this.owner.getActions().get(CloseAction.class));
		 * closeButton.setPreferredSize(new Dimension(13,13)); tabTitle.add(new
		 * JLabel(GVUtilities.getIcon(iconName)), BorderLayout.WEST);
		 * tabTitle.add(title, BorderLayout.CENTER); tabTitle.add(closeButton,
		 * BorderLayout.EAST); this.addTab(null, scrollPane);
		 * this.setTabComponentAt(this.getTabCount()-1, tabTitle);
		 * this.setTitleAt(this.getTabCount()-1, graphName);
		 */
	}

	/**
	 * refreshes the currently displayed graphs by iterating through all tabs of
	 * the graph pane
	 *
	 * @param event
	 *            contains color information
	 */
	public void refreshView(RefreshViewEvent event) {
		final Properties colors = readColors();//(Properties) event.getSource();
		final Font font = new Font("Dialog", Font.PLAIN, 9);

		boolean unknownEdgeColorError = false;

		// iterate through all tabs of the graph pane
		for (int i = 0; i < this.getTabCount(); i++) {
			final JGraph currGraph = (JGraph) ((JScrollPane) this.getComponentAt(i)).getViewport().getComponent(0);

			if (currGraph instanceof CallGraphView) continue;

			final GraphLayoutCache view = currGraph.getGraphLayoutCache();
			final GraphModel model = currGraph.getModel();
			final Object[] cells = DefaultGraphModel.getAll(model);
			final Map<DefaultGraphCell, Map<String, String>> nested = new Hashtable<DefaultGraphCell, Map<String, String>>();
			// go through all cells of each graph; only vertices and edges
			// are relevant
			for (Object o : cells) {
				if (!model.isPort(o)) {
					final DefaultGraphCell cell = (DefaultGraphCell) o;
					boolean edgeNull = false;
					final Map<String, String> attributeMap = new Hashtable<String, String>();

					if (model.isEdge(cell)) {
						// cell is edge
						Color color = Color.BLACK;

						if ((cell.getAttributes()).get("kind") != null) {
							final String key = "" + ((String) (cell.getAttributes()).get("kind"));
							String colString = colors.getProperty(key + "_COL");

							if (colString == null) {
								// fallback
								if (!unknownEdgeColorError) {
									System.err.println("No color for edge " + key + " defined. Defaulting to black...");
									unknownEdgeColorError = true;
								}

								colString = "0";
							}
							// decode colors according to color format
							if (colString.charAt(0) == '#') {
								color = Color.decode(colString);
							} else {
								final int colInt = (new Integer(colString)).intValue();
								color = new Color(colInt);
							}
						} else {
							edgeNull = true;
						}
						// store new color in attribute Map for each cell
						GraphConstants.setLineColor(attributeMap, color);
					} else {
						// cell is vertex
						Color color = Color.WHITE;

						// ignore cells withouth a value for kind
						if ((cell.getAttributes()).get("kind") != null) {
							final String key = "" + ((String) (cell.getAttributes()).get("kind"));
							final String colString = colors.getProperty(key);
							// decode colors according to color format
							if (colString.charAt(0) == '#') {
								color = Color.decode(colString);
							} else {
								final int colInt = (new Integer(colString)).intValue();
								color = new Color(colInt);
							}
						}
						// store new color in attribute Map for each cell
						GraphConstants.setBackground(attributeMap, color);
					}
					// store cell-attributeMap - pair in nested hashtable
					if (!edgeNull) {
						nested.put(cell, attributeMap);
					}
				}
			}

			// hide edges
			final Graph graph = ((GraphPaneTab) this.getComponentAt(i)).getGraph();
			final Set<String> toHideStr = graph.getEdgesToHideAsStrings();
			for (Object o : cells) {
				if (!model.isPort(o)) {
					final DefaultGraphCell cell = (DefaultGraphCell) o;

					if (model.isEdge(cell)) {
						final String key = "" + ((String) (cell.getAttributes()).get("kind"));
						final Map<String, String> attributeMap = getEdgeAttributes(cell, font, colors);

						if (toHideStr.contains(key)) {
//							view.setVisible(cell, false);
							GraphConstants.setLineColor(attributeMap, TRANSPARENT);
							GraphConstants.setForeground(attributeMap, TRANSPARENT);
							GraphConstants.setSelectable(attributeMap, false);
						} else {
							GraphConstants.setForeground(attributeMap, Color.GRAY);
							GraphConstants.setSelectable(attributeMap, true);
//							view.setVisible(cell, true);
						}

						nested.put(cell, attributeMap);
					}
				}
			}

			// make changes
			view.edit(nested);
		}
	}

	private void resetBorders() {
		final JGraph graph = getSelectedJGraph();
		final GraphLayoutCache view = graph.getGraphLayoutCache();
		final GraphModel model = graph.getModel();
		final Object[] cells = DefaultGraphModel.getAll(model);
		final Map<DefaultGraphCell, Map> nested = new Hashtable<DefaultGraphCell, Map>();

		// go through all cells of each graph; only vertices are relevant
		for (Object o : cells) {
			if (!model.isPort(o)) {
				final DefaultGraphCell cell = (DefaultGraphCell) o;
				final Map map = new Hashtable();

				if (!model.isEdge(cell)) {
					// cell is vertex
					GraphConstants.setBorder(map, BorderFactory.createLineBorder(Color.BLACK));
				}
				// store cell-attributeMap - pair in nested hashtable
				nested.put(cell, map);
			}
		}
		// make changes
		view.edit(nested);
	}

	public Dimension getCenterPoint() {
		return this.getSize();
	}

	@SuppressWarnings("resource")
	private Properties readColors() {
		final Properties colors = new Properties();

		try {
			final String filename = "preferences.txt";
			final File preferences = new File(System.getProperty("user.home") + "/" + filename);

			final InputStream inStream;
			if (preferences.exists()) {
				inStream = new FileInputStream(preferences);
			} else {
//				inStream = ClassLoader.getSystemResourceAsStream(filename);
				inStream = this.getClass().getClassLoader().getResourceAsStream(filename);
			}

			colors.load(inStream);
			inStream.close();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		return colors;
	}

	private Map<String, String> getEdgeAttributes(DefaultGraphCell cell, Font font, Properties colors) {
		final Map<String, String> attributeMap = new Hashtable<String, String>();

		final SDGEdge sdgEdge = (SDGEdge) cell.getUserObject();
		final String kind = sdgEdge.getKind().toString();
		PDGConstants.setKind(attributeMap, kind);

		GraphConstants.setEndSize(attributeMap, 6);
		GraphConstants.setFont(attributeMap, font);

		Color color = Color.decode(colors.getProperty(kind.toString() + "_COL",	"#000000"));
		GraphConstants.setLineColor(attributeMap, color);

		final String pattern = colors.getProperty(kind.toString() + "_PAT", "0");
		if ("1".equals(pattern)) {
			GraphConstants.setDashPattern(attributeMap, new float[] { 5f });
		} else if ("2".equals(pattern)) {
			GraphConstants.setDashPattern(attributeMap, new float[] { 1f, 3f });
		}

		return attributeMap;
	}
}
