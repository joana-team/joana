/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.List;

import edu.kit.joana.wala.util.jobber.Job;
import edu.kit.joana.wala.util.jobber.JobState;
import edu.kit.joana.wala.util.jobber.io.CmdMessage;
import edu.kit.joana.wala.util.jobber.io.IOUtils;
import edu.kit.joana.wala.util.jobber.io.MessageParseException;
import edu.kit.joana.wala.util.jobber.io.RespMessage;

class CmdInterpreter extends Thread {

	private final JobberServer dispatch;
	private PrintWriter out;
	private BufferedReader in;
	private Socket soc;

	public CmdInterpreter(JobberServer dispatch) {
		this.dispatch = dispatch;
	}

	protected synchronized void setSocket(Socket soc) throws IOException {
		this.soc = soc;
		this.in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
		this.out = new PrintWriter(soc.getOutputStream());
	}

	public void run() {
		try {
			// get command
			CmdMessage cmd = null;
			RespMessage error = null;
			try {
				cmd = IOUtils.readCommand(in);
			} catch (MessageParseException exc) {
				error = RespMessage.error("Parse Command", exc.getMessage());
			}

			// work
			RespMessage resp = null;
			if (error == null) {
				resp = process(cmd);
			} else {
				resp = error;
			}

			// send response
			IOUtils.sendMessage(out, resp);

			// cleanup
			closeStreams();
		} catch (IOException e) {
			dispatch.log(e);
		} finally {
			this.soc = null;
			this.in = null;
			this.out = null;
		}
	}

	private void closeStreams() throws IOException {
		in.close();
		out.close();
		if (!soc.isClosed()) {
			soc.close();
		}
	}

	private RespMessage process(final CmdMessage cmd) {
		RespMessage resp = null;

		try {
			switch (cmd.getCommand()) {
			case CAN_DO:
				resp = registerWorker(cmd);
				break;
			case GRAB_JOB:
				resp = grabJob(cmd);
				break;
			case GET_RESULT:
				resp = getResult(cmd);
				break;
			case CHECK_STATE:
				resp = checkState(cmd);
				break;
			case CHECK_STATES:
				resp = checkStates(cmd);
				break;
			case CANCEL:
				resp = cancelJob(cmd);
				break;
			case SEND_JOB:
				resp = addJob(cmd);
				break;
			case LIST_ALL:
				resp = listAll(cmd);
				break;
			case LIST_WORKING:
				resp = listWorking(cmd);
				break;
			case LIST_RESULTS:
				resp = listResults(cmd);
				break;
			case SEND_RESULT:
				resp = sendResult(cmd);
				break;
			case IM_DONE:
				resp = unregisterWorker(cmd);
				break;
			case SHUT_DOWN_WORKERS:
				resp = shutDownWorkers(cmd);
				break;
			default:
				resp = RespMessage.error("Process Cmd", "Command not implemented: " + cmd);
			}
		} catch (MessageParseException exc) {
			resp = RespMessage.error("Parse Cmd", exc.getMessage());
		}

		return resp;
	}

	private RespMessage checkState(final CmdMessage cmd) throws MessageParseException {
		final int jobId = cmd.getIntArg(0);

		final JobState state = dispatch.checkState(jobId);

		if (state != null) {
			final RespMessage resp = RespMessage.status(state);
			return resp;
		} else {
			return RespMessage.error("Check Status", "No job with id " + jobId + " found");
		}
	}

	private RespMessage checkStates(final CmdMessage cmd) throws MessageParseException {
		final int[] jobIds = cmd.getIntArrayArg(0);

		final JobState[] state = new JobState[jobIds.length];

		for (int i = 0; i < jobIds.length; i++) {
			state[i] = dispatch.checkState(jobIds[i]);

			if (state[i] == null) {
				return RespMessage.error("Check Status", "No job with id " + jobIds[i] + " found");
			}
		}

		final RespMessage resp = RespMessage.statusList(state);

		return resp;
	}

	private RespMessage getResult(final CmdMessage cmd) throws MessageParseException {
		final int jobId = cmd.getIntArg(0);

		final Assignment assign = dispatch.getResult(jobId);

		if (assign != null) {
			final Job job = assign.getJob();
			final RespMessage resp = RespMessage.result(job.getType(), job.getName(), job.getComment(), assign.getState());
			resp.setData(assign.getData());
			return resp;
		} else {
			return RespMessage.error("Get Result", "Job result not found");
		}
	}

	private RespMessage unregisterWorker(final CmdMessage cmd) throws MessageParseException {
		final int wId = cmd.getIntArg(0);

		try {
			dispatch.unregisterWorker(wId);
		} catch (ServerException e) {
			return RespMessage.error("Unregister Worker", e.getMessage());
		}

		return RespMessage.ok();
	}

	private RespMessage shutDownWorkers(final CmdMessage cmd) throws MessageParseException {
		final String type = cmd.getArg(0);

		dispatch.shutDownWorkers(type);

		return RespMessage.ok();
	}

	private RespMessage sendResult(final CmdMessage cmd) throws MessageParseException {
		final int wId = cmd.getIntArg(0);

		final String status = cmd.getArg(1).toUpperCase();
		JobState state = JobState.findState(status);
		if (state == null) {
			return RespMessage.error("Send Result", "Unknown state: " + status);
		}

		try {
			dispatch.finishedAssignment(wId, state, cmd.getData());
		} catch (ServerException e) {
			return RespMessage.error("Dispatcher", e.getMessage());
		}

		return RespMessage.ok();
	}

	private RespMessage listAll(final CmdMessage cmd) {
		final String type = cmd.getArg(0);

		List<Job> jobs = dispatch.listAllQueuedJobs(type);

		final RespMessage resp = RespMessage.list();
		final StringBuilder sb = new StringBuilder();
		for (Job job : jobs) {
			sb.append(job.toString());
			sb.append("\n");
		}
		final CharBuffer data = CharBuffer.wrap(sb.toString().toCharArray());
		resp.setData(data);

		return resp;
	}

	private RespMessage listWorking(final CmdMessage cmd) {
		final String type = cmd.getArg(0);

		List<Assignment> jobs = dispatch.listAllWorking(type);

		final RespMessage resp = RespMessage.list();
		final StringBuilder sb = new StringBuilder();
		for (Assignment job : jobs) {
			sb.append(job.toString());
			sb.append("\n");
		}
		final CharBuffer data = CharBuffer.wrap(sb.toString().toCharArray());
		resp.setData(data);

		return resp;
	}


	private RespMessage listResults(final CmdMessage cmd) {
		final String type = cmd.getArg(0);

		List<Assignment> jobs = dispatch.listAllResults(type);

		final RespMessage resp = RespMessage.list();
		final StringBuilder sb = new StringBuilder();
		for (Assignment job : jobs) {
			sb.append(job.toString());
			sb.append("\n");
		}
		final CharBuffer data = CharBuffer.wrap(sb.toString().toCharArray());
		resp.setData(data);

		return resp;
	}

	private RespMessage cancelJob(final CmdMessage cmd) throws MessageParseException {
		final int jobId = cmd.getIntArg(0);

		final Job job = dispatch.cancelJob(jobId);

		if (job != null) {
			return RespMessage.ok();
		} else {
			return RespMessage.noJob("Job with id " + jobId + " not found");
		}
	}

	private RespMessage addJob(final CmdMessage cmd) {
		final String type = cmd.getArg(0).toLowerCase();
		final String name = cmd.getArg(1);
		final String comment = cmd.getArg(2);

		final Job job = dispatch.newJob(type, name, comment, cmd.getData());

		if (job != null) {
			return RespMessage.newJob(job.getId());
		} else {
			return RespMessage.error("Add Job", "No job created.");
		}
	}

	private RespMessage grabJob(final CmdMessage cmd) throws MessageParseException {
		final int wId = cmd.getIntArg(0);

		Job job = null;
		try {
			job = dispatch.assignNewJobTo(wId);
		} catch (ServerException exc) {
			return RespMessage.error("Dispatcher", exc.getMessage());
		}

		if (job != null) {
			final RespMessage resp =  RespMessage.job(job.getId(), job.getName(), job.getComment());
			resp.setData(job.getData());

			return resp;
		} else {
			return RespMessage.noJob("Sorry, no new job for you.");
		}
	}

	private RespMessage registerWorker(CmdMessage cmd) {
		final String type = cmd.getArg(0);
		final String name = type + "-worker";

		final Worker worker = dispatch.newWorker(name, type);

		if (worker != null) {
			return RespMessage.worker(worker.getId());
		} else {
			return RespMessage.error("Register Worker", "Could not register a new worker.");
		}
	}

}
