/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.server;

import java.nio.CharBuffer;

import edu.kit.joana.wala.util.jobber.Job;
import edu.kit.joana.wala.util.jobber.JobState;

class Assignment {

	private final Job job;
	private final Worker worker;
	private JobState state = JobState.NEW;
	private CharBuffer data = null;

	protected Assignment(Job job, Worker worker) {
		if (job == null || worker == null) {
			throw new IllegalArgumentException("No null values allowed here.");
		}

		this.job = job;
		this.worker = worker;
	}

	protected Job getJob() {
		return job;
	}

	protected Worker getWorker() {
		return worker;
	}

	protected synchronized JobState getState() {
		return state;
	}

	protected synchronized void setRunning() {
		if (this.state != JobState.NEW) {
			throw new IllegalStateException("Assignment is not new");
		}

		this.state = JobState.RUNNING;
	}

	protected synchronized void setDone() {
		if (this.state != JobState.RUNNING) {
			throw new IllegalStateException("Assignment is not new");
		}

		this.state = JobState.DONE;
	}

	protected synchronized void setFailed() {
		if (this.state != JobState.RUNNING) {
			throw new IllegalStateException("Assignment is not new");
		}

		this.state = JobState.FAILED;
	}

	protected void setData(CharBuffer cbuf) {
		this.data = cbuf;
	}

	protected CharBuffer getData() {
		return this.data;
	}

	public String toString() {
		return worker.toString() + " -> " + job.toString();
	}
}
