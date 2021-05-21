package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.SimpleLogger;

public class ErrorHandler {

	public static void fatal(Exception e) {
		System.err.println(e.getMessage());
		SimpleLogger.log(e.getMessage());
		System.exit(1);
	}

	public static void nonFatal(Exception e) {
		SimpleLogger.log(e.getMessage());
	}

}
