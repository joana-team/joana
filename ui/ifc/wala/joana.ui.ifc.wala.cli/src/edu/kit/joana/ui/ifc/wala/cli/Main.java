package edu.kit.joana.ui.ifc.wala.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import edu.kit.joana.ui.ifc.wala.console.console.IFCConsole;
import edu.kit.joana.ui.ifc.wala.console.console.ImprovedCLI;
import edu.kit.joana.ui.ifc.wala.console.io.IFCConsoleOutput;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.NullPrintStream;
import picocli.CommandLine;

/**
 * Runs the program on the command line
 */
public class Main {
	
	public static void main(String[] args) {
		ImprovedCLI.run(args, new IFCConsole(new BufferedReader(new InputStreamReader(System.in)), new IFCConsoleOutput() {

			@Override
			public void log(String logMessage) {
				//System.out.print(logMessage);
			}

			@Override
			public void logln(String logMessage) {
				//System.out.println(logMessage);
			}

			@Override
			public void info(String infoMessage) {
				//System.out.println(infoMessage);
			}

			@Override
			public void error(String errorMessage) {
				System.err.println(errorMessage);
			}

			@Override
			public Answer question(String questionMessage) {
				System.out.println(questionMessage);
				try {
					return new BufferedReader(new InputStreamReader(System.in)).readLine().contains("y")
							? Answer.YES
							: Answer.NO;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}

			@Override
			public PrintStream getPrintStream() {
				return System.out;
			}

			@Override public PrintStream getDebugPrintStream() {
				if (Logger.getGlobal().isLoggable(Level.INFO)){
					return getPrintStream();
				}
				return new NullPrintStream();
			}
		}).createWrapper(), false);
	}

}
