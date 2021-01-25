package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input;

import edu.kit.joana.ui.annotations.Sink;

public class Out {

	/**
	 * Method to print a value to a public output. To be used in input programs for the interpreter whenever a value should be leaked
	 * @param i the leaked value
	 */
	@Sink
	public static void print(int i) {}

}
