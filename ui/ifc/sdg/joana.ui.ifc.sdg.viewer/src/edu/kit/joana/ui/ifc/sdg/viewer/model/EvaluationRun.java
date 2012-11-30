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

import java.util.ArrayList;

import edu.kit.joana.ui.ifc.sdg.viewer.Activator;
import edu.kit.joana.ui.ifc.sdg.viewer.view.EvaluationResultDialog;
import edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview.EvaluationRunView;



public class EvaluationRun  {

	private static int RUN = 1;

	private ArrayList<Run> children;
	private int runID;
	private ArrayList<EvaluationCriteria> criterias;
	private ArrayList<String> results;
	private EvaluationRunView view;

	public EvaluationRun() {
		runID = RUN++;
		children = new ArrayList<Run>();
		criterias = new ArrayList<EvaluationCriteria>();
		results = new ArrayList<String>();
	}

	public void setView(EvaluationRunView view) {
		this.view = view;
	}

	public String getName() {
		return "Evaluation Run " + runID;
	}

	public void addChild(Run child) {
		children.add(child);
	}

	public boolean removeChild(Run child) {
		return children.remove(child);
	}

	public void addCriteria(EvaluationCriteria crit) {
		criterias.add(crit);
	}

	public boolean removeCriteria(EvaluationCriteria crit) {
		return children.remove(crit);
	}

	public void execute() {
		results = new ArrayList<String>();

		new Thread() {
			public void run() {
				for (Run r : children) {
					for (EvaluationCriteria ec : criterias) {
						ec.executeBefore(r);
					}
					r.run();
					for (EvaluationCriteria ec : criterias) {
						ec.executeAfter();
						results.add(ec.getResult());
					}
				}
				Activator.getDefault().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						view.finished();
					}
				});
			}
		}.start();

	}

	public void showResult() {
		EvaluationResultDialog dlg = new EvaluationResultDialog(Activator.getDefault().getDisplay().getActiveShell(), children, results, criterias);
		dlg.open();
	}

}
