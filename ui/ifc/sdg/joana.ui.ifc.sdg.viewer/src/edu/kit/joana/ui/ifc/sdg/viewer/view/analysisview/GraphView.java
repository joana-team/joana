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
package edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview;

import java.util.LinkedList;


import org.eclipse.core.resources.IProject;

import edu.kit.joana.ui.ifc.sdg.viewer.algorithms.Algorithm;
import edu.kit.joana.ui.ifc.sdg.viewer.model.Graph;

class GraphView extends TreeNode implements Parent<TreeNode> {
	// the SDG
	private Graph graph;
	// the root
	private Root parent;
	// the children
	private LinkedList<TreeNode> children;

	/** Creates a new Graph object.
	 *
	 * @param sdg   A SDG.
	 * @param file  A Java source file.
	 */
	public GraphView(Graph graph) {
		this.graph = graph;
		children = new LinkedList<TreeNode>();
	}

	public String getName() {
		return graph.getName();
	}

	/** Sets the parent.
	 *
	 * @param parent  The parent.
	 */
	public void setParent(Root parent) {
		this.parent = parent;
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode#getParent()
	 */
	public Root getParent() {
		return parent;
	}

	/** Adds a new Child
	 *
	 * @param child  The new child.
	 */
	public void addChild(TreeNode child) {
		if (child instanceof ChosenAlgorithmView) {
			graph.addChild(((ChosenAlgorithmView)child).getAlgorithm());
			((ChosenAlgorithmView) child).setParent(this);
		} else if (child instanceof EvaluationRunView) {
			((EvaluationRunView) child).setParent(this);
		}
		children.add(child);

	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#removeChild(edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode)
	 */
	public void removeChild(TreeNode child) {
		if (child instanceof ChosenAlgorithmView) {
			ChosenAlgorithmView a = (ChosenAlgorithmView)child;
			graph.removeChild(a.getAlgorithm());
			a.setParent(null);
		}
		children.remove(child);
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#getChildren()
	 */
	public TreeNode [] getChildren() {
		return children.toArray(new TreeNode[children.size()]);
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#hasChildren()
	 */
	public boolean hasChildren() {
		return children.size() > 0;
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode#doubleClicked()
	 */
	public void doubleClicked() {

	}

	public IProject getProject() {
		return graph.getProject();
	}

	public boolean contains(Algorithm alg) {
		return graph.contains(alg);
	}

	public Graph getGraph() {
		return graph;
	}

	@Override
	public boolean[] getPossibleActions() {
		boolean[] ret = {true, true, true, false, false, false, false, true, false, true, false, false};
		return ret;
	}
}
