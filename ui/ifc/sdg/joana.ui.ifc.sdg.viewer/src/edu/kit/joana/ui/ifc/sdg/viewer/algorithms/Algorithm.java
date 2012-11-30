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

/** This class represents an algorithm or an algorithmic category for analyzing SDGs.
 *
 * @author giffhorn
 * @see Algorihtms
 */
public class Algorithm {
	// the name
	private String name;
	// a description
	private String description;
	// the java class that implements the algorithm
	private String className;
	// a category this algorithm belongs to
	private Algorithm parent;
	// algorithms of this category
	private List<Algorithm> algorithms;

	/** Creates a new Algorithm.
	 *
	 * @param parent  Represents a category to wich this algorithm belongs.
	 */
	public Algorithm(Algorithm parent) {
		this.parent = parent;
		algorithms = new LinkedList<Algorithm>();
	}

	/** Sets the Algorithm's name.
	 *
	 * @param str  A name.
	 */
	public void setName(String str) {
		name = str;
	}

	/** Sets the Algorithm's description.
	 *
	 * @param str  A description.
	 */
	public void setDescription(String str) {
		description = str;
	}

	/** Sets the Algorithm's class name.
	 *
	 * @param str  A class name.
	 */
	public void setClassName(String str) {
		className = str;
	}

	/** Adds a subcategory or subalgorithm.
	 *
	 * @param str  An algorithm.
	 */
	public void addAlgorithm(Algorithm a) {
		algorithms.add(a);
	}

	/** Returns the name.
	 *
	 * @return The name.
	 */
	public String getName() {
		return name;
	}

	/** Returns the description.
	 *
	 * @return The description.
	 */

	public String getDescription() {
		return description;
	}

	/** Returns the class name.
	 *
	 * @return The class name.
	 */
	public String getClassName() {
		return className;
	}

	/** Returns the category the algorihtm belongs to.
	 *
	 * @return The category.
	 */
	public String getCategory() {
		if (parent != null) {
			return parent.getCategory();
		}

		return className;
	}

	/** Returns all subordinated algorithms.
	 *
	 * @return The subordinated algorithms.
	 */
	public List<Algorithm> getAlgorithms() {
		return algorithms;
	}

	/** Returns true if this algorithm describes a category.
	 */
	public boolean hasAlgorithms() {
		return algorithms.size() > 0;
	}

	/** Returns a textual representation.
	 */
	public String toString() {
		String str = "<algorithm" +
				"\n   name=" + name +
				"\n   description=" + description +
				"\n   class=" + className + ">\n";



		for (Algorithm a : algorithms) {
			str += "   " + a;
		}

		str += "</algorithm>\n";

		return str;
	}
}
