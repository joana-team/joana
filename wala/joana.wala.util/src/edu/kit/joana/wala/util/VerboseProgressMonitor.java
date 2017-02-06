/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util;

import java.io.PrintStream;
import java.time.Instant;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;


/**
 *
 * @author Juergen Graf <grafj@ipd.info.uni-karlsruhe.de>
 *
 */
public class VerboseProgressMonitor implements IProgressMonitor {

	private final PrintStream out;
	private boolean canceled = false;
	private boolean isWorking = false;
	private long beginTime = -1;

	public VerboseProgressMonitor(PrintStream out) {
		this.out = out;
	}

	public void beginTask(String name, int totalWork) {
		if (isWorking) {
			out.println();
		}
		out.print("[Task @ " + Instant.now() + "] " + name);
		out.flush();
		beginTime = System.currentTimeMillis();
		
		isWorking = true;
	}

	public void done() {
		if (!isWorking) {
			out.print("[Task]");
		}
		isWorking = false;
		out.println();
		out.println(" done [@ " + Instant.now() + ", " + (System.currentTimeMillis() - beginTime) + "ms]");
		out.flush();
		beginTime = -1;
	}

	public void internalWorked(double work) {
		out.print('.');
		out.flush();
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void cancel() {
		if (!canceled) {
			isWorking = false;
			out.println();
			out.println(" canceled. [@ " + Instant.now() + ", " + (System.currentTimeMillis() - beginTime) + "ms]");
			out.flush();
		}
		canceled = true;
		beginTime = -1;
	}

	public void setTaskName(String name) {
	}

	public void subTask(String name) {
		if (!isWorking) {
			isWorking = true;
		} else {
			out.println();
		}

		out.print("[SubTask] " + name + " ");
		out.flush();
	}

	public void worked(int work) {
		out.print('.');
		out.flush();
	}

	@Override
	public String getCancelMessage() {
		return "canceled.";
	}

}
