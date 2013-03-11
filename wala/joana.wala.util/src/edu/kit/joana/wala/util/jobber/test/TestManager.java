/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.test;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import edu.kit.joana.wala.util.jobber.JobState;
import edu.kit.joana.wala.util.jobber.client.ManagerClient;
import edu.kit.joana.wala.util.jobber.io.MessageParseException;
import edu.kit.joana.wala.util.jobber.io.RespMessage;
import edu.kit.joana.wala.util.jobber.server.JobberServer;

public class TestManager extends ManagerClient {

	public TestManager(String serverIp, int port) {
		super(serverIp, port);
	}

	public static void main(String[] argv) throws UnknownHostException, IOException, MessageParseException {
		TestManager tm = new TestManager("localhost", JobberServer.PORT);

//		for (int i = 0; i < 35; i++) {
//			final CharBuffer buf = CharBuffer.wrap("Hallo Welt");
//			final Job job = tm.sendJob(TestWorker.TYPE, "MyJob", "No " + i, buf);
//			System.out.println("Sent job - got id " + job.getId());
//		}

//		tm.doListAll("");
//		tm.doListWorking("");
//		tm.doListResult("");
		tm.doListAll(TestWorker.TYPE);
//		tm.doListAll("huhu");


		if (tm.cancelJob(22)) {
			System.out.println("Job canceled");
		} else {
			System.out.println("Job not canceled");
		}

		if (tm.cancelJob(22)) {
			System.out.println("Job canceled");
		} else {
			System.out.println("Job not canceled");
		}


//		JobState state = JobState.NEW;
//
//		while (state != JobState.DONE && state != JobState.FAILED && state != JobState.UNKNOWN) {
//			state = tm.checkJob(33);
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {}
//		}
	}

	private void doListAll(final String type) throws UnknownHostException, IOException, MessageParseException {
		final List<String> list = listAll(type);
		System.out.println("List all (" + type + "):");
		for (String item : list) {
			System.out.println("\t" + item);
		}
	}

	@SuppressWarnings("unused")
	private void doListWorking(final String type) throws UnknownHostException, IOException, MessageParseException {
		final List<String> list = listWorking(type);
		System.out.println("List working (" + type + "):");
		for (String item : list) {
			System.out.println("\t" + item);
		}
	}

	@SuppressWarnings("unused")
	private void doListResult(final String type) throws UnknownHostException, IOException, MessageParseException {
		final List<String> list = listResults(type);
		System.out.println("List results (" + type + "):");
		for (String item : list) {
			System.out.println("\t" + item);
		}
	}

	@SuppressWarnings("unused")
	private JobState checkJob(int id) throws UnknownHostException, IOException, MessageParseException {
		JobState state = checkStatus(id);
		System.out.println("State of job no. " + id + " is " + state);

		return state;
	}

	@Override
	public void displayError(String msg) {
		System.out.println("ERR: " + msg);
	}

	@Override
	public void displayError(RespMessage msg) {
		System.out.println(msg);
	}

}
