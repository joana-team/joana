package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class App {

	// magic strings are evil
	private static final String JAVA_FILE_EXT = ".java";
	private static final String CLASS_FILE_EXT = ".class";

	public static void main(String[] args) {
		Args jArgs = new Args();
		JCommander jc = JCommander.newBuilder().addObject(jArgs).build();
		jc.setProgramName("QIF Interpreter");

		try {
			jc.parse(args);
			jArgs.validate();
		} catch (ParameterException e) {
			jc.usage();
			System.exit(1);
		}

		if (jArgs.help) {
			jc.usage();
		}

		// check if we got a .java file as input. If yes, we need to compile it to a .class file first
		String programPath = getAbsPath(jArgs.inputFiles.get(0), jArgs.workingDir);
		if (programPath.endsWith(JAVA_FILE_EXT)) {

		}

		if (jArgs.doStatic) {
			// do static analysis
		} else {
			// load data from provided dnnf file
		}

		if (!jArgs.onlyStatic) {
			// run the program
		}
	}

	private static String getAbsPath(String path, String workingDir) {
		return (path.startsWith("/")) ? path : workingDir + "/" + path;
	}

	public static class Args implements IParameterValidator {

		private static final String OUTPUT_DIR_NAME = "/out_";

		@Parameter(names = "--usage", description = "Print help") private boolean help;

		@Parameter(names = "--static", description = "Perform only static analysis on the input program") private boolean onlyStatic;

		@Parameter(names = "--dump-graphs", description = "Dump graphs created by JOANA") private boolean dumpGraphs;

		@Parameter(names = "-o", description = "Specify a path where the output directory should be created (Default is the current working directory)") String outputDirectory = ".";

		@Parameter(description = "A program for the interpreter to execute, plus optionally the result of a previous static analysis", validateWith = Args.class) private List<String> inputFiles = new ArrayList<>();

		@Parameter(names = "-args", description = "Arguments for running the input program", variableArity = true) private List<String> args = new ArrayList<>();

		@Parameter(names = "-workingDir", description = "Directory from which the interpreter was started. Should be set automatically by run.sh", required = true) private String workingDir;

		/**
		 * sometimes we don't need to do a static analysis, bc it is already provided via input
		 */
		private boolean doStatic = true;

		/**
		 * Validates the given arguments. Expected are:
		 * - option {@code} static: A .java file containing the program to be analysed
		 * - otherwise: A .class file of the program to be analysed,
		 * 		optionally a .dnnf file (if this is provided the static analysis will be skipped)
		 * 		and the input parameters for the program execution
		 *
		 * @throws ParameterException if some paramter contraint is violated
		 */
		public void validate() throws ParameterException {

			if (help) { return; }

			// check if we have a valid path to create our output directory
			File out = new File(outputDirectory);
			if (!out.exists() | !out.isDirectory()) {
				throw new ParameterException("Error: Couldn't find output directory.");
			} else {
				outputDirectory = outputDirectory + OUTPUT_DIR_NAME + System.currentTimeMillis();
			}

			// we always need an input program
			if (inputFiles.size() == 0) {
				throw new ParameterException("Error: No input file found");
			}

			// if 2 input files are provided one of them is from a previous static analysis, hence we don't need to do it again
			if (inputFiles.size() == 2) {
				doStatic = false;
				return;
			} else if (inputFiles.size() != 1) {
				throw new ParameterException("Error: unexpected number of arguments");
			}
		}

		@Override public void validate(String name, String value) throws ParameterException {
			File f = new File(value);
			if (f.isDirectory() || !f.exists() || !f.canRead()) {
				throw new ParameterException("Input File couldn't be found: Path not valid.");
			}
		}
	}
}
