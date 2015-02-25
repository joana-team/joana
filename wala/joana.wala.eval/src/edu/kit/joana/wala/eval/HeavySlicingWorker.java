/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval;

import java.io.PrintStream;
import java.nio.CharBuffer;

import edu.kit.joana.wala.eval.util.HeavySlicer;
import edu.kit.joana.wala.eval.util.HeavySlicer.Task;
import edu.kit.joana.wala.util.jobber.Job;
import edu.kit.joana.wala.util.jobber.JobState;
import edu.kit.joana.wala.util.jobber.client.WorkerClient;
import edu.kit.joana.wala.util.jobber.io.RespMessage;
import edu.kit.joana.wala.util.jobber.server.JobberServer;


/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class HeavySlicingWorker extends WorkerClient {

	public static final String TYPE = "heavyslicing";
	private final PrintStream log;
	
	public HeavySlicingWorker(final String serverIp, final int port, final PrintStream log) {
		super(serverIp, port, TYPE);
		this.log = log;
	}
	
	public static void main(final String argv[]) {
		final HeavySlicingWorker worker = new HeavySlicingWorker("localhost", JobberServer.PORT, System.out);
		worker.start();
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.util.jobber.client.WorkerClient#work(edu.kit.joana.wala.util.jobber.Job)
	 */
	@Override
	public JobState work(final Job job) {
		final CharBuffer buf = job.getData();
		final String fileName = buf.toString();
		
		final HeavySlicer hs = new HeavySlicer(log);
		final Task t = hs.createTask(fileName);
		if (t == null) {
			return JobState.FAILED;
		}

		final boolean success = hs.work(t);
		
		return (success ? JobState.DONE : JobState.FAILED);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.util.jobber.client.WorkerClient#displayError(java.lang.String)
	 */
	@Override
	public void displayError(final String msg) {
		log.println(msg);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.util.jobber.client.WorkerClient#displayError(java.lang.Throwable)
	 */
	@Override
	public void displayError(final Throwable t) {
		t.printStackTrace(log);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.util.jobber.client.WorkerClient#displayError(edu.kit.joana.wala.util.jobber.io.RespMessage)
	 */
	@Override
	public void displayError(final RespMessage msg) {
		log.println(msg);
	}

}
