/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.model;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


/** A Criteria object contains information about a set of statements that are chosen as a criterion.
 *
 * @author giffhorn
 *
 */
public class Criteria {
	private CriteriaCategory parent;
	private String text;
    private Collection<SDGNode> nodes;


	/** Creates a new Criteria object.
	 */
	public Criteria(String text, Collection<SDGNode> nodes) {
        this.text = text;
        this.nodes = nodes;
    }

	/** Sets the parent to a new value.
	 *
	 * @param parent  The new parent.
	 */
	public void setParent(CriteriaCategory parent) {
		this.parent = parent;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode#getParent()
	 */
	public CriteriaCategory getParent() {
		return parent;
	}

	/** Converts the start line and end line information into nodes of the given graph.
	 *
	 * @param g  A graph.
	 * @return   A set of nodes.
	 */
	public Collection<SDGNode> retrieveCriteria() {
	    return nodes;
	}

	public void removeNode(SDGNode node) {
		nodes.remove(node);
	}

	public String toString() {
		return text;
	}
}
