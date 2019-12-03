/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

public class NullProgressMonitor implements IProgressMonitor {

	public static NullProgressMonitor INSTANCE = new NullProgressMonitor();

	private boolean cancel = false;

	public NullProgressMonitor() {}

	@Override
	public void beginTask(String task, int totalWork) {
	}

	@Override
	public void subTask(String subTask) {
	}

	@Override
	public boolean isCanceled() {
		return cancel;
	}

	@Override
	public void cancel() {
		cancel = true;
	}

	@Override
	public void done() {
	}

	@Override
	public void worked(int units) {
	}

	@Override
	public String getCancelMessage() {
		return "canceled.";
	}

}
