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
package edu.kit.joana.ui.ifc.sdg.viewer.model;

import java.util.LinkedList;

public class CriteriaCategory {
	private Run parent;
	private String name;
	private LinkedList<Criteria> children;

	/** Creates a new Runs object.
	 *
	 * @param alg  The concrete algorithm in a wrapper.
	 */
	public CriteriaCategory(String name) {
		this.name = name;
		this.children = new LinkedList<Criteria>();
	}

	/** Sets the parent to a new value.
	 *
	 * @param parent  The new parent.
	 */
	public void setParent(Run parent) {
		this.parent = parent;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode#getParent()
	 */
	public Run getParent() {
		return parent;
	}

	/** Adds a new Child
	 *
	 * @param child  The new child.
	 */
	public void addChild(Criteria child) {
		children.add(child);
		child.setParent(this);
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#removeChild(edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode)
	 */
	public void removeChild(Criteria child) {
		children.remove(child);
		child.setParent(null);
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#getChildren()
	 */
	public LinkedList<Criteria> getChildren() {
		return children;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#hasChildren()
	 */
	public boolean hasChildren() {
		return true;
	}

	public String getName() {
		return name;
	}
}
