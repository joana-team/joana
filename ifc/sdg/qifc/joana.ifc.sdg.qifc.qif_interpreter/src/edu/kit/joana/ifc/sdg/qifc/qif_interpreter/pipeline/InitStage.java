package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.App;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.Config;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ErrorHandler;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.JavacException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Logger;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

/**
 * - Check all paths / input files from arguments
 * - Compile input program if necessary / check if program is compilable
 */
public class InitStage implements IStage {

	private static final String JAVA_FILE_EXT = ".java";
	private static final String CLASS_FILE_EXT = ".class";
	private static final String DNNF_FILE_EXT = ".dnnf";
	private static final String JAVAC_INVOKE_CMD = "javac -target 1.8 -source 1.8 -d %s %s -classpath %s";

	private boolean success = false;

	@Override public boolean success() {
		return success;
	}

	@Override public Environment execute(Environment env) {
		assert (env.args != null);

		// configs used for benchmarking
		Config.usePP = env.args.pp;
		Config.useHybrid = env.args.hybrid;
		Config.bitwidth = env.args.width;

		String classFilePath;
		String programPath = env.args.inputFiles.get(0);
		String jarPath = null;
		try {
			jarPath = App.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		// check if we got a .java file as input. If yes, we need to compile it to a .class file first
		Logger.log("Starting compilation with javac");
		if (programPath.endsWith(JAVA_FILE_EXT)) {
			try {
				compile(env.args.outputDirectory, programPath, jarPath);
			} catch (IOException | InterruptedException | JavacException e) {
				ErrorHandler.fatal(e);
			}
			classFilePath = env.args.outputDirectory + "/" + FilenameUtils.getBaseName(programPath) + CLASS_FILE_EXT;
		} else {
			classFilePath = programPath;
		}
		Logger.log(String.format("Finished compilation. Generated file: %s", classFilePath));
		DotGrapher.configureDest(env.args.outputDirectory);

		env.config = new Config(env.args.loopMax, env.args.recMax, env.args.methodMax);

		// get classname via filename
		String className = FilenameUtils.getBaseName(programPath);
		env.lastStage = new InitResult(className, classFilePath);
		this.success = true;
		return env;
	}

	private void compile(String outputDirectory, String programPath, String jarPath)
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

	@Override public Stage identity() {
		return Stage.INIT;
	}

	class InitResult implements IResult {
		String className;
		String classFilePath;

		@Override public Stage fromStage() {
			return Stage.INIT;
		}

		public InitResult(String className, String classFilePath) {
			this.classFilePath = classFilePath;
			this.className = className;
		}
	}
}