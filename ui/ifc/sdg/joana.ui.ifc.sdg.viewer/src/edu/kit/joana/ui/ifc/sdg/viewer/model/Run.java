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
import java.util.List;
import java.util.Map;
import java.util.Observable;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ui.ifc.sdg.viewer.Activator;
import edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview.AnalysisView;



public abstract class Run extends Observable implements Runnable {
	private static int RUN = 1;

	// marker colors
    public static final int DEPENDENCY = 0;
    public static final int TARGET = -1;
    public static final int SOURCE = -2;
    public static final int BARRIER = -3;

	// the parent
	private ChosenAlgorithm parent;
	// contains the result of the analysis
	private ArrayList<CriteriaCategory> children;
	private int runID;

	private boolean executing;
	private boolean finished;

	/** Creates a new Runs object.
	 *
	 * @param alg  The concrete algorithm in a wrapper.
	 */
	public Run() {
		 children = new ArrayList<CriteriaCategory>();
		 runID = RUN;
		 RUN++;
		 executing = false;
		 finished = false;
	}

	/** Sets the parent to a new value.
	 *
	 * @param parent  The new parent.
	 */
	public void setParent(ChosenAlgorithm parent) {
		this.parent = parent;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode#getParent()
	 */
	public ChosenAlgorithm getParent() {
		return parent;
	}

	public edu.kit.joana.ui.ifc.sdg.viewer.algorithms.Algorithm getAlgorithm() {
		return parent.getAlgorithm();
	}

	/** Returns the name of the used algorithm.
	 *
	 */
	public String getName() {
		return "Run " + runID;
	}


	public Graph getGraph() {
		return parent.getParent();
	}

	/** Adds a new Child
	 *
	 * @param child  The new child.
	 */
	public void addChild(CriteriaCategory child) {
		children.add(child);
		child.setParent(this);
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#removeChild(edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.TreeNode)
	 */
	public void removeChild(CriteriaCategory child) {
		children.remove(child);
		child.setParent(null);
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#getChildren()
	 */
	public List<CriteriaCategory> getChildren() {
		return children;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.ui.ifc.sdg.viewer.view.AnalysisView.Parent#hasChildren()
	 */
	public boolean hasChildren() {
		return true;
	}


	/** With this method a user can add nodes to a certain kind of criteria.
	 *
	 * @param crit  The criteria to add.
	 * @param kind  The category where the new criteria shall be added to.
	 */
	public void setCriteria(Criteria crit, Enum<?> kind) {
		for (CriteriaCategory cc : children) {
			if (cc.getName().equals(kind.toString())) {
				cc.addChild(crit);
			}
		}
	}

	/** Starts the analysis in a separate thread.
	 * It calls method execute().
	 */
	public void run() {
		//Execute sache
		executing = true;
		Activator.getDefault().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				AnalysisView.getInstance().refresh();
			}
		});
		convertCriteria();
		execute();
		setChanged();
		notifyObservers();
		executing = false;
		finished = true;
		Activator.getDefault().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				AnalysisView.getInstance().refresh();
			}
		});
	}

	public boolean isExecuting() {
		return executing;
	}

	public boolean isFinished() {
		return finished;
	}


	/****************************************************************
	 *                                                              *
	 * Abstract part - has to be implemented by algorithm adapters. *
	 * See Slicer.java or Chopper.java for examples.                *
	 *                                                              *
	 ****************************************************************/

	/** Runs the analysis.
	 * Implementations should
	 * 1. Run the analysis and save the result in the field result.
	 * 2. Notify the observer with observer.update().
	 */
	public abstract void execute();

	/** Returns the result or null.
	 * Should return null in case that the analysis is not started or not finished.
	 */
	public abstract Map<SDGNode, Integer> getResult();

	/** Returns the possible kinds of criteria of the used algorithm.
	 * This method is used to parameterize the different kinds of criteria that
	 * different categories of algorithms have.
	 * For example, slicer have 1 kind of criteria, the slicing criterion.
	 * Chopper have 2 kinds of criteria, the source and the sink criterion.
	 *
	 * @return  The kinds of criteria as an array of enums.
	 */
	@SuppressWarnings("unchecked")
    public abstract Enum[] getKindsOfCriteria();

	public abstract void convertCriteria();

}
