/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.io;

import edu.kit.joana.wala.util.jobber.JobState;


public final class CmdMessage extends Message {

	public enum Command {

		/* worker commands */
		CAN_DO(1), GRAB_JOB(1), IM_DONE(1), SEND_RESULT(2, true),

		/* client commands */
		SEND_JOB(3, true), GET_RESULT(1), CHECK_STATE(1), CHECK_STATES(1), LIST_ALL(1),
		LIST_WORKING(1), LIST_RESULTS(1), CANCEL(1), SHUT_DOWN_WORKERS(1);

		final int args;
		final boolean hasData;

		private Command(int args) {
			this(args, false);
		}

		private Command(int args, boolean hasData) {
			this.args = args;
			this.hasData = hasData;
		}
	}

	private final Command cmd;

	private CmdMessage(Command cmd, String ... args) {
		super(args);
		this.cmd = cmd;
	}

	@Override
	public String getTypeName() {
		return cmd.name();
	}

	@Override
	public boolean hasData() {
		return cmd.hasData;
	}

	public Command getCommand() {
		return cmd;
	}

	public static CmdMessage parse(final String str) throws MessageParseException {
		final String[] tokens = str.split(SEPERATOR);
		final String cmdTok = tokens[0].trim().toUpperCase();

		Command cmd = findCommand(cmdTok);

		if (cmd == null) {
			throw new MessageParseException("Unknown command: " + cmdTok);
		}

		// + 1 for  prefix, + 1 for terminator
		checkArgs(tokens, cmd.args + 1 + 1);

		String[] args = new String[cmd.args];
		for (int i = 0; i < args.length; i++) {
			args[i] = tokens[i + 1].trim();
		}

		return new CmdMessage(cmd, args);
	}

	private static Command findCommand(final String cmd) {
		for (Command c : Command.values()) {
			if (c.name().equals(cmd)) {
				return c;
			}
		}

		return null;
	}

	public static CmdMessage canDo(final String type) {
		final CmdMessage canDo = new CmdMessage(Command.CAN_DO, type);
		return canDo;
	}

	public static CmdMessage grabJob(final int workerId) {
		final CmdMessage grabJob = new CmdMessage(Command.GRAB_JOB, "" + workerId);
		return grabJob;
	}

	public static CmdMessage imDone(final int workerId) {
		final CmdMessage imDone = new CmdMessage(Command.IM_DONE, "" + workerId);
		return imDone;
	}

	public static CmdMessage sendResult(final int workerId, JobState state) {
		final CmdMessage sendResult = new CmdMessage(Command.SEND_RESULT, "" + workerId, state.name());
		return sendResult;
	}

	public static CmdMessage sendJob(final String type, final String name, final String comment) {
		final CmdMessage sendJob = new CmdMessage(Command.SEND_JOB, type, name, comment);
		return sendJob;
	}

	public static CmdMessage checkStatus(final int jobId) {
		final CmdMessage check = new CmdMessage(Command.CHECK_STATE, "" + jobId);
		return check;
	}

	public static CmdMessage checkStatus(final int[] jobIds) {
		String jobs = Message.createIntArrayArg(jobIds);

		final CmdMessage check = new CmdMessage(Command.CHECK_STATES, jobs);
		return check;
	}

	public static CmdMessage getResult(final int jobId) {
		final CmdMessage getResult = new CmdMessage(Command.GET_RESULT, "" + jobId);
		return getResult;
	}

	public static CmdMessage cancel(final int jobId) {
		final CmdMessage getResult = new CmdMessage(Command.CANCEL, "" + jobId);
		return getResult;
	}

	public static CmdMessage listAll(final String type) {
		final CmdMessage listAll = new CmdMessage(Command.LIST_ALL, type);
		return listAll;
	}

	public static CmdMessage listWorking(final String type) {
		final CmdMessage listAll = new CmdMessage(Command.LIST_WORKING, type);
		return listAll;
	}

	public static CmdMessage listResults(final String type) {
		final CmdMessage listAll = new CmdMessage(Command.LIST_RESULTS, type);
		return listAll;
	}

	public static CmdMessage shutDownWorkers(String type) {
		final CmdMessage shutDown = new CmdMessage(Command.SHUT_DOWN_WORKERS, type);
		return shutDown;
	}


}
