/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import java.util.function.Supplier;

/**
 * A primitive global logger for debug information, that just 
 * prints everything onto the console.
 */
public class BasicLogger {

	private static boolean enabled = false;
	
	public static void log(Supplier<Object> complexMsgCreator) {
		if (enabled) {
			System.out.println(complexMsgCreator.get());
		}
	}
	
	public static void log(Object msg) {
		if (enabled) {
			System.out.println(msg);
		}
	}
	
	public static void log(String format, Object... args) {
		if (enabled) {
			System.out.printf(format + "\n", args);
		}
	}
	
	public static void logOnErr(String msg) {
		if (enabled) {
			System.err.println(msg);
		}
	}
	
	public static void enable() {
		enabled = true;
	}
	
	public static void disable() {
		enabled = false;
	}
	
	public static boolean isLoggingEnabled() {
		return enabled;
	}
}
