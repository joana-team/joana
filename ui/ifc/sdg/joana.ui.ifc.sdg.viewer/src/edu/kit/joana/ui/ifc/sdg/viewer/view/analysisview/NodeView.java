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

import org.eclipse.core.resources.IProject;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


class NodeView  extends TreeNode {
 // the parent
    private CriteriaView parent;
    private SDGNode node;

    /** Creates a new Runs object.
     *
     * @param alg  The concrete algorithm in a wrapper.
     */
    public NodeView(SDGNode node) {
        this.node = node;
    }

    /** Sets the parent to a new value.
     *
        return (TreeNode [])children.toArray(new TreeNode[children.size()]);
     * @param parent  The new parent.
     */
    public void setParent(CriteriaView parent) {
        this.parent = parent;
    }

    public CriteriaView getParent() {
        return parent;
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
        return node.toString();
    }

    public IProject getProject() {
        return parent.getProject();
    }

    public SDGNode getNode() {
        return node;
    }

	@Override
	public boolean[] getPossibleActions() {
		boolean[] ret = {true, false, false, false, false, false, false, false, false, false, false, false};
		return ret;
	}
}
