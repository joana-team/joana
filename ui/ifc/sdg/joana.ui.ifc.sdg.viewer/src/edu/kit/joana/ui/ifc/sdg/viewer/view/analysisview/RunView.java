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

import edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight.HighlightPlugin;
import edu.kit.joana.ui.ifc.sdg.viewer.model.Criteria;
import edu.kit.joana.ui.ifc.sdg.viewer.model.Run;
import edu.kit.joana.ui.ifc.sdg.viewer.view.CompareRunDialog;

public class RunView extends TreeNode implements Parent<CriteriaCategoryView> {

	// the parent
	private ChosenAlgorithmView parent;
	// a wrapper with the concrete algorithm
	private Run run;
	private ArrayList<CriteriaCategoryView> children;

	/** Creates a new Runs object.
	 *
	 * @param alg  The concrete algorithm in a wrapper.
	 */
	public RunView(Run run) {
		this.run = run;
		children = new ArrayList<CriteriaCategoryView>();
	}

	/** Sets the parent to a new value.
	 *
		return (TreeNode [])children.toArray(new TreeNode[children.size()]);
	 * @param parent  The new parent.
	 */
	public void setParent(ChosenAlgorithmView parent) {
		this.parent = parent;
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode#getParent()
	 */
	public ChosenAlgorithmView getParent() {
		return parent;
	}

	/** Returns the children.
	 */
	public CriteriaCategoryView[] getChildren() {
		return children.toArray(new CriteriaCategoryView[children.size()]);
	}

	public IProject getProject() {
        return parent.getProject();
    }

	/** Adds a new child.
	 *
	 * @param child  The new child.
	 */
	public void addChild(CriteriaCategoryView child) {
		children.add(child);
		child.setParent(this);
	}

	/** Removes the given child.
	 */
	public void removeChild(CriteriaCategoryView child) {
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

	/** Starts the algorithm.
	 *
	 */
	public void execute() {
		boolean ok = true;

		for (CriteriaCategoryView cv : children) {
			if (!cv.hasChildren()) {
				ok = false;
			}
		}

		if (ok) {
			new Thread(run).start();

		} else {
			AnalysisView.getInstance().showMessage("You have to define at least 1 criterion per criterion category");
		}
	}

	/** Shows the result of the algorithm.
	 * It uses Kai Brueckners Highlight plugin to visualize the result in the source code.
	 */
	public void showResult() {
		if (run.getResult() == null) {
		    AnalysisView.getInstance().showMessage("Computation not finished yet...");

		} else {
			// get the Highlight plugin
			HighlightPlugin high = HighlightPlugin.getDefault();

			// insert the result into a hash map
			/*Collection<SDGNode> result = run.getResult();
			HashMap<SDGNode, Integer> map = new HashMap<SDGNode, Integer>();

			for (SDGNode n : result) {
				map.put(n, DEPENDENCY);
			}*/

			// get the Java project
			IProject p = parent.getProject();

			// clear old results
			clearResult();

			// call the plugin
			high.highlightJC(p, run.getResult());
		}
	}

	public void compareRuns() {
		CompareRunDialog.create(this);
	}

	public void clearResult() {
		try {
			HighlightPlugin high = HighlightPlugin.getDefault();

			// get the Java project
			IProject p = parent.getProject();

			high.clearHighlight(p, Run.DEPENDENCY);
            high.clearHighlight(p, Run.SOURCE);
            high.clearHighlight(p, Run.TARGET);

		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode#getName()
	 */
	public String getName() {
		return run.getName();
	}

	public Run getRun() {
		return this.run;
	}

	@SuppressWarnings("rawtypes")
    public Enum[] getKindsOfCriteria() {
		return run.getKindsOfCriteria();
	}

	public void setCriteria(Criteria crit, Enum<?> kind) {
		// insert into model
		run.setCriteria(crit, kind);

		// insert into view
		for (CriteriaCategoryView cc : children) {
			if (cc.getName().equals(kind.toString())) {
				CriteriaView cv = new CriteriaView(crit);
				cc.addChild(cv);
			}
		}
	}

	@Override
	public boolean[] getPossibleActions() {
		boolean[] ret = {true, false, false, true, false, true, true, true, true, false, false, false};
		return ret;
	}
}
