/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.algorithms;

import java.util.LinkedList;
import java.util.List;

/** Algorithms represents the data of the Algorithms.xml file <br>
 * that contains all available slicing and chopping algorithms for <br>
 * this plugin.
 * This data structure is a tree that consists of Algorithm objects.
 *
 * @author giffhorn
 * @see Algorithm
 */
public class Algorithms {
	// the top-level Algorithm objects
	private List<Algorithm> algorithms;

	/** Creates a new Algorithms object.
	 */
	public Algorithms() {
		algorithms = new LinkedList<Algorithm>();
	}

	/** Adds an Algorithm.
	 *
	 * @param t  The Algorithms to add.
	 */
	public void addAlgorithm(Algorithm t) {
		algorithms.add(t);
	}

	/** Retrieves all top-level Algorithm objects.
	 *
	 * @return  The top-level Algorithm objects as a (probably empty) List.
	 */
	public List<Algorithm> getAlgorithms() {
		return algorithms;
	}

	/** Returns a textual representation.
	 */
	public String toString() {
		String str = "<algorithms>\n";

		for (Algorithm t : algorithms) {
			str += "   " + t + "\n\n";
		}

		str += "</algorithms>";

		return str;
	}
}
