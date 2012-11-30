/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.client;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

import edu.kit.joana.wala.util.jobber.Job;
import edu.kit.joana.wala.util.jobber.JobState;
import edu.kit.joana.wala.util.jobber.io.CmdMessage;
import edu.kit.joana.wala.util.jobber.io.MessageParseException;
import edu.kit.joana.wala.util.jobber.io.RespMessage;

/**
 * A ManagerClient manages jobs that are distributed by the JobberServer. It submits new jobs and also retrieves
 * its results and combines them to the final overall result.
 * Normally there is a single Manager for a certain type of job and a bunch of workers that work on them.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public abstract class ManagerClient extends JobberClient {

	public ManagerClient(String serverIp, int port) {
		super(serverIp, port);
	}

	public abstract void displayError(String msg);
	public abstract void displayError(RespMessage msg);

	@Override
	public void run() {
		throw new IllegalStateException("Dont know what to do. Please overwrite run() method before starting this thread.");
	}

	/**
	 * Sends a new Job to the JobberServer.
	 *
	 * @param type The type of the job. This String is used to identify which workers can handle this job.
	 * @param name A name of the job. May be chosen freely.
	 * @param comment A comment about the job.
	 * @param data Additional data that is sent with the Job. May contain special arguments or even whole files.
	 * @return A new Job Object containing the id that has been assigned to the job by the server. Returns null, if the job could not
	 * be sent.
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws MessageParseException
	 */
	public Job sendJob(final String type, final String name, final String comment,
			final CharBuffer data) throws UnknownHostException, IOException, MessageParseException {
		if (type == null || type.isEmpty()) {
			throw new IllegalArgumentException("type may not be null or empty");
		} else if (name == null) {
			throw new IllegalArgumentException("name may not be null");
		} else if (comment == null) {
			throw new IllegalArgumentException("comment may not be null");
		}

		final CmdMessage sendJob = CmdMessage.sendJob(type, name, comment);
		sendJob.setData(data);

		RespMessage resp = send(sendJob);

		Job newJob = null;
		if (!resp.isError() && resp.getResponse() == RespMessage.Response.NEW_JOB) {
			final int jobId = resp.getIntArg(0);

			newJob = new Job(jobId, type, name, comment, data);
		} else if (!resp.isError()) {
			displayError("Unexpected response: " + resp);
		} else {
			displayError(resp);
		}

		return newJob;
	}

	/**
	 * Retrieves the status of a given job.
	 * @param jobId The id of the job.
	 * @return Status of the job. Returns JobState.UNKNOWN on error.
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws MessageParseException
	 */
	public JobState checkStatus(final int jobId) throws UnknownHostException, IOException, MessageParseException {
		JobState state = JobState.UNKNOWN;

		final CmdMessage checkStatus = CmdMessage.checkStatus(jobId);

		final RespMessage resp = send(checkStatus);

		if (!resp.isError() && resp.getResponse() == RespMessage.Response.STATUS) {
			state = resp.getJobStateArg(0);
		} else if (!resp.isError()) {
			displayError("Unexpected response: " + resp);
		} else {
			displayError(resp);
		}

		return state;
	}

	/**
	 * Tells the server to issue an cancel command to each worker of the given type that is currently registered at the server.
	 * The next time a worker asks the server for a new job, it will retrieve a cancel message instead and subsequently shut
	 * down itself. This can be used to terminate all worker threads when the overall solution has been computed.
	 * @param type The type of the workers that should be canceled.
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws MessageParseException
	 */
	public void shutDownWorkers(String type) throws UnknownHostException, IOException, MessageParseException {
		final CmdMessage shutDown = CmdMessage.shutDownWorkers(type);

		final RespMessage resp = send(shutDown);

		if (!resp.isError() && resp.getResponse() == RespMessage.Response.OK) {
			// all fine
		} else if (!resp.isError()) {
			displayError("Unexpected response: " + resp);
		} else {
			displayError(resp);
		}
	}


	/**
	 * Retrieves the status of a list of jobs.
	 * @param jobIds An array of job ids.
	 * @return An array containing the status of those jobs. Returns null if an error occurred.
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws MessageParseException
	 */
	public JobState[] checkStatus(final int[] jobIds) throws UnknownHostException, IOException, MessageParseException {
		JobState[] state = null;

		final CmdMessage checkStatus = CmdMessage.checkStatus(jobIds);

		final RespMessage resp = send(checkStatus);

		if (!resp.isError() && resp.getResponse() == RespMessage.Response.STATUS_LIST) {
			state = resp.getJobStateArrayArg(0);
		} else if (!resp.isError()) {
			displayError("Unexpected response: " + resp);
		} else {
			displayError(resp);
		}

		return state;
	}

	/**
	 * Issues a cancel command for a given job. If the job is registered on the server and
	 * no worker is currently working on it, it will be removed from the list.
	 * @param jobId The id of the job to cancel.
	 * @return true if the job was canceled.
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws MessageParseException
	 */
	public boolean cancelJob(final int jobId) throws UnknownHostException, IOException, MessageParseException {
		boolean ok = false;

		final CmdMessage cancel = CmdMessage.cancel(jobId);

		final RespMessage resp = send(cancel);

		if (!resp.isError() && resp.getResponse() == RespMessage.Response.OK) {
			ok = true;
		} else if (!resp.isError() && resp.getResponse() == RespMessage.Response.NO_JOB) {
			ok = false;
		} else if (!resp.isError()) {
			displayError("Unexpected response: " + resp);
		} else {
			displayError(resp);
		}

		return ok;
	}

	/**
	 * Retrieves the result of a finished job from the server. This response contains the status of the
	 * job (failed or not) as well as optional raw data.
	 * @param jobId Id of the job.
	 * @return A job object containing the status and the optional response data. Returns null if an error occurred.
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws MessageParseException
	 */
	public Job getResult(final int jobId) throws UnknownHostException, IOException, MessageParseException {
		Job newJob = null;

		final CmdMessage getResult = CmdMessage.getResult(jobId);

		final RespMessage resp = send(getResult);

		if (!resp.isError() && resp.getResponse() == RespMessage.Response.RESULT) {
			final String jobType = resp.getArg(0);
			final String jobName = resp.getArg(1);
			final String jobComment = resp.getArg(2);
			final String stateStr = resp.getArg(3).toUpperCase();
			final JobState state = JobState.findState(stateStr);

			newJob = new Job(jobId, jobType, jobName, jobComment, resp.getData(), state);
		} else if (!resp.isError()) {
			displayError("Unexpected response: " + resp);
		} else {
			displayError(resp);
		}

		return newJob;
	}

	/**
	 * Retrieves a list of all jobs of a certain type that are currently in the joblist of the server. This list
	 * does not include jobs that are currently assigned to a worker.
	 *
	 * @param type Type of the jobs to list.
	 * @return A list of String representations of the jobs in the queue. Contains job ids, names and comments.
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws MessageParseException
	 */
	public List<String> listAll(final String type) throws UnknownHostException, IOException, MessageParseException {
		if (type == null) {
			throw new IllegalArgumentException("type may not be null");
		}

		List<String> jobs = null;

		final CmdMessage listAll = CmdMessage.listAll(type);

		final RespMessage resp = send(listAll);

		if (!resp.isError() && resp.getResponse() == RespMessage.Response.LIST) {
			final CharBuffer data = resp.getData();
			jobs = new ArrayList<String>(data.length());
			char[] buf = new char[data.length()];
			final int savedPos = data.position();
			data.get(buf);
			data.position(savedPos);
			final CharArrayReader cRead = new CharArrayReader(buf);
			final BufferedReader bIn = new BufferedReader(cRead);

			while (bIn.ready()) {
				final String line = bIn.readLine();
				jobs.add(line);
			}
		}

		return jobs;
	}

	/**
	 * Retrieves a list of all jobs of a certain type that are currently assigned to a worker. This list
	 * does not include jobs that are in the queue or already finished.
	 *
	 * @param type Type of the jobs to list.
	 * @return A list of String representations of the jobs that are assigned to a worker. Contains job ids, names and comments.
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws MessageParseException
	 */
	public List<String> listWorking(final String type) throws UnknownHostException, IOException, MessageParseException {
		if (type == null) {
			throw new IllegalArgumentException("type may not be null");
		}

		List<String> jobs = null;

		final CmdMessage listAll = CmdMessage.listWorking(type);

		final RespMessage resp = send(listAll);

		if (!resp.isError() && resp.getResponse() == RespMessage.Response.LIST) {
			final CharBuffer data = resp.getData();
			jobs = new ArrayList<String>(data.length());
			char[] buf = new char[data.length()];
			final int savedPos = data.position();
			data.get(buf);
			data.position(savedPos);
			final CharArrayReader cRead = new CharArrayReader(buf);
			final BufferedReader bIn = new BufferedReader(cRead);

			while (bIn.ready()) {
				final String line = bIn.readLine();
				jobs.add(line);
			}
		}

		return jobs;
	}

	/**
	 * Retrieves a list of all jobs of a certain type that are finished.
	 *
	 * @param type Type of the jobs to list.
	 * @return A list of String representations of the jobs that are finished. Contains job ids, names and comments.
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws MessageParseException
	 */
	public List<String> listResults(final String type) throws UnknownHostException, IOException, MessageParseException {
		if (type == null) {
			throw new IllegalArgumentException("type may not be null");
		}

		List<String> jobs = null;

		final CmdMessage listAll = CmdMessage.listResults(type);

		final RespMessage resp = send(listAll);

		if (!resp.isError() && resp.getResponse() == RespMessage.Response.LIST) {
			final CharBuffer data = resp.getData();
			jobs = new ArrayList<String>(data.length());
			char[] buf = new char[data.length()];
			final int savedPos = data.position();
			data.get(buf);
			data.position(savedPos);
			final CharArrayReader cRead = new CharArrayReader(buf);
			final BufferedReader bIn = new BufferedReader(cRead);

			while (bIn.ready()) {
				final String line = bIn.readLine();
				jobs.add(line);
			}
		}

		return jobs;
	}

}
