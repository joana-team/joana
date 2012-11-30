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

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;

import edu.kit.joana.ui.ifc.sdg.viewer.model.CriteriaCategory;


class CriteriaCategoryView extends TreeNode implements Parent<CriteriaView> {
    // the parent
	private Parent<CriteriaCategoryView> parent;
	// a wrapper with the concrete algorithm
	private CriteriaCategory category;
	private ArrayList<CriteriaView> children;

	/** Creates a new Runs object.
	 *
	 * @param alg  The concrete algorithm in a wrapper.
	 */
	public CriteriaCategoryView(CriteriaCategory category) {
		this.category = category;
		children = new ArrayList<CriteriaView>();
	}

	/** Sets the parent to a new value.
	 *
		return (TreeNode [])children.toArray(new TreeNode[children.size()]);
	 * @param parent  The new parent.
	 */
	public void setParent(Parent<CriteriaCategoryView> parent) {
		this.parent = parent;
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode#getParent()
	 */
	public Parent<CriteriaCategoryView> getParent() {
		return parent;
	}

	/** Adds a new child.
	 *
	 * @param child  The new child.
	 */
	@Override
	public void addChild(CriteriaView child) {
		children.add(child);
		child.setParent(this);
	}

	/** Returns the children.
	 */
	public CriteriaView[] getChildren() {
		return children.toArray(new CriteriaView[children.size()]);
	}

	/** Removes the given child.
	 */
	public void removeChild(CriteriaView child) {
		category.removeChild(child.getCriteria());
		children.remove(child);
		child.setParent(null);
	}

	/** Returns true if this element has children.
	 */
	public boolean hasChildren() {
		return children.size() > 0;
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
		return category.getName();
	}

	public IProject getProject() {
        return ((TreeNode) parent).getProject();
    }

	@Override
	public boolean[] getPossibleActions() {
		boolean[] ret = {false, false, false, false, false, false, false, false, false, false, false, false};
		return ret;
	}
}
