/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber;

import java.nio.CharBuffer;

public final class Job {

	public static final int START_ID = 0;
	public static final int CANCEL_ID = -1;

	private final int id;
	private final String type;
	private final String name;
	private final String comment;
	private final CharBuffer data;
	private final JobState state;

	public Job(int id, String type, String name, String comment, CharBuffer data) {
		this(id, type, name, comment, data, JobState.UNKNOWN);
	}

	public Job(int id, String type, String name, String comment, CharBuffer data, JobState state) {
		if (state != JobState.DONE && state != JobState.FAILED && state != JobState.UNKNOWN) {
			throw new IllegalArgumentException("A job may only be marked as done, failed or unknown.");
		}

		this.id = id;
		this.type = type;
		this.name = name;
		this.comment = comment;
		this.data = (data != null ? data.asReadOnlyBuffer() : null);
		this.state = state;
	}

	public int getId() {
		return this.id;
	}

	public String getType() {
		return this.type;
	}

	public JobState getState() {
		return state;
	}

	public String getName() {
		return this.name;
	}

	public String getComment() {
		return this.comment;
	}

	public String toString() {
		return "Job(" + id + ")[" + type + "] - " + name + " - '" + comment + "'";
	}

	public int hashCode() {
		return id;
	}

	public boolean equals(Object obj) {
		return obj instanceof Job && ((Job) obj).id == id;
	}

	public CharBuffer getData() {
		return data;
	}
}
