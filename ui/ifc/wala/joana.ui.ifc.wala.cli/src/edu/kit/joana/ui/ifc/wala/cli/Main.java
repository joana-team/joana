package edu.kit.joana.ui.ifc.wala.cli;

import edu.kit.joana.ui.ifc.wala.console.console.IFCConsole;
import edu.kit.joana.ui.ifc.wala.console.console.ImprovedCLI;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 * <p>
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

/**
 * Runs the program on the command line
 */
public class Main {
	
	public static void main(String[] args) {
		ImprovedCLI.run(args, new IFCConsole(new BufferedReader(new InputStreamReader(System.in)), new LimitedIFCConsoleOutput()).createWrapper(), false);
	}

}
