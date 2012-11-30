/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 08.12.2004
 *
 */
package edu.kit.joana.ifc.sdg.lattice;

/**
 * @author naxan
 *
 */
public class WrongLatticeDefinitionException extends Exception {
	private static final long serialVersionUID = 1L;

	String[] messages = new String[0];

	/**
	 * Constructor
	 *
	 * @param messages
	 *            a list of messages describing the definition problems
	 */
	public WrongLatticeDefinitionException(String[] messages) {
		this.messages = messages;
	}

	/**
	 * Constructor
	 *
	 * @param msg
	 *            a message describing the definition problem
	 */
	public WrongLatticeDefinitionException(String msg) {
		messages = new String[] { msg };
	}

	/**
	 * @return Returns the messages.
	 * @uml.property name="messages"
	 */
	public String[] getMessages() {
		return messages;
	}

	public String toString() {
		return messages[0];

	}
}
