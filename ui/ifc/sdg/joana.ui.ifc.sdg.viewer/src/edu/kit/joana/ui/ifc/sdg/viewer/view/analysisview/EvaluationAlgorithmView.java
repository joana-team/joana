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

import edu.kit.joana.ui.ifc.sdg.viewer.model.Criteria;
import edu.kit.joana.ui.ifc.sdg.viewer.model.Run;

public class EvaluationAlgorithmView extends TreeNode implements Parent<CriteriaCategoryView> {

	private ArrayList<CriteriaCategoryView> children;
	private EvaluationRunView parent;
	private Run algo;

	/**
	 * @param algo
	 */
	public EvaluationAlgorithmView(Run algo) {
		this.algo = algo;
		children = new ArrayList<CriteriaCategoryView>();
	}

	@Override
	public CriteriaCategoryView[] getChildren() {
		return children.toArray(new CriteriaCategoryView[children.size()]);
	}

	@Override
	public void removeChild(CriteriaCategoryView child) {
		children.remove(child);
		child.setParent(null);
	}

	/** Adds a new child.
	 *
	 * @param child  The new child.
	 */
	public void addChild(CriteriaCategoryView child) {
		children.add(child);
		child.setParent(this);
	}

	@Override
	public boolean hasChildren() {
		return children.size() > 0;
	}

	@Override
	public Parent getParent() {
		return parent;
	}

	public void setParent(EvaluationRunView parent) {
		this.parent = parent;
	}

	@Override
	public String getName() {
		return algo.getAlgorithm().getName();
	}

	@Override
	public IProject getProject() {
		return parent.getProject();
	}

	@Override
	public void doubleClicked() {

	}

	public Run getRun() {
		return this.algo;
	}

	public void setCriteria(Criteria crit, Enum<?> kind) {
		// insert into model
		algo.setCriteria(crit, kind);

		// insert into view
		for (CriteriaCategoryView cc : children) {
			if (cc.getName().equals(kind.toString())) {
				CriteriaView cv = new CriteriaView(crit);
				cc.addChild(cv);
			}
		}
	}

	public boolean isExecuting() {
		return algo.isExecuting();
	}

	public boolean isFinished() {
		return algo.isFinished();
	}

	@Override
	public boolean[] getPossibleActions() {
		boolean[] ret = {true, false, false, false, false, false, false, false, false, false, false, false};
		return ret;
	}

}
