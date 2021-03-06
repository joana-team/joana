/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber;


public enum JobState {

	NEW, RUNNING, DONE, FAILED, UNKNOWN;

	public static JobState findState(String state) {
		for (JobState s : JobState.values()) {
			if (s.name().equals(state)) {
				return s;
			}
		}

		return null;
	}



}
