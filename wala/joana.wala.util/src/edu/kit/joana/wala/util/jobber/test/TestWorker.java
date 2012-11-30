/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.test;

import java.nio.CharBuffer;

import edu.kit.joana.wala.util.jobber.Job;
import edu.kit.joana.wala.util.jobber.JobState;
import edu.kit.joana.wala.util.jobber.client.WorkerClient;
import edu.kit.joana.wala.util.jobber.io.RespMessage;
import edu.kit.joana.wala.util.jobber.server.JobberServer;

public class TestWorker extends WorkerClient {

	public static final String TYPE = "test";

	public TestWorker(String serverIp, int port) {
		super(serverIp, port, TYPE);
	}

	public static void main(String[] args) {
		for (int i = 0; i < 4; i++) {
			TestWorker wc = new TestWorker("localhost", JobberServer.PORT);
			wc.start();
		}
	}

	@Override
	public JobState work(Job job) {
		final CharBuffer buf = job.getData();
		System.out.println(this.toString() + " working on " + job + ": " + buf.toString());
		return JobState.DONE;
	}

	@Override
	public void displayError(String msg) {
		System.out.println("ERR: " + msg);
	}

	@Override
	public void displayError(RespMessage msg) {
		System.out.println("ERR: " + msg);
	}

	@Override
	public void displayError(Throwable t) {
		t.printStackTrace();
	}

}
