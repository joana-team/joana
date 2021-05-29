package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.IStage;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.*;

public class Logger {

	private static long start;
	private static final Level DEFAULT_LOG_LEVEL = Level.INFO;

	private static final java.util.logging.Logger LOGGER = java.util.logging.Logger
			.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
	private static boolean enabled;

	public static void initWithConsoleHandler(Level logLevel) {
		LOGGER.setUseParentHandlers(false);
		Handler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(logLevel);
		LOGGER.addHandler(consoleHandler);
		enabled = true;
	}

	public static void initWithFileHandler(Level logLevel, String logFilePath, String inputProgPath)
			throws IOException {

		String className = Arrays.stream(inputProgPath.split("/")).reduce((fst, snd) -> snd).get();
		className = className.substring(0, className.lastIndexOf('.'));
		start = System.currentTimeMillis();
		LOGGER.setUseParentHandlers(false);
		Handler fileHandler = new FileHandler(logFilePath + "/" + className + ".log");
		fileHandler.setLevel(logLevel);
		fileHandler.setFormatter(new Formatter() {
			@Override public String format(LogRecord record) {
				return String.format("%7d - [%s] %s%n", record.getMillis() - start, record.getLevel().getName(),
						record.getMessage());
			}
		});
		LOGGER.addHandler(fileHandler);
		enabled = true;
	}

	public static void log(Level level, String msg) {
		if (enabled)
			LOGGER.log(level, msg);
	}

	public static void log(String msg) {
		if (enabled)
			LOGGER.log(DEFAULT_LOG_LEVEL, msg);
	}

	public static void logEval(String msg) {
		Logger.log(Eval.EVAL, msg);
	}

	public static void startPipelineStage(IStage.Stage stage) {
		logEval("[start] " + stage.toString());
	}

	public static void finishPipelineStage(IStage.Stage stage, boolean success) {
		if (success) {
			Logger.logEval("[finish] " + stage.toString());
		} else {
			Logger.logEval("[fail] " + stage.toString());
		}
	}

	public static void invokeMC(long execTime, String dimacsFile) {
		logEval(String.format("[mc] %d %s", execTime, dimacsFile));
	}

	public static void disable() {
		enabled = false;
	}

	public static void enable() {
		enabled = true;
	}

	public static void satAnalysis(int visitedInstructions, long duration) {
		logEval(String.format("[se] %d %d", visitedInstructions, duration));
	}

	public static class Eval extends Level {
		public static final Level EVAL = new Eval("EVAL");

		protected Eval(String name) {
			super(name, Level.SEVERE.intValue() + 1);
		}
	}
}