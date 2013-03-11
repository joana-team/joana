/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview;

import java.util.ArrayList;


import org.eclipse.core.resources.IProject;

import edu.kit.joana.ui.ifc.sdg.viewer.model.ChosenAlgorithm;

public class ChosenAlgorithmView extends TreeNode implements Parent<RunView> {
	// the SDG
	private GraphView parent;
	// the algorithms
	private ChosenAlgorithm alg;
	// the various runs
	private ArrayList<RunView> children;

	/** Creates a new Algorithms object.
	 *
	 * @param alg  The algorithm.
	 */
	public ChosenAlgorithmView(ChosenAlgorithm alg) {
		this.alg = alg;
		children = new ArrayList<RunView>();
	}

	/** Sets the parent to a new value.
	 *
	 * @param parent  The new parent.
	 */
	@SuppressWarnings("rawtypes")
	public void setParent(Parent parent) {
		this.parent = (GraphView) parent;
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode#getParent()
	 */
	public GraphView getParent() {
		return parent;
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#getChildren()
	 */
	public RunView[] getChildren() {
		return children.toArray(new RunView[children.size()]);
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#hasChildren()
	 */
	public boolean hasChildren() {
		return children.size() > 0;
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#removeChild(edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode)
	 */
	@Override
	public void removeChild(RunView child) {
		alg.removeChild(child.getRun());
		children.remove(child);
		child.setParent(null);
	}

	/** Adds a new child.
	 *
	 * @param child  The new child.
	 */
	@Override
	public void addChild(RunView child) {
		alg.addChild(child.getRun());
		children.add(child);
		child.setParent(this);
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode#doubleClicked()
	 */
	public void doubleClicked() {

	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode#getName()
	 */
	public String getName() {
		return alg.getName();
	}

	public IProject getProject() {
		return parent.getProject();
	}

	public ChosenAlgorithm getAlgorithm() {
		return alg;
	}

	@Override
	public boolean[] getPossibleActions() {
		boolean[] ret = {true, false, false, false, true, false, false, true, false, false, false, false};
		return ret;
	}

}
