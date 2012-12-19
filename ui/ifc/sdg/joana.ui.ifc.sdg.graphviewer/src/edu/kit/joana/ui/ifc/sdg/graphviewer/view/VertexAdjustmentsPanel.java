/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)VertexAdjustmentsPanel.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 06.11.2004 at 13:38:29
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.DefaultTranslator;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVButton;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVColorChooser;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVLabel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVPanel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.pdg.PDGAttributeMap;



import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JComboBox;

import org.jgraph.graph.GraphConstants;


/**
 * A tab that opens in the adjustments dialog. It allows to choose a colour for
 * each node type and vertex type.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>, <a
 *         href="mailto:westerhe@fmi.uni-passau.de">Marieke Westerheide </a>
 * @version 1.1
 */
public class VertexAdjustmentsPanel extends GVPanel implements ActionListener, BundleConstants {
	private static final long serialVersionUID = -1610819056570344287L;
	private static final int PREVIEW_HEIGHT = 20;
	private static final int PREVIEW_BORDER_THICKNESS = 1;

	/**
	 * A pane of controls designed to allow a user to manipulate and select a
	 * color.
	 */
	private GVColorChooser chooser = null;

	/**
	 * A combo box where a node type can be chosen.
	 */
	private JComboBox nodesList;

	/**
	 * A combo box where a edge type can be chosen.
	 */
	private JComboBox edgeList;

	private static final String BACKGROUND_COLOR_ACTION_COMMAND = "background";

	private static final String EDGE_COLOR_ACTION_COMMAND = "edge";

	/**
	 * A Hashtable that maps a node type to its background colour.
	 */
	protected Properties backgroundColors = null;

	/**
	 * Shows the colour chosen in <code>chooser</code>.
	 */
	protected JLabel backgroundColorPreview = new JLabel();

	protected JLabel edgeColorPreview = new JLabel();

	private static final String FOREGROUND_COLOR_ACTION_COMMAND = "foreground";

	/**
	 * The current foreground colour.
	 */
	protected Color foregroundColor = Color.BLACK;

	/**
	 * Shows the colour chosen in <code>chooser</code>.
	 */
	protected JLabel foregroundColorPreview = new JLabel();

	/**
	 * @param colors
	 *            a hashtable that maps a node type to its background colour
	 * @param maps
	 *            a hashtable that maps a node type to its attribute map, which
	 *            also defines the colour
	 */
	public VertexAdjustmentsPanel(Properties colors, Hashtable<?, ?> maps) {
		super(new DefaultTranslator(), new GridBagLayout());
		this.backgroundColors = colors;
		this.restoreValues(maps);
	}

	/**
	 * @see edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVPanel#setTranslator(edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator)
	 * @param translator
	 */
	@Override
	public void setTranslator(Translator translator) {
		this.translator.removeLanguageListener(this);
		this.translator = translator;
		this.translator.addLanguageListener(this);
		this.initComponents();
	}

	/**
	 * Initializes and lays out the components shown on this tab.
	 */
	private void initComponents() {
		this.chooser = new GVColorChooser(this.translator, new Resource(
				VERTICES_BUNDLE, "colorChooser.dialog.title"));

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.NONE;
		constraints.insets = new Insets(10, 10, 0, 0);

		/*
		 * vertex drop-down menu
		 */
		constraints.gridx = 0;
		constraints.gridy = 0;

		GVLabel nodeTypeLabel = new GVLabel(this.translator, new Resource(
				VERTICES_BUNDLE, "nodeType.label"));
		this.add(nodeTypeLabel, constraints);

		constraints.gridx = GridBagConstraints.RELATIVE;

		this.nodesList = new JComboBox(SDGNode.Kind.values());
		nodesList.addActionListener(this);
		this.add(nodesList, constraints);

		/*
		 * background
		 */
		constraints.gridx = 2;
		constraints.gridy = 0;

		GVLabel backgroundColorLabel = new GVLabel(this.translator,
				new Resource(VERTICES_BUNDLE, "backgroundColor.label"));
		this.add(backgroundColorLabel, constraints);

		constraints.gridx = GridBagConstraints.RELATIVE;

		this.backgroundColorPreview = new JLabel();
		this.backgroundColorPreview.setPreferredSize(new Dimension(50, 20));
		this.setPreviewColor(this.backgroundColorPreview, Color
				.decode(backgroundColors.getProperty(nodesList
						.getSelectedItem().toString())));
		this.add(this.backgroundColorPreview, constraints);

		GVButton backgroundColorButton = new GVButton(this.translator,
				new Resource(VERTICES_BUNDLE, "choose.label"));
		backgroundColorButton.setActionCommand(BACKGROUND_COLOR_ACTION_COMMAND);
		backgroundColorButton.addActionListener(this);
		this.add(backgroundColorButton, constraints);

		/*
		 * foreground
		 */
		constraints.gridx = 2;
		constraints.gridy = 1;

		GVLabel foregroundColorLabel = new GVLabel(this.translator,
				new Resource(VERTICES_BUNDLE, "foregroundColor.label"));
		this.add(foregroundColorLabel, constraints);

		constraints.gridx = GridBagConstraints.RELATIVE;

		this.foregroundColorPreview = new JLabel();
		this.foregroundColorPreview.setPreferredSize(new Dimension(50, 20));
		this.setPreviewColor(this.foregroundColorPreview, this.foregroundColor);
		this.add(this.foregroundColorPreview, constraints);

		GVButton foregroundColorButton = new GVButton(this.translator,
				new Resource(VERTICES_BUNDLE, "choose.label"));
		foregroundColorButton.setActionCommand(FOREGROUND_COLOR_ACTION_COMMAND);
		foregroundColorButton.addActionListener(this);
		this.add(foregroundColorButton, constraints);

		/*
		 * edge drop-down menu
		 */
		constraints.gridx = 0;
		constraints.gridy = 2;

		GVLabel edgeTypeLabel = new GVLabel(this.translator, new Resource(
				VERTICES_BUNDLE, "edgeType.label"));
		this.add(edgeTypeLabel, constraints);

		constraints.gridx = GridBagConstraints.RELATIVE;

		this.edgeList = new JComboBox(SDGEdge.Kind.values());
		edgeList.addActionListener(this);
		this.add(edgeList, constraints);

		/*
		 * edge color
		 */
		constraints.gridx = 2;
		constraints.gridy = 2;

		GVLabel edgeColorLabel = new GVLabel(this.translator, new Resource(
				VERTICES_BUNDLE, "edgeColor.label"));
		this.add(edgeColorLabel, constraints);

		constraints.gridx = GridBagConstraints.RELATIVE;

		this.edgeColorPreview = new JLabel();
		this.edgeColorPreview.setPreferredSize(new Dimension(50, 20));
		this.setPreviewColor(this.edgeColorPreview, Color.YELLOW);
		this.add(this.edgeColorPreview, constraints);

		GVButton edgeColorButton = new GVButton(this.translator, new Resource(
				VERTICES_BUNDLE, "choose.label"));
		edgeColorButton.setActionCommand(EDGE_COLOR_ACTION_COMMAND);
		edgeColorButton.addActionListener(this);
		this.add(edgeColorButton, constraints);

		/*
		 * spaces
		 */
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1.0;
		constraints.insets = new Insets(0, 0, 0, 0);
		this.add(new JLabel(), constraints);

		constraints.gridx = 3;
		constraints.gridy = GridBagConstraints.RELATIVE;
		constraints.weightx = 0;
		constraints.weighty = 1.0;
		constraints.fill = GridBagConstraints.VERTICAL;
		this.add(new JLabel(), constraints);
	}

	private void setPreviewColor(JLabel label, Color col) {
		label.setBorder(BorderFactory.createCompoundBorder(BorderFactory
				.createLineBorder(Color.BLACK, PREVIEW_BORDER_THICKNESS),
				BorderFactory.createLineBorder(col, PREVIEW_HEIGHT - 2
						* PREVIEW_BORDER_THICKNESS)));
	}

	/**
	 * Reads out the information about the colours contained in map and stores
	 * it in an instance variable.
	 *
	 * @param maps
	 *            a hashtable that maps a node type to its attribute map, which
	 *            defines the colour
	 */
	public void restoreValues(Hashtable<?, ?> maps) {

		for (Enumeration<?> e = maps.keys(); e.hasMoreElements();) {
			SDGNode.Kind key = (SDGNode.Kind) e.nextElement();
			PDGAttributeMap map = (PDGAttributeMap) maps.get(key);
			this.setBackgroundColor(GraphConstants.getBackground(map), key
					.toString());
			this.setForegroundColor(GraphConstants.getForeground(map));
		}
	}

	/**
	 * Reacts when the user selected a node type in the combo list or chose a
	 * new colour by updating the colour preview label and/or storing the newly
	 * chosen colour.
	 *
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 * @param event
	 */
	public void actionPerformed(ActionEvent event) {
		if (event.getActionCommand().equals(BACKGROUND_COLOR_ACTION_COMMAND)) {
			String sdgNodeType = nodesList.getSelectedItem().toString();
			Color col = this.chooser.showDialog(this, Color
					.decode(backgroundColors.getProperty(sdgNodeType)));
			if (col != null) {
				this.setBackgroundColor(col, sdgNodeType);
			}
		} else if (event.getActionCommand().equals(
				FOREGROUND_COLOR_ACTION_COMMAND)) {
			Color col = this.chooser.showDialog(this, this.foregroundColor);
			if (col != null) {
				this.setForegroundColor(col);
			}

		} else if (event.getActionCommand().equals(EDGE_COLOR_ACTION_COMMAND)) {

			String sdgEdgeType = edgeList.getSelectedItem().toString();

			Color col = this.chooser
					.showDialog(this, Color.decode(backgroundColors
							.getProperty(sdgEdgeType + "_COL")));
			if (col != null) {
				this.setEdgeColor(col, sdgEdgeType);
			}
		} else if (event.getSource().equals(nodesList)) {
			this.setPreviewColor(this.backgroundColorPreview, Color
					.decode(backgroundColors.getProperty(nodesList
							.getSelectedItem().toString())));
		} else if (event.getSource().equals(edgeList)) {
			this.setPreviewColor(this.edgeColorPreview, Color
					.decode(backgroundColors.getProperty(edgeList
							.getSelectedItem().toString() + "_COL")));
		}
	}

	/**
	 * @param sdgNodeType
	 *            the node type of which we want to know the current colour
	 * @return the current colour of nodes of type sdgNodeType
	 */
	public Color getBackgroundColor(String sdgNodeType) {
		return Color.decode(backgroundColors.getProperty(sdgNodeType));
	}

	/**
	 * Stores the new colour for the given node type in an instance variable.
	 * (The nodes will take their new colour only when the user clicks OK on the
	 * parent adjustments dialog.)
	 *
	 * @param col
	 *            the new colour
	 * @param sdgNodeType
	 *            the node type that shall take this colour
	 */
	protected void setBackgroundColor(Color col, String sdgNodeType) {
		this.backgroundColors.setProperty(sdgNodeType, "" + col.getRGB());
		this.setPreviewColor(this.backgroundColorPreview, Color
				.decode(backgroundColors.getProperty(nodesList
						.getSelectedItem().toString())));
	}

	public Color getEdgeColor(String sdgEdgeType) {
		return Color.decode(backgroundColors.getProperty(sdgEdgeType + "_COL"));
	}

	/**
	 * Stores the new color for the given edge type in an instance variable.
	 * (The edges will take their new color only when the user clicks OK on the
	 * parent adjustments dialog.)
	 *
	 * @param col
	 *            the new colour
	 * @param sdgEdgeType
	 *            the node type that shall take this color
	 */
	protected void setEdgeColor(Color col, String sdgEdgeType) {
		this.backgroundColors.setProperty(sdgEdgeType + "_COL", "" + col.getRGB());
		this.setPreviewColor(this.edgeColorPreview, Color
				.decode(backgroundColors.getProperty(edgeList.getSelectedItem()
						.toString() + "_COL")));
	}

	/**
	 * Stores the new foreground colour in an instance variable. (The labels
	 * will take their new colour only when the user clicks OK on the parent
	 * adjustments dialog.)
	 *
	 * @return the current foreground (text) colour of the nodes
	 */
	public Color getForegroundColor() {
		return this.foregroundColor;
	}

	/**
	 * @param col
	 *            the new colour
	 */
	protected void setForegroundColor(Color col) {
		this.foregroundColor = col;
		this.setPreviewColor(this.foregroundColorPreview, col);
	}

}
