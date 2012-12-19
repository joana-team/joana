/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.model;

import java.util.ArrayList;

public class ChosenAlgorithm {
	// the SDG
	private Graph parent;
	// the algorithm
	private edu.kit.joana.ui.ifc.sdg.viewer.algorithms.Algorithm alg;
	// the various runs
	private ArrayList<Run> children;

	/** Creates a new Algorithms object.
	 *
	 * @param alg  The algorithm.
	 */
	public ChosenAlgorithm(edu.kit.joana.ui.ifc.sdg.viewer.algorithms.Algorithm alg) {
		this.alg = alg;
		children = new ArrayList<Run>();
	}

	/** Sets the parent to a new value.
	 *
	 * @param parent  The new parent.
	 */
	public void setParent(Graph parent) {
		this.parent = parent;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode#getParent()
	 */
	public Graph getParent() {
		return parent;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#getChildren()
	 */
	public ArrayList<Run> getChildren() {
		return children;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#hasChildren()
	 */
	public boolean hasChildren() {
		return children.size() > 0;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#removeChild(edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode)
	 */
	public void removeChild(Run child) {
		children.remove(child);
		child.setParent(null);
	}

	/** Adds a new child.
	 *
	 * @param child  The new child.
	 */
	public void addChild(Run child) {
		children.add(child);
		child.setParent(this);
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode#getName()
	 */
	public String getName() {
		return alg.getName();
	}

	/** Returns the represented algorithm.
	 *
	 * @return  the contained Algorithm object.
	 */
	public edu.kit.joana.ui.ifc.sdg.viewer.algorithms.Algorithm getAlgorithm() {
		return alg;
	}

	public String getCategory() {
		return alg.getCategory();
	}

	public String getClassName() {
		return alg.getClassName();
	}
}
