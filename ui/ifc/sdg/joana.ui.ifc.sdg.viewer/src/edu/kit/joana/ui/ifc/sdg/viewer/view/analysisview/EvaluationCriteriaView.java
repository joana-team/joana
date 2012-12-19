/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview;


import org.eclipse.core.resources.IProject;

import edu.kit.joana.ui.ifc.sdg.viewer.model.EvaluationCriteria;

public class EvaluationCriteriaView extends TreeNode {

	private EvaluationRunView parent;
	private EvaluationCriteria crit;

	/**
	 * @param parent
	 * @param crit
	 */
	public EvaluationCriteriaView(EvaluationRunView parent,	EvaluationCriteria crit) {
		this.parent = parent;
		this.crit = crit;
	}

	@Override
	public boolean[] getPossibleActions() {
		boolean[] ret = {false, false, false, false, false, false, false, false, false, false, false, false};
		return ret;
	}

	@Override
	public Parent<TreeNode> getParent() {
		return parent;
	}

	@Override
	public String getName() {
		return crit.getName();
	}

	@Override
	public IProject getProject() {
		return parent.getProject();
	}

	@Override
	public void doubleClicked() {
	}

	public void setParent(EvaluationRunView object) {
		parent = object;
	}

	public EvaluationCriteria getCriteria() {
		return crit;
	}

}
