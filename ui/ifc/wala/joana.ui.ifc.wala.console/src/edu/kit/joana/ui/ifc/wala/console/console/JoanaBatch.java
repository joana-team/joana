/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.console;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import edu.kit.joana.ui.ifc.wala.console.io.IFCConsoleOutput;
import edu.kit.joana.ui.ifc.wala.console.io.PrintStreamConsoleWrapper;
import edu.kit.joana.util.io.IOFactory;

/**
 * @author Martin Mohr
 */
public final class JoanaBatch {

	private JoanaBatch() {}

	public static void main(String[] args) throws IOException {
		if (args.length == 1) {
			executeScriptWithStandardOutput(args[0]);
		} else {
			printUsage();
		}
	}

	public static void executeScriptWithStandardOutput(String scriptFile) throws IOException {
		runConsole(new File(scriptFile), System.out);
	}
	
	public static void executeScriptWithStandardOutput(List<String> script) throws IOException {
		runConsole(new BufferedReader(IOFactory.createUTF8ISReader(new ByteArrayInputStream(IOFactory.createUTF8Bytes(fuseIntoOneBigString(script))))), System.out);
	}
	
	private static String fuseIntoOneBigString(List<String> instructions) {
		final StringBuilder b = new StringBuilder();
		for (String line : instructions) {
			b.append(line + '\n');
		}
		return b.toString();
	}

	private static void runConsole(File scriptFile, PrintStream out) throws IOException {
		runConsole(new BufferedReader(IOFactory.createUTF8ISReader(new FileInputStream(scriptFile))), out);
	}
	
	private static void runConsole(BufferedReader in, PrintStream out) throws IOException {
		IFCConsoleOutput cOut = new PrintStreamConsoleWrapper(out, out, null, out, out);
		IFCConsole c = new IFCConsole(in, cOut);
		c.setShowPrompt(false);
		c.interactive();
	}

	private static void printUsage() {
		System.out
				.println("Usage: edu.kit.joana.ui.ifc.wala.console.console.JoanaBatch <scriptfile>");
	}

}
