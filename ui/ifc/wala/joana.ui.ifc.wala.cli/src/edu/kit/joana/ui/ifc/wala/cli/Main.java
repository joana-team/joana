package edu.kit.joana.ui.ifc.wala.cli;

import edu.kit.joana.ui.ifc.wala.console.console.attest.JumpTargetAnalysis;
import edu.kit.joana.ui.ifc.wala.console.console.IFCConsole;
import edu.kit.joana.ui.ifc.wala.console.console.ImprovedCLI;
import edu.kit.joana.ui.ifc.wala.console.io.IFCConsoleOutput;
import edu.kit.joana.util.NullPrintStream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.kit.joana.ui.ifc.wala.console.console.ImprovedCLI.exit;

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

	@Command(name = "attest", description = "Create attestation files for newly generated SDGs")
	public static class AttestCommand implements Callable<Integer> {

		static enum OutputMode {
			JUMPS,
			JUMPS_TARGETS,
			JUMPS_TARGETS_NODES
		}

		@ParentCommand ImprovedCLI.CliCommands parent;

		@Option(names = "--output", description = "Target file (or '-' for standard out)")
		String output = "-";

		@Option(names="--mode", description = "Valid values: ${COMPLETION-CANDIDATES}")
		public OutputMode mode = OutputMode.JUMPS_TARGETS;

		public @ImprovedCLI.State ImprovedCLI.SinksAndSourcesEnabled state;

		static IFCConsole console;
		static IFCConsole.Wrapper wrapper;

		@Override public Integer call() throws Exception {
			console.buildSDGIfNeeded();
			JumpTargetAnalysis ana = JumpTargetAnalysis
					.analyse(parent.verbose ? System.out : new NullPrintStream(), console.getAnalysis(), console.getCallGraph());
			PrintStream out = new PrintStream(output.equals("-") ? System.out : Files.newOutputStream(Paths.get(output)));
			switch (mode) {
			case JUMPS:
				ana.jumpsOutput(out);
				break;
			case JUMPS_TARGETS:
				ana.jumpsAndTargetsOutput(out);
				break;
			case JUMPS_TARGETS_NODES:
				ana.jumpsAndTargetsAndNodesOutput(out);
				break;
			}
			return exit(true);
		}
	}

	public static void main(String[] args) {
		IFCConsole console = new IFCConsole(new BufferedReader(new InputStreamReader(System.in)), new IFCConsoleOutput() {

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
		});
		IFCConsole.Wrapper wrapper = console.createWrapper();
		AttestCommand.console = console;
		AttestCommand.wrapper = wrapper;
		ImprovedCLI.run(args, wrapper, false, AttestCommand.class);
	}

}
