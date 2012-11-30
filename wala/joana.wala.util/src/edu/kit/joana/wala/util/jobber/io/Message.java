/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.io;

import java.nio.CharBuffer;
import java.util.StringTokenizer;

import edu.kit.joana.wala.util.jobber.JobState;

public abstract class Message {

	public static final String SEPERATOR = ":";
	public static final String TERMINATOR = ";";
	private static final String ARRAY_FIELD_SEPERATOR = ",";


	private final String[] args;
	private CharBuffer data;

	Message(String ... args) {
		this.args = new String[args.length + 1];

		for (int i = 0; i < args.length; i++ ) {
			this.args[i] = args[i];
		}
		this.args[args.length] = TERMINATOR;

		this.data = null;

		try {
			checkArgs(this.args, this.args.length);
		} catch (MessageParseException exc) {
			throw new IllegalArgumentException(exc);
		}
	}

	public abstract String getTypeName();
	public abstract boolean hasData();

	public String getHeaderStr() {
		String cmdLine = getTypeName();
		for (String str : args) {
			cmdLine += SEPERATOR + str;
		}

		return cmdLine;
	}

	public String getArg(int num) {
		return args[num];
	}

	public int getIntArg(int num) throws MessageParseException {
		try {
			final String argStr = args[num];
			final int argInt = Integer.parseInt(argStr);

			return argInt;
		} catch (NumberFormatException exc) {
			throw new MessageParseException(exc);
		}
	}

	public int[] getIntArrayArg(int num) throws MessageParseException {
		try {
			final String argStr = args[num];

			StringTokenizer tok = new StringTokenizer(argStr, ARRAY_FIELD_SEPERATOR);
			int[] array = new int[tok.countTokens()];

			for (int i = 0; i < array.length; i++) {
				String token = tok.nextToken();
				array[i] = Integer.parseInt(token);
			}

			return array;
		} catch (NumberFormatException exc) {
			throw new MessageParseException(exc);
		}
	}

	public JobState getJobStateArg(int num) {
		final String stateStr = args[num].toUpperCase();
		return JobState.findState(stateStr);
	}

	public JobState[] getJobStateArrayArg(int num) throws MessageParseException {
		try {
			final String argStr = args[num];

			StringTokenizer tok = new StringTokenizer(argStr, ARRAY_FIELD_SEPERATOR);
			JobState[] array = new JobState[tok.countTokens()];

			for (int i = 0; i < array.length; i++) {
				String token = tok.nextToken();
				array[i] = JobState.findState(token.toUpperCase());
			}

			return array;
		} catch (NumberFormatException exc) {
			throw new MessageParseException(exc);
		}
	}

	public static String createIntArrayArg(int[] array) {
		String args = "";
		for (int i = 0; i < array.length; i++) {
			args += array[i];
			if (i != array.length - 1) {
				args += ARRAY_FIELD_SEPERATOR;
			}
		}

		return args;
	}

	public static String createJobStateArrayArg(JobState[] array) {
		String args = "";
		for (int i = 0; i < array.length; i++) {
			args += array[i];
			if (i != array.length - 1) {
				args += ARRAY_FIELD_SEPERATOR;
			}
		}

		return args;
	}

	public int getNumArgs() {
		return args.length;
	}

	public CharBuffer getData() {
		return this.data;
	}

	public void setData(CharBuffer data) {
		this.data = data;
	}

	static void checkArgs(String[] args, int num) throws MessageParseException {
		if (args.length != num) {
			throw new MessageParseException("Unexpected number of arguments. Expected: " + num + " Found: " + args.length);
		}

		for (int i = 0; i < args.length; i++) {
			if (args[i] == null) {
				throw new MessageParseException("Argument no " + i + " is null.");
			} else if (args[i].contains(SEPERATOR)) {
				throw new MessageParseException("Message may not contain seperator char: '" + SEPERATOR + "'");
			}
		}
	}

	public String toString() {
		return getHeaderStr();
	}

}
