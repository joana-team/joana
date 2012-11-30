/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.io.PrintStream;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

public class VerboseProgressMonitor implements IProgressMonitor {

	private PrintStream out;
	private int total = 0;
	private boolean cancel = false;

	public VerboseProgressMonitor(PrintStream out) {
		this.out = out;
	}
	@Override
	public void beginTask(String task, int totalWork) {
		out.println("[Task] "+task);
		total += totalWork;
	}

	@Override
	public void subTask(String subTask) {
		out.println("[Task] "+subTask);
	}

	@Override
	public boolean isCanceled() {
		return cancel;
	}

	@Override
	public void done() {
		out.println("done.");
	}

	@Override
	public void worked(int units) {

	}

	@Override
	public void cancel() {
		cancel = true;
	}

}
