/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.server;

import edu.kit.joana.wala.util.jobber.Job;


class Worker {

	private final int id;
	private final String name;
	private final String type;

	protected Worker(int id, String name, String type) {
		this.id = id;
		this.name = name;
		this.type = type;
	}

	protected boolean supports(Job job) {
		return type.equals(job.getType());
	}

	protected String getType() {
		return this.type;
	}

	protected int getId() {
		return this.id;
	}

	protected String getName() {
		return this.name;
	}

	public String toString() {
		return "Worker(" + id + ")" + " - " + name;
	}

	public int hashCode() {
		return id;
	}

	public boolean equals(Object obj) {
		return obj instanceof Worker && ((Worker) obj).id == id;
	}

}
