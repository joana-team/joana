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

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ui.ifc.sdg.viewer.model.Criteria;


class CriteriaView extends TreeNode implements Parent<NodeView> {
	// the parent
	private CriteriaCategoryView parent;
	// a wrapper with the concrete algorithm
	private Criteria crit;
    private ArrayList<NodeView> children;

	/** Creates a new Runs object.
	 *
	 * @param alg  The concrete algorithm in a wrapper.
	 */
	public CriteriaView(Criteria crit) {
		this.crit = crit;
		children = new ArrayList<NodeView>();

		for (SDGNode n : crit.retrieveCriteria()) {
		    NodeView v = new NodeView(n);
		    v.setParent(this);
		    children.add(v);
		}
	}

	/** Sets the parent to a new value.
	 *
	 * @param parent  The new parent.
	 */
	public void setParent(CriteriaCategoryView parent) {
		this.parent = parent;
	}

	public CriteriaCategoryView getParent() {
		return parent;
	}

    /** Returns the children.
     */
    public NodeView[] getChildren() {
        return children.toArray(new NodeView[children.size()]);
    }

    /** Removes the given child.
     */
    public void removeChild(NodeView child) {
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
		return crit.toString();
	}

	public IProject getProject() {
        return parent.getProject();
    }

	public Criteria getCriteria() {
		return crit;
	}

	@Override
	public void addChild(NodeView child) {
		//Do Nothing, because its not intended to be called
	}

	@Override
	public boolean[] getPossibleActions() {
		boolean[] ret = {false, false, false, false, false, false, false, false, false, false, false, false};
		return ret;
	}
}
