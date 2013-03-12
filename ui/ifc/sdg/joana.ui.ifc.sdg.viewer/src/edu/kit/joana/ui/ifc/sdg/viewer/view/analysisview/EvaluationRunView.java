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

import edu.kit.joana.ui.ifc.sdg.viewer.model.EvaluationRun;

public class EvaluationRunView extends TreeNode implements Parent<TreeNode> {

	private GraphView parent;
	private EvaluationRun run;
	private ArrayList<EvaluationAlgorithmView> children;
	private ArrayList<EvaluationCriteriaView> criterias;
	private boolean executed, executing;

	public EvaluationRunView(EvaluationRun run) {
		this.run = run;
		run.setView(this);
		children = new ArrayList<EvaluationAlgorithmView>();
		criterias = new ArrayList<EvaluationCriteriaView>();
		executed = false;
		executing = false;
	}

	@Override
	public TreeNode[] getChildren() {
		TreeNode[] ret = new TreeNode[children.size() + criterias.size()];
		System.arraycopy(children.toArray(new TreeNode[children.size()]), 0, ret, 0, children.size());
		System.arraycopy(criterias.toArray(new TreeNode[children.size()]), 0, ret, children.size(), criterias.size());
		return ret;
	}

	@Override
	public void removeChild(TreeNode child) {
		if (child instanceof EvaluationAlgorithmView) {
			((EvaluationAlgorithmView) child).setParent(null);
			children.remove(child);
			run.removeChild(((EvaluationAlgorithmView) child).getRun());
		} else if (child instanceof EvaluationCriteriaView) {
			criterias.remove(child);
			((EvaluationCriteriaView) child).setParent(null);
			run.removeCriteria(((EvaluationCriteriaView) child).getCriteria());
		}
	}

	@Override
	public boolean hasChildren() {
		return children.size() > 0 || criterias.size() > 0;
	}

	@Override
	public Parent<TreeNode> getParent() {
		return parent;
	}

	@Override
	public String getName() {
		return run.getName();
	}

	@Override
	public IProject getProject() {
		return parent.getProject();
	}

	@Override
	public void doubleClicked() {

	}

	public void setParent(GraphView parent) {
		this.parent = parent;
	}

	@Override
	public void addChild(TreeNode child) {
		if (child instanceof EvaluationAlgorithmView) {
			((EvaluationAlgorithmView) child).setParent(this);
			children.add((EvaluationAlgorithmView) child);
			run.addChild(((EvaluationAlgorithmView) child).getRun());
		} else if (child instanceof EvaluationCriteriaView) {
			criterias.add((EvaluationCriteriaView) child);
			run.addCriteria(((EvaluationCriteriaView) child).getCriteria());
			((EvaluationCriteriaView) child).setParent(this);
		}
		executed = false;
	}

	public void setParent(Parent<TreeNode> p) {
		this.parent = (GraphView) p;
	}

	@Override
	public boolean[] getPossibleActions() {
		boolean[] ret = {true, false, false, false, false, true, true, true, false, false, true, true};
		return ret;
	}

	/** Starts the algorithm.
	 *
	 */
	public void execute() {
		boolean ok = true;

		for (EvaluationAlgorithmView ev : children) {
			for (CriteriaCategoryView cv : ev.getChildren()) {
				if (!cv.hasChildren()) {
					ok = false;
				}
			}
		}
		if (ok) {
			executing = true;
			run.execute();
		} else {
			AnalysisView.getInstance().showMessage("You have to define at least 1 criterion per criterion category");
		}
	}

	public void showResult() {
		run.showResult();
	}

	public boolean hasExecuted() {
		return executed;
	}

	public boolean isExecuting() {
		return executing;
	}

	public void finished() {
		executing = false;
		executed = true;
		AnalysisView.getInstance().refresh();
	}
}
