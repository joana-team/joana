/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.util;

import java.util.List;
import java.util.Map;

import org.jgraph.graph.DefaultGraphCell;

import edu.kit.joana.ui.ifc.sdg.graphviewer.layout.PDGConstants;

/**
 * class to stores information of a single vertex node
 * if sorted, it is ordered by vertex ids
 */
public class VertexNode implements Comparable<VertexNode> {

	/**
	 * kind of vertex
	 */
	private String kind = null;

	/**
	 *  vertex id
	 */
	private int id = -1;

	/**
	 * vertex label
	 */
	private String label = null;

	private List<?> preds = null;

	private List<?> succs = null;

	private DefaultGraphCell cell;

	/**
	 * creates a vertex node
	 * @param cell
	 * 			contains vertex information
	 */
	public VertexNode(DefaultGraphCell cell) {
		// initialize variables
		this.cell = cell;
		Map<?, ?> map = cell.getAttributes();
		kind = PDGConstants.getKind(map);
		id = PDGConstants.getID(map);
		String toolTip = PDGConstants.getToolTip(map);
		if (toolTip == null) {
			label = cell.toString();
			label = new String(label.replace("</html>", " "));
			label = new String(label.replace("<html>", " "));
			label = new String(label.replace("</br>", " "));
			label = new String(label.replace("<br>", " "));
		} else {
			label = toolTip;
		}
	}

	public int getID() {
		return id;
	}

	public String getKind() {
		return kind;
	}

	@Override
	public String toString() {
		return label;
	}

	public int compareTo(VertexNode o) {
		return (this.getID() - ((VertexNode)o).getID());
	}

	public void setPreds(List<?> preds) {
		this.preds = preds;
	}

	public List<?> getPreds() {
		return preds;
	}

	public void setSuccs(List<?> succs) {
		this.succs = succs;
	}

	public List<?> getSuccs() {
		return succs;
	}

	public DefaultGraphCell getCell() {
		return this.cell;
	}
}
