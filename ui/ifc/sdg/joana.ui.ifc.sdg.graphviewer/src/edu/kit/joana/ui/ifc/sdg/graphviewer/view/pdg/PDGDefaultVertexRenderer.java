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
/*
 * @(c)PDGDefaultVertexRenderer.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 03.12.2004 at 17:23:17
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view.pdg;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.util.Map;

import org.jgraph.JGraph;

import org.jgraph.graph.CellView;
import org.jgraph.graph.CellViewRenderer;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * This class is responsible for "rendering" (displaying) the vertices.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class PDGDefaultVertexRenderer extends org.jgraph.graph.VertexRenderer implements CellViewRenderer {
	private static final long serialVersionUID = 2148585364940781126L;

	protected JGraph graph = null;

	protected boolean hasFocus, selected, preview;

	protected final static int MAX_NODE_NAME_LENGTH = 30;

	public PDGDefaultVertexRenderer() {
		// this.setWrapStyleWord(true);
	}

	/**
	 * Configure and return the renderer based on the passed in components.
	 *
	 * @see org.jgraph.graph.CellViewRenderer#getRendererComponent(org.jgraph.JGraph,
	 *      org.jgraph.graph.CellView, boolean, boolean, boolean)
	 * @param graph
	 * @param view
	 * @param sel
	 * @param focus
	 * @param preview
	 * @return
	 */
	@Override
	public Component getRendererComponent(JGraph graph, CellView view, boolean sel, boolean focus, boolean preview) {
		this.graph = graph;
		this.setComponentOrientation(graph.getComponentOrientation());

		if (graph.getEditingCell() != view.getCell()) {
			SDGNode node = (SDGNode) ((DefaultGraphCell) view.getCell()).getUserObject();
			// old text style
			// this.setText(Integer.toString(node.getId()));
			// if (node.getLabel() != null) {
			// //this.append("\n" + node.getLabel().toString());
			// this.setText(this.getText()+" \n"+node.getLabel().toString());
			// }

			final StringBuffer buff = new StringBuffer("");
			buff.append("<html>");
			final int number = node.getId();
			buff.append(Integer.toString(number) + ":");

			if (node.getLabel() == null) {
				buff.append("<br>");
				buff.append("*START*");
			} else {
				String nodeText = node.getLabel().toString();

				if (nodeText.length() > MAX_NODE_NAME_LENGTH + "...".length()) {
					// try to shrink params at first
					if (nodeText.indexOf('(') > 0) {
						final int first = nodeText.indexOf('(');
						final int last = nodeText.lastIndexOf(')');
						if (last > first) {
							nodeText = nodeText.substring(0, first) + "(..." + nodeText.substring(last);
						} else {
							nodeText = nodeText.substring(0, first) + "(...)";
						}
					}

					// try to shrink package name
					if (nodeText.length() > MAX_NODE_NAME_LENGTH) {
						final int last = nodeText.indexOf('(');
						int lastPoint = -1;
						// keep className and method name at least.
						int minNumOfParts = 2;
						for (int i = last; i >= 0; i--) {
							if (nodeText.charAt(i) == '.') {
								if (minNumOfParts > 1) {
									minNumOfParts--;
								} else if (lastPoint < 0) {
									lastPoint = i;
									break;
								}
							}
						}

						if (lastPoint > 0) {
							int nextPoint = 0;
							while ((nodeText.length() - nextPoint) > MAX_NODE_NAME_LENGTH) {
								final int next1 = nodeText.indexOf('.', nextPoint + 1);
								final int next2 = nodeText.indexOf('$', nextPoint + 1);

								if (next2 > 0 && next2 < next1) {
									nextPoint = next2;
								} else {
									nextPoint = next1;
								}

								if (nextPoint < 0 || nextPoint == lastPoint) {
									nextPoint = lastPoint;
									break;
								}
							}

							nodeText = nodeText.substring(nextPoint);
						}
					}

//					// shorten more as last resort
//					if (nodeText.length() > MAX_NODE_NAME_LENGTH) {
//						nodeText = nodeText.substring(0, MAX_NODE_NAME_LENGTH - 3) + "...";
//					}
				}

				nodeText = nodeText.replace("<", "&lt;");
				nodeText = nodeText.replace(">", "&gt;");

				buff.append("<br>" + nodeText);
			}

			buff.append("</html>");
			this.setText(buff.toString());
		} else {
			this.setText(null);
		}
		this.hasFocus = focus;
		this.selected = sel;
		this.preview = preview;
		this.installAttributes(view);
		return this;
	}

	/**
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 * @param g
	 */
	@Override
	public void paint(Graphics g) {
		try {
			super.paint(g);
			this.paintSelectionBorder(g);
		} catch (IllegalArgumentException e) {
			// JDK Bug: Zero length string passed to TextLayout constructor
		}
	}

	@Override
	protected void paintSelectionBorder(Graphics g) {
		((Graphics2D) g).setStroke(GraphConstants.SELECTION_STROKE);
		if (this.hasFocus && this.selected)
			g.setColor(graph.getLockedHandleColor());
		else if (this.selected)
			g.setColor(graph.getHighlightColor());
		if (this.selected) {
			Dimension d = this.getSize();
			g.drawRect(0, 0, d.width - 1, d.height - 1);
		}
	}

	@Override
	protected void installAttributes(CellView view) {
		Map<?, ?> map = view.getAllAttributes();
		this.setOpaque(GraphConstants.isOpaque(map));
		this.setBorder(GraphConstants.getBorder(map));
		this.setForeground(GraphConstants.getForeground(map));
		this.setBackground(GraphConstants.getBackground(map));
		this.setFont(GraphConstants.getFont(map));
	}

}
