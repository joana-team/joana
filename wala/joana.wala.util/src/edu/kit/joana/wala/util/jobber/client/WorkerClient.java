/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Random;

import edu.kit.joana.wala.util.jobber.Job;
import edu.kit.joana.wala.util.jobber.JobState;
import edu.kit.joana.wala.util.jobber.io.CmdMessage;
import edu.kit.joana.wala.util.jobber.io.MessageParseException;
import edu.kit.joana.wala.util.jobber.io.RespMessage;

public abstract class WorkerClient extends JobberClient {

	public static final int SLEEP = 1000;

	private final String type;
	private int id;
	private volatile boolean quit = false;

	public WorkerClient(String serverIp, int port, String type) {
		super(serverIp, port);

		if (type == null || type.isEmpty()) {
			throw new IllegalArgumentException("Type may not be null or empty.");
		}

		this.type = type;
	}

	public abstract JobState work(Job job);

	public abstract void displayError(String msg);
	public abstract void displayError(Throwable t);
	public abstract void displayError(RespMessage msg);

	public final void quit() {
		this.quit = true;
	}

	public final void run() {
		final Random rand = new Random();

		try {
			registerMe();

			while (!quit) {
				// poll for job
				Job job = grabJob();

				if (job != null) {
					// work job
					workJob(job);
				}

				// randomize sleep time for better load distribution
				final int nextSleepTime = SLEEP + rand.nextInt(SLEEP);
				sleep(nextSleepTime);
			}

			iAmDone();
		} catch (UnknownHostException e) {
			displayError(e);
		} catch (IOException e) {
			displayError(e);
		} catch (NumberFormatException e) {
			displayError(e);
		} catch (InterruptedException e) {
			displayError(e);
		} catch (MessageParseException e) {
			displayError(e);
		}
	}

	private void workJob(Job job) throws UnknownHostException, IOException, MessageParseException {
		if (job.getId() == Job.CANCEL_ID) {
			quit();
			return;
		}

		final JobState state = work(job);

		final CmdMessage sendResult = CmdMessage.sendResult(this.id, state);
		sendResult.setData(job.getData());

		final RespMessage resp = send(sendResult);

		if (resp.getResponse() != RespMessage.Response.OK) {
			displayError("Expected ok message but got: " + resp);
		}
	}

	private void registerMe() throws UnknownHostException, IOException, MessageParseException {
		CmdMessage canDo = CmdMessage.canDo(type);
		RespMessage resp = send(canDo);
		if (resp.isError()) {
			throw new IllegalStateException(resp.getData().toString());
		}

		if (resp.getResponse() == RespMessage.Response.WORKER) {
			final int workerId = resp.getIntArg(0);
			this.id = workerId;
		} else {
			throw new IllegalStateException("Unexpected response: " + resp.getResponse());
		}

		System.out.println("I am worker no " + id);
	}


	private Job grabJob() throws IOException, MessageParseException {
		Job job = null;

		final CmdMessage grabJob = CmdMessage.grabJob(this.id);
		final RespMessage resp = send(grabJob);

		if (!resp.isError() && resp.getResponse() == RespMessage.Response.JOB) {
			final int jobId = resp.getIntArg(0);
			final String name = resp.getArg(1);
			final String comment = resp.getArg(2);

			job = new Job(jobId, type, name, comment, resp.getData());
		} else if (!resp.isError() && resp.getResponse() == RespMessage.Response.NO_JOB) {
			// no job found -> thats ok, so we do nothing
		} else if (resp.isError()) {
			displayError(resp);
		} else {
			displayError("Unexpected response from grab_job: " + resp.getResponse());
		}

		return job;
	}

	private RespMessage iAmDone() throws UnknownHostException, IOException, MessageParseException {
		final CmdMessage imDone = CmdMessage.imDone(this.id);
		return send(imDone);
	}

	public String toString() {
		return "Worker(" + id + ")[" + type + "]" + super.toString();
	}
}
