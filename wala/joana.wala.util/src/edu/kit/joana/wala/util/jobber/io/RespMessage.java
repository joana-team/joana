/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.io;

import edu.kit.joana.wala.util.jobber.JobState;




public class RespMessage extends Message {

	public enum Response {

		WORKER(1), JOB(3, true), ERROR(2), NO_JOB(1), NEW_JOB(1), OK(0), RESULT(4, true),
		STATUS(1), STATUS_LIST(1), LIST(0, true);

		final int args;
		final boolean hasData;

		private Response(int args) {
			this(args, false);
		}

		private Response(int args, boolean hasData) {
			this.args = args;
			this.hasData = hasData;
		}
	}

	private final Response resp;

	private RespMessage(Response resp, String ... args) {
		super(args);
		this.resp = resp;
	}

	@Override
	public String getTypeName() {
		return resp.name();
	}

	@Override
	public boolean hasData() {
		return resp.hasData;
	}

	public Response getResponse() {
		return resp;
	}

	public boolean isError() {
		return resp == Response.ERROR;
	}

	public static RespMessage parse(String str) throws MessageParseException {
		final String[] tokens = str.split(SEPERATOR);
		final String cmdTok = tokens[0].trim().toUpperCase();

		final Response type = findType(cmdTok);

		if (type == null) {
			throw new MessageParseException("Unknown response: " + cmdTok);
		}

		// +1 for prefix, + 1 for terminator
		checkArgs(tokens, type.args + 1 + 1);

		final String[] args = new String[type.args];
		for (int i = 0; i < args.length; i++) {
			args[i] = tokens[i + 1].trim();
		}

		return new RespMessage(type, args);
	}

	private static Response findType(String str) {
		for (RespMessage.Response type : RespMessage.Response.values()) {
			if (type.name().equals(str)) {
				return type;
			}
		}

		return null;
	}

	public static RespMessage error(final String name, final String message) {
		final RespMessage err = new RespMessage(Response.ERROR, name, message);
		return err;
	}

	public static RespMessage worker(final int id) {
		final RespMessage worker = new RespMessage(Response.WORKER, "" + id);
		return worker;
	}

	public static RespMessage job(final int id, final String name, final String comment) {
		final RespMessage job = new RespMessage(Response.JOB, "" + id, name, comment);
		return job;
	}

	public static RespMessage noJob(final String comment) {
		final RespMessage job = new RespMessage(Response.NO_JOB, comment);
		return job;
	}

	public static RespMessage newJob(int id) {
		final RespMessage job = new RespMessage(Response.NEW_JOB, "" + id);
		return job;
	}

	public static RespMessage ok() {
		final RespMessage ok = new RespMessage(Response.OK);
		return ok;
	}

	public static RespMessage list() {
		final RespMessage list = new RespMessage(Response.LIST);
		return list;
	}

	public static RespMessage result(final String type, final String name, final String comment, final JobState state) {
		final RespMessage ok = new RespMessage(Response.RESULT, type, name, comment, state.name());
		return ok;
	}

	public static RespMessage status(JobState state) {
		final RespMessage ok = new RespMessage(Response.STATUS, state.name());
		return ok;
	}

	public static RespMessage statusList(JobState[] state) {
		final String jobStates = Message.createJobStateArrayArg(state);
		final RespMessage ok = new RespMessage(Response.STATUS_LIST, jobStates);
		return ok;
	}

}
