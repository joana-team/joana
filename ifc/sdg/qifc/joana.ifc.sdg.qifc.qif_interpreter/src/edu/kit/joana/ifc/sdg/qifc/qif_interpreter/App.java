package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.*;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.exec.Interpreter;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.StaticAnalysis;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.FileSystemException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class App {

	// magic strings are evil
	private static final String JAVA_FILE_EXT = ".java";
	private static final String CLASS_FILE_EXT = ".class";
	private static final String DNNF_FILE_EXT = ".dnnf";
	private static final String JAVAC_INVOKE_CMD = "javac -target 1.8 -source 1.8 -d %s %s -classpath %s";

	public static void main(String[] args) throws InvalidClassFileException, MissingValueException,
			edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ParameterException, OutOfScopeException, IOException,
			UnexpectedTypeException, InterruptedException {
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
		}

		try {
			SimpleLogger.initWithFileHandler(Level.ALL, jArgs.outputDirectory);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String classFilePath;
		String programPath = jArgs.inputFiles.get(0);
		String jarPath = null;
		try {
			jarPath = App.class
					.getProtectionDomain()
					.getCodeSource()
					.getLocation()
					.toURI()
					.getPath();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		// check if we got a .java file as input. If yes, we need to compile it to a .class file first
		SimpleLogger.log("Starting compilation with javac");
		if (programPath.endsWith(JAVA_FILE_EXT)) {
			try {
				compile(jArgs.outputDirectory, programPath, jarPath);
			} catch (IOException | InterruptedException | JavacException e) {
				ErrorHandler.fatal(e);
			}
			classFilePath = jArgs.outputDirectory + "/" + FilenameUtils.getBaseName(programPath) + CLASS_FILE_EXT;
		} else {
			classFilePath = programPath;
		}
		SimpleLogger.log(String.format("Finished compilation. Generated file: %s", classFilePath));

		// get classname via filename
		String className = FilenameUtils.getBaseName(programPath);
		SimpleLogger.log("Classname: " + className);

		// create SDG
		IRBuilder builder = new IRBuilder(classFilePath, className);
		builder.createBaseSDGConfig();
		try {
			builder.buildAndKeepBuilder();
		} catch (IOException | CancelException | ClassHierarchyException | GraphIntegrity.UnsoundGraphException e) {
			e.printStackTrace();
		}

		if (jArgs.dumpGraphs) {
			builder.dumpGraph(jArgs.outputDirectory);
		}

		Program p = builder.getProgram();

		// execute
		Interpreter i = new Interpreter(p);
		StaticAnalysis sa = new StaticAnalysis(p);

		sa.computeSATDeps();
		i.execute(jArgs.args);

		Method entry = p.getEntryMethod();
		Value leaked = entry.getProgramValues().values().stream().filter(Value::isLeaked).findFirst().get();
		int[] params = entry.getIr().getParameterValueNumbers();
		List<Value> hVals = Arrays.stream(params).mapToObj(entry::getValue).filter(Objects::nonNull)
				.collect(Collectors.toList());
		LeakageComputation lc = new LeakageComputation(hVals, leaked, entry);
		lc.compute(jArgs.outputDirectory);
	}

	private static void compile(String outputDirectory, String programPath, String jarPath)
			throws IOException, InterruptedException, JavacException {
		String cmd = String.format(JAVAC_INVOKE_CMD, outputDirectory, programPath, jarPath);
		Process compilation = Runtime.getRuntime().exec(cmd);

		InputStreamReader isr = new InputStreamReader(compilation.getErrorStream());
		BufferedReader rdr = new BufferedReader(isr);
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = rdr.readLine()) != null) {
			sb.append(line);
		}

		int exitCode = compilation.waitFor();

		if (exitCode != 0) {
			throw new JavacException(sb.toString());
		}
	}

	public static class Args implements IParameterValidator, IStringConverter<String> {

		private static final String OUTPUT_DIR_NAME = "out_";
		@Parameter(names = "-o", description = "Specify a path where the output directory should be created (Default is the current working directory)") String outputDirectory = ".";
		@Parameter(names = "--usage", description = "Print help") private boolean help = false;
		@Parameter(names = "--static", description = "Perform only static analysis on the input program") private boolean onlyStatic = false;
		@Parameter(names = "--dump-graphs", description = "Dump graphs created by JOANA") private boolean dumpGraphs = false;
		@Parameter(description = "A program for the interpreter to execute, plus optionally the result of a previous static analysis", validateWith = Args.class, converter = Args.class) private List<String> inputFiles = new ArrayList<>();

		@Parameter(names = "-args", description = "Arguments for running the input program", variableArity = true) private List<String> args = new ArrayList<>();

		@Parameter(names = "-workingDir", description = "Directory from which the interpreter was started. Should be set automatically by run.sh", required = true) private String workingDir = System.getProperty("user.dir");

		/**
		 * sometimes we don't need to do a static analysis, bc it is already provided via input
		 */
		private boolean doStatic = true;

		/**
		 * Validates the given arguments. Expected are:
		 * - option {@code} static: A .java file containing the program to be analysed
		 * - otherwise: A .class file of the program to be analysed,
		 * optionally a .dnnf file (if this is provided the static analysis will be skipped)
		 * and the input parameters for the program execution
		 *
		 * @throws ParameterException if some paramter contraint is violated
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
			SimpleLogger.log(String.format("Using output directory: %s", outputDirectory));
			// we always need an input program
			if (inputFiles.size() == 0) {
				throw new ParameterException("Error: No input file found");
			}

			// if 2 input files are provided one of them is from a previous static analysis, hence we don't need to do it again
			if (inputFiles.size() == 2) {
				doStatic = false;
			} else if (inputFiles.size() != 1) {
				throw new ParameterException("Error: unexpected number of arguments");
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
