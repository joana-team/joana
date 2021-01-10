package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import java.io.IOException;
import java.util.logging.*;

public class SimpleLogger {

	private static final Level DEFAULT_LOG_LEVEL = Level.INFO;
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private static boolean enabled;

	public static void initWithConsoleHandler(Level logLevel) {
		LOGGER.setUseParentHandlers(false);
		Handler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(logLevel);
		LOGGER.addHandler(consoleHandler);
		enabled = true;
	}

	public static void initWithFileHandler(Level logLevel, String logFilePath) throws IOException {
		LOGGER.setUseParentHandlers(false);
		Handler fileHandler = new FileHandler(logFilePath + "/" + System.currentTimeMillis() + ".log");
		fileHandler.setLevel(logLevel);
		fileHandler.setFormatter(new SimpleFormatter());
		LOGGER.addHandler(fileHandler);
		enabled = true;
	}

	public static void log(Level level, String msg) {
		if (enabled) LOGGER.log(level, msg);
	}

	public static void log(String msg) {
		if (enabled) LOGGER.log(DEFAULT_LOG_LEVEL, msg);
	}

	public static void disable() {
		enabled = false;
	}

	public static void enable() {
		enabled = true;
	}
}
