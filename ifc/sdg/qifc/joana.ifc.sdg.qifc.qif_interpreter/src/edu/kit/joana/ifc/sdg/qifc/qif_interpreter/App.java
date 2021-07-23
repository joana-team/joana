package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import com.beust.jcommander.*;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.MissingValueException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.AnalysisPipeline;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class App {

	private static final String JAVA_FILE_EXT = ".java";
	private static final String CLASS_FILE_EXT = ".class";
	private static final String DNNF_FILE_EXT = ".dnnf";
	private static final String JAVAC_INVOKE_CMD = "javac -target 1.8 -source 1.8 -d %s %s -classpath %s";

	public static void main(String[] args) throws InvalidClassFileException, MissingValueException,
			edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ParameterException, OutOfScopeException, IOException,
			UnexpectedTypeException, InterruptedException {
		//System.setErr(new NullPrintStream());
		Args jArgs = new Args();
		JCommander jc = JCommander.newBuilder().addObject(jArgs).build();
		jc.setProgramName("QIF Interpreter");
		try {
			jc.parse(args);
			jArgs.validate();
		} catch (ParameterException e) {
			e.printStackTrace();
			jc.usage();
			System.exit(1);
		}

		if (jArgs.help) {
			jc.usage();
			System.exit(0);
		}

		try {
			Logger.initWithFileHandler(Level.ALL, jArgs.outputDirectory, jArgs.inputFiles.get(0));
		} catch (IOException e) {
			e.printStackTrace();
		}

		AnalysisPipeline pipeline = new AnalysisPipeline();
		pipeline.runPipeline(jArgs);
	}

	public static class Args implements IParameterValidator, IStringConverter<String> {

		private static final String OUTPUT_DIR_NAME = "out_";
		@Parameter(names = "--o", description = "Specify a path where the output directory should be created (Default is the current working directory)") public String outputDirectory = ".";
		@Parameter(names = "--usage", description = "Print help") boolean help = false;
		@Parameter(names = "--static", description = "Perform only static analysis on the input program to measure its channel capacity") public boolean onlyStatic = false;
		@Parameter(names = "--run", description = "Run the program without performing any analysis") public boolean onlyRun = false;
		@Parameter(names = "--dump-graphs", description = "Dump graphs created by JOANA") public boolean dumpGraphs = false;
		@Parameter(description = "A program for the interpreter to execute, plus optionally the result of a previous static analysis", validateWith = Args.class, converter = Args.class) public List<String> inputFiles = new ArrayList<>();
		@Parameter(names = "--unwind", description = "Set limits for recursion depth, method depth and loop iteration") public int unwind = Config.DEFAULT_UNWIND;
		@Parameter(names = "--args", description = "Arguments for running the input program", variableArity = true) public List<String> args = new ArrayList<>();
		@Parameter(names = "--pp", description = "Use static pre-processing") public boolean pp = Config.DEFAULT_PP;
		@Parameter(names = "--hybrid", description = "Use hybrid analysis for calculating the channel capacity") public boolean hybrid = Config.DEFAULT_HYBRID;

		@Parameter(names = "--workingDir", description = "Directory from which the interpreter was started. Should be set automatically by run.sh", required = true) public String workingDir = System
				.getProperty("user.dir");

		/**
		 * Validates the given arguments. Expected are:
		 * - option {@code} static: A .java file containing the program to be analysed
		 * - otherwise: A .class file of the program to be analysed,
		 * optionally a .dnnf file (if this is provided the static analysis will be skipped)
		 * and the input parameters for the program execution
		 *
		 * @throws ParameterException if some parameter constraint is violated
		 */
		public void validate() throws ParameterException {

			if (help) {
				return;
			}

			// check if we have a valid path to create our output directory
			// TODO: clean up this mess
			File out = new File(outputDirectory);

			if (!out.exists() | !out.isDirectory()) {
				throw new ParameterException("Error: Couldn't find output directory.");
			} else {
				outputDirectory = OUTPUT_DIR_NAME + System.currentTimeMillis();
				out = new File(outputDirectory);
				final boolean mkdir = out.mkdir();
				if (!mkdir) {
					try {
						throw new FileSystemException("Error: Couldn't create output directory");
					} catch (FileSystemException e) {
						e.printStackTrace();
					}
				}
			}
			Logger.log(String.format("Using output directory: %s", outputDirectory));
			// we always need an input program
			if (inputFiles.size() == 0) {
				throw new ParameterException("Error: No input file found");
			}
		}

		@Override public void validate(String name, String value) throws ParameterException {
			value = (value.startsWith("/")) ? value : workingDir + "/" + value;
			File f = new File(value);
			if (f.isDirectory() || !f.exists() || !f.canRead() || !hasValidExtension(value)) {
				throw new ParameterException(String.format("Input File couldn't be found: %s -- Path not valid.", value));
			}
		}

		private boolean hasValidExtension(String path) {
			return (path.endsWith(JAVA_FILE_EXT) || path.endsWith(CLASS_FILE_EXT) || path.endsWith(DNNF_FILE_EXT));
		}

		// convert all paths to absolute paths
		@Override public String convert(String path) {
			return (path.startsWith("/")) ? path : workingDir + "/" + path;
		}
	}
}