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
package edu.kit.joana.ui.ifc.wala.console.gui;

import javax.swing.ProgressMonitor;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

public class SwingProgressMonitorDelegate implements IProgressMonitor {

	private final ProgressMonitor delegate;
	private int workDone;

	public SwingProgressMonitorDelegate(ProgressMonitor delegate) {
		this.delegate = delegate;
	}

	@Override
	public void beginTask(String task, int totalWork) {
		delegate.setMinimum(0);
		delegate.setMaximum(totalWork);
		delegate.setMillisToDecideToPopup(0);
		delegate.setMillisToPopup(0);
		this.workDone = 0;
		worked(1);
	}

	@Override
	public void subTask(String subTask) {
		delegate.setNote(subTask);
	}

	@Override
	public void cancel() {

	}

	@Override
	public boolean isCanceled() {
		return delegate.isCanceled();
	}

	@Override
	public void done() {
		delegate.close();
	}

	@Override
	public void worked(int units) {
		workDone += units;
		delegate.setProgress(workDone);
	}

}
