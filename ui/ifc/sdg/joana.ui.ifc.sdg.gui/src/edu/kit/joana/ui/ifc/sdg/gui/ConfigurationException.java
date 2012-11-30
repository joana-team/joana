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
package edu.kit.joana.ui.ifc.sdg.gui;

public class ConfigurationException extends Exception {

	public ConfigurationException(String message) {
		super(message);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public String toString() {
		return "ConfigurationException: " + getMessage();
	}
}
