/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.console;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import edu.kit.joana.ui.ifc.wala.console.io.IFCConsoleOutput;
import edu.kit.joana.ui.ifc.wala.console.io.PrintStreamConsoleWrapper;

/**
 * @author Martin Mohr
 */
public final class JoanaBatch {

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			printUsage();
		} else {
			executeScriptWithStandardOutput(args[0]);
		}
	}

	private static void executeScriptWithStandardOutput(String scriptFile) throws IOException {
		runConsole(scriptFile, System.out);
	}

	private static void runConsole(String scriptFile, PrintStream out) throws IOException {
		BufferedReader fileIn = new BufferedReader(new InputStreamReader(new FileInputStream(scriptFile)));
		IFCConsoleOutput cOut = new PrintStreamConsoleWrapper(out, out, null, out, out);
		IFCConsole c = new IFCConsole(fileIn, cOut);
		c.interactive();
	}

	private static void printUsage() {
		System.out
				.println("Usage: edu.kit.joana.ui.ifc.wala.console.console.JoanaBatch <scriptfile>");
	}

}
