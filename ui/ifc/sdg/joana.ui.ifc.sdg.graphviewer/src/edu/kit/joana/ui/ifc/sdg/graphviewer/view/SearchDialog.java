/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphModel;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.PredAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.SuccAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.util.VertexNode;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVButton;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVDialog;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVFrame;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVPanel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVRadioButton;

@SuppressWarnings("rawtypes")
public class SearchDialog extends GVPanel implements BundleConstants, ActionListener {
	private static final long serialVersionUID = 2920892978395866486L;

	public static final int ERROR_OPTION = -1;

	public static final int CANCEL_OPTION = 2;

	private int returnValue = ERROR_OPTION;

	protected GVFrame parent = null;

	protected GVDialog dialog = null;

	/**
	 * contains the panel that holds all the GUI elements for a search
	 */
	private GVPanel searchPanel = null;

	/**
	 * this panel contains a representation of the search results
	 */
	private GVPanel resultPanel = null;

	private JScrollPane resultScrollPane = null;

	private boolean hidden = true;

	private final Dimension SHOW_SIZE = new Dimension(400, 600);

	private final Dimension HIDE_SIZE = new Dimension(400, 176);

	private GridBagConstraints c = null;

	private GVButton hideButton = null;

	//private String hideButtonText = null;

	private String hideText = null;

	private String showText = null;

	private GVPanel viewportPanel = null;

	private ButtonGroup buttonGroup;

	private final int VERTEX_SEARCH = 0;

	private final int TEXT_SEARCH = 1;

	private final int REG_EXPR_SEARCH = 2;

	private int resultCount = 0;

	private JPanel[] fill = new JPanel[4];

	private GraphPane graphPane = null;

	private LinkedList<VertexNode> results = null;

	private JTextField searchField = null;

	public List predList = null;

	public List succList = null;

	private String APPLY = "APPLY";

	public DefaultGraphCell currGraphCell = null;

	public SearchDialog(MainFrame parent) {
		super(parent.getTranslator());
		showText = "Ergebnis zeigen";
		hideText = "Ergebnis verbergen";
		this.parent = parent;
		this.initComponents();

	}

	public int showSearchDialog() {
		this.dialog = new GVDialog(this.getTranslator(), this.parent,
				new Resource(MAIN_FRAME_BUNDLE, "search.dialog.title"), true);
		this.dialog.setComponentOrientation(this.getComponentOrientation());
		this.dialog.getContentPane().setLayout(new BorderLayout());
		this.dialog.getContentPane().add(this, BorderLayout.CENTER);
		this.dialog.setSize(HIDE_SIZE);
		this.viewportPanel.removeAll();
		this.dialog.setLocationRelativeTo(this.parent);
		this.dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				returnValue = CANCEL_OPTION;
			}
		});
		this.returnValue = ERROR_OPTION;
		this.dialog.setVisible(true);
		this.dialog.dispose();
		this.dialog = null;
		this.parent.setEnabled(true);
		this.dialog.setResizable(false);
		return this.returnValue;
	}

	/**
	 * initialize and set up the GUI components
	 */
	private void initComponents() {
		// overall layout is grid bag
		this.setLayout(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;

		// main panels
		searchPanel = new GVPanel(this.getTranslator(), new BorderLayout());
		resultPanel = new GVPanel(this.getTranslator(), new GridLayout(0, 1));

		// initialize north of searchPanel
		GVPanel searchPanelNorth = new GVPanel(this.getTranslator(),
				new FlowLayout());
		GVButton searchButton = new GVButton(this.getTranslator(),
				new Resource(SEARCH_DIALOG_BUNDLE, "search.button.label"));
		searchButton.addActionListener(this);
		searchField = new JTextField();
		searchField.setPreferredSize(new Dimension(200, 20));
		searchPanelNorth.add(searchButton);
		searchPanelNorth.add(searchField);
		searchPanel.add(searchPanelNorth, BorderLayout.NORTH);

		// initialize south of searchPanel
		GVPanel searchPanelSouth = new GVPanel(this.getTranslator(),
				new FlowLayout());
		hideButton = new GVButton(this.getTranslator(), new Resource(
				SEARCH_DIALOG_BUNDLE, "hide.button.label.show"));
		hideButton.addActionListener(this);
		searchPanelSouth.add(hideButton);
		GVButton applyButton = new GVButton(this.getTranslator(), new Resource(
				SEARCH_DIALOG_BUNDLE, "apply.button.label"));
		applyButton.setActionCommand(this.APPLY);
		searchPanel.add(searchPanelSouth, BorderLayout.SOUTH);
		applyButton.addActionListener(this);
		searchPanelSouth.add(applyButton);
		// initialize center of searchPanel
		GVPanel searchPanelCenter = new GVPanel(this.getTranslator(),
				new GridLayout(3, 1));
		buttonGroup = new ButtonGroup();
		GVRadioButton vertexNrRadioButton = new GVRadioButton(this
				.getTranslator(), new Resource(SEARCH_DIALOG_BUNDLE,
				"search.vertex.label"));
		GVRadioButton textRadioButton = new GVRadioButton(this.getTranslator(),
				new Resource(SEARCH_DIALOG_BUNDLE, "search.text.label"));
		GVRadioButton regExprRadioButton = new GVRadioButton(this
				.getTranslator(), new Resource(SEARCH_DIALOG_BUNDLE,
				"search.regExpr.label"));
		vertexNrRadioButton.setActionCommand("" + this.VERTEX_SEARCH);
		regExprRadioButton.setActionCommand("" + this.REG_EXPR_SEARCH);
		textRadioButton.setActionCommand("" + this.TEXT_SEARCH);
		buttonGroup.add(vertexNrRadioButton);
		buttonGroup.add(textRadioButton);
		buttonGroup.add(regExprRadioButton);
		vertexNrRadioButton.setSelected(true);
		searchPanelCenter.add(vertexNrRadioButton);
		searchPanelCenter.add(textRadioButton);
		searchPanelCenter.add(regExprRadioButton);
		searchPanel.add(searchPanelCenter, BorderLayout.CENTER);

		// set weights for main panels and add to search dialog
		c.weighty = 0;
		c.weightx = 1;
		c.gridy = 0;
		c.gridheight = 1;
		this.add(searchPanel, c);
		c.gridy = 1;
		c.weighty = 1;
		c.gridheight = 2;
		this.add(resultPanel, c);

		// set up scrollable viewport for result panel
		viewportPanel = new GVPanel(this.getTranslator(), new GridLayout(0, 1));
		resultScrollPane = new JScrollPane(viewportPanel);

		resultScrollPane
				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		resultScrollPane
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		resultPanel.add(resultScrollPane);
		// initialize help panels for keeping result panel in right format
		for (int i = 0; i < 4; i++) {
			fill[i] = new JPanel();
		}
	}

	public void setGraphPane(GraphPane graphPane) {
		this.graphPane = graphPane;
	}

	/**
	 * handles actions performed search panel
	 */
	public void actionPerformed(ActionEvent e) {
		String text;
		text = ((JButton) e.getSource()).getText();
		// show or hide results
		if (text.equals(hideText) || text.equals(showText)) {
			if (hidden) {
				showResultPanel();
			} else {
				hideResultPanel();
			}
		} else if (((JButton) e.getSource()).getActionCommand().equals(
				this.APPLY)) {
			Object[] newSelection = graphPane.getSelectedJGraph()
					.getSelectionCells();
			viewportPanel.removeAll();
			resultCount = 0;
			results = new LinkedList<VertexNode>();

			for (int i = 0; i < newSelection.length; i++) {
				DefaultGraphCell cell = (DefaultGraphCell) newSelection[i];
				VertexNode node = new VertexNode(cell);
				findPredsAndSuccs(cell);
				node.setPreds(predList);
				node.setSuccs(succList);
				results.add(node);
			}
			addResults();
			showResultPanel();
			this.dialog.setVisible(true);

		} else {
			viewportPanel.removeAll();
			resultCount = 0;
			results = new LinkedList<VertexNode>();

			search((new Integer(buttonGroup.getSelection().getActionCommand()))
					.intValue(), searchField.getText());

			showResultPanel();
			this.dialog.setVisible(true);
		}
	}

	private void showResultPanel() {
		this.add(resultPanel, c);
		this.dialog.setSize(SHOW_SIZE);
		hidden = false;
		hideButton.setText(hideText);
		this.dialog.setVisible(true);
	}

	private void hideResultPanel() {
		this.remove(resultPanel);
		this.dialog.setSize(HIDE_SIZE);
		hidden = true;
		hideButton.setText(showText);
		this.dialog.setVisible(true);
	}

	private void addResults() {

		if (resultCount != 0) {
			for (int i = resultCount; i < 4; i++) {
				viewportPanel.remove(fill[i - 1]);
			}
		}
		Collections.sort(results);
		for (Iterator<VertexNode> i = results.iterator(); i.hasNext();) {
			VertexNode node = i.next();
			viewportPanel.add(new ResultItem(node));

			resultCount++;
		}
		for (int i = resultCount; i < 3; i++) {
			viewportPanel.add(fill[i]);
		}
		this.dialog.setVisible(true);
	}

	/**
	 * perform search on set of nodes on currently displayed tab
	 *
	 * @param searchType
	 *            determines wether to look for a vertex number, text, or a
	 *            regular expression
	 * @param searchString
	 *            the # / text / reg expr to look for
	 */
	private void search(int searchType, String searchString) {
		// to store results
		results = new LinkedList<VertexNode>();
		// get currently displayed tab and its cells
		resultCount = 0;
		viewportPanel.removeAll();
		JGraph graph = (JGraph) ((JScrollPane) graphPane.getSelectedComponent()).getViewport().getComponent(0);
		GraphModel model = graph.getModel();
		Object[] cells = DefaultGraphModel.getAll(model);
		// remove selection from cells
		graph.clearSelection();
		// iterate through all the cells
		for (Object o : cells) {
			if (!model.isPort(o)) {
				DefaultGraphCell cell = (DefaultGraphCell) o;
				if (!model.isEdge(cell)
						&& (cell.getAttributes()).get("kind") != null) {
					// cell is valid vertex
					int id = ((Integer) cell.getAttributes().get("id")).intValue();
					// remove html-tags
					String label = cell.toString();
					label = new String(label.replace("</html>", " "));
					label = new String(label.replace("<html>", " "));
					label = new String(label.replace("</br>", " "));
					label = new String(label.replace("<br>", " "));
					// determine type of search and add results to results-list
					switch (searchType) {
					case VERTEX_SEARCH:
						int vertexNr = (new Integer(searchString)).intValue();
						if (id == vertexNr) {
							VertexNode node = new VertexNode(cell);
							findPredsAndSuccs(cell);
							node.setPreds(predList);
							node.setSuccs(succList);
							results.add(node);
						}
						break;
					case TEXT_SEARCH:
						if (label.contains(searchString)) {
							VertexNode node = new VertexNode(cell);
							findPredsAndSuccs(cell);
							node.setPreds(predList);
							node.setSuccs(succList);
							results.add(node);
						}
						break;
					case REG_EXPR_SEARCH:
						Pattern pattern = Pattern.compile(searchString);
						Matcher matcher = pattern.matcher(label);
						if (matcher.find()) {
							VertexNode node = new VertexNode(cell);
							findPredsAndSuccs(cell);
							node.setPreds(predList);
							node.setSuccs(succList);
							results.add(node);
						}
						break;
					default:
						break;
					}
				}
			}
		}
		addResults();
	}

	private void findPredsAndSuccs(DefaultGraphCell cell) {

	   currGraphCell = cell;


       PredAction predAction = (PredAction) graphPane.owner.getActions().get(PredAction.class);
       predAction.actionPerformed(null);

       SuccAction succAction = (SuccAction) graphPane.owner.getActions().get(SuccAction.class);
       succAction.actionPerformed(null);
	}

	/**
	 * contains a panel representing one search result item also shows the preds
	 * and succs of the item
	 */
	private class ResultItem extends JPanel implements ActionListener {
		private static final long serialVersionUID = -2704762055215080845L;

		private final String PRED = "PRED"; // button label for pred

		private final String SUCC = "SUCC"; // button label for succ

		private final String NODE = "NODE"; // action comand for node button

		private JPanel predPanel = null; // panel containing preds

		private JPanel succPanel = null; // panel containing succs

		private boolean predSelected = false; // pred button state

		private boolean succSelected = false; // succ button state

		private JButton nodeButton = null; // to select node

		private VertexNode node = null; // result node

		/**
		 * constructor for one result item
		 *
		 * @param node
		 *            one search result
		 */
		public ResultItem(VertexNode node) {
			this.node = node;
			setBorder(BorderFactory.createTitledBorder(BorderFactory
					.createRaisedBevelBorder(), node.toString()));
			initComponents();
		}

		/**
		 * initialize GUI components
		 */
		private void initComponents() {
			// initialize components of single result item
			JPanel succAndPred = new JPanel(new GridLayout(2, 1));
			predPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			succPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			predPanel.setBackground(Color.WHITE);
			this.setLayout(new BorderLayout());
			JButton predButton = new JButton(PRED);
			JButton succButton = new JButton(SUCC);
			predButton.setBackground(Color.LIGHT_GRAY);
			succButton.setBackground(Color.LIGHT_GRAY);
			predButton.setActionCommand(this.PRED);
			succButton.setActionCommand(this.SUCC);
			predButton.addActionListener(this);
			succButton.addActionListener(this);
			predPanel.add(predButton);
			succPanel.add(succButton);
			nodeButton = new JButton("" + node.getID());
			nodeButton.setBackground(Color.RED);
			nodeButton.addActionListener(this);
			nodeButton.setActionCommand(this.NODE);
			this.add(nodeButton, BorderLayout.WEST);
			JGraph graph = graphPane.getSelectedJGraph();
			graph.addSelectionCell(node.getCell());
			// initialize predecessor items
			for (Iterator<?> p = node.getPreds().iterator(); p.hasNext();) {
				Object pred = p.next();
				JButton predItem = new JButton(pred.toString());
				predItem.setBackground(Color.WHITE);
				predItem.addActionListener(this);
				predItem.setActionCommand("" + ((VertexNode) pred).getID());
				predPanel.add(predItem);
			}
			// initialize successor items
			for (Iterator<?> s = node.getSuccs().iterator(); s.hasNext();) {
				Object succ = s.next();
				JButton succItem = new JButton(succ.toString());
				succItem.setBackground(Color.WHITE);
				succItem.addActionListener(this);
				succItem.setActionCommand("" + ((VertexNode) succ).getID());
				succPanel.add(succItem);
			}
			succAndPred.add(predPanel);
			succAndPred.add(succPanel);
			this.add(succAndPred, BorderLayout.CENTER);
			this.setSize(new Dimension(resultPanel.getSize().width, 100));
			this.setVisible(true);
		}

		/**
		 * handles events if a button on result panel is pressed
		 */
		public void actionPerformed(ActionEvent e) {

			String command = e.getActionCommand();

			// pred buttton pressed
			if (command.equals(this.PRED)) {
				JButton predButton = ((JButton) e.getSource());
				// reverse selection state
				predSelected = !predSelected;
				// if pred button selected, change color
				if (predSelected) {
					predButton.setBackground(Color.RED);
					for (int i = 1; i < this.predPanel.getComponentCount(); i++) {
						JButton pressButton = (JButton) this.predPanel
								.getComponent(i);
						pressButton.setBackground(Color.RED);
					}
					// if not selected, change color back
				} else {
					predButton.setBackground(Color.LIGHT_GRAY);
					for (int i = 1; i < this.predPanel.getComponentCount(); i++) {
						JButton pressButton = (JButton) this.predPanel
								.getComponent(i);
						pressButton.setBackground(Color.WHITE);
					}
				}
				// add or remove selection
				for (Iterator<?> p = node.getPreds().iterator(); p.hasNext();) {
					selectItem(((VertexNode) p.next()).getID(), predSelected);
				}
				// succ button pressed
			} else if (command.equals(this.SUCC)) {
				JButton succButton = ((JButton) e.getSource());
				// reverse selection state
				succSelected = !succSelected;
				// if succ button selected, change color
				if (succSelected) {
					succButton.setBackground(Color.RED);
					for (int i = 1; i < this.succPanel.getComponentCount(); i++) {
						JButton pressButton = (JButton) this.succPanel
								.getComponent(i);
						pressButton.setBackground(Color.RED);
					}
					// if not selected, change color back
				} else {
					succButton.setBackground(Color.LIGHT_GRAY);
					for (int i = 1; i < this.succPanel.getComponentCount(); i++) {
						JButton pressButton = (JButton) this.succPanel
								.getComponent(i);
						pressButton.setBackground(Color.WHITE);
					}
				}
				// add or remove selection
				for (Iterator<?> s = node.getSuccs().iterator(); s.hasNext();) {
					selectItem(((VertexNode) s.next()).getID(), succSelected);
				}
				// search result node selected
			} else if (command.equals(this.NODE)) {
				JButton button = ((JButton) e.getSource());
				if (button.getBackground().equals(Color.RED)) {
					button.setBackground(Color.WHITE);
					graphPane.getSelectedJGraph().removeSelectionCell(
							node.getCell());
				} else {
					button.setBackground(Color.RED);
					graphPane.getSelectedJGraph().addSelectionCell(
							node.getCell());
				}
				// single pred or succ node selected
			} else {
				JButton button = ((JButton) e.getSource());
				int id = (new Integer(button.getActionCommand())).intValue();
				if (button.getBackground().equals(Color.RED)) {
					button.setBackground(Color.WHITE);
					selectItem(id, false);
				} else {
					button.setBackground(Color.RED);
					graphPane.getSelectedJGraph().addSelectionCell(
							node.getCell());
					selectItem(id, true);
				}
			}
		}

		public void selectItem(int id, boolean select) {
			JGraph graph = graphPane.getSelectedJGraph();
//			GraphLayoutCache view = graph.getGraphLayoutCache();
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
							if (graph.isCellSelected(cell) && !select) {
								graph.removeSelectionCell(cell);
							} else if (!graph.isCellSelected(cell) && select) {
								graph.addSelectionCell(cell);
								break;
							}
						}
					}
				}
			}
		}
	}
}
