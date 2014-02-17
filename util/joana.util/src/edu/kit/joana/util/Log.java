/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Logging stuff. Looks for logging configuration in System.properties > property file > DEFAULT_ENABLED.
 * The default is disabled logging.
 * 
 * @author Juergen Graf <graf@kit.edu>
 */
public final class Log {
	
	public static final String L_ERROR 							= "error";
	public static final String L_API_DEBUG 						= "api.debug";
	public static final String L_API_ANNOTATION_DEBUG			= "api.annotation.debug";
	public static final String L_CONSOLE_DEBUG 					= "console.debug";
	public static final String L_SDG_CORE_DEBUG 				= "sdg.core.debug";
	public static final String L_SDG_GRAPH_DEBUG 				= "sdg.graph.debug";
	public static final String L_SDG_INTERFERENCE_DEBUG 		= "sdg.interference.debug";
	public static final String L_SDG_INTERFERENCE_INFO 		    = "sdg.interference.info";
	public static final String L_SDG_CALLGRAPH_DEBUG 			= "sdg.callgraph.debug";
	public static final String L_SDG_ISCR_DEBUG 				= "sdg.iscr.debug";
	public static final String L_SDG_INFO 						= "sdg.info";
	public static final String L_MHP_DEBUG 						= "mhp.debug";
	public static final String L_MHP_INFO						= "mhp.info";
	public static final String L_WALA_UNRESOLVED_CLASSES		= "wala.unresolved";
	public static final String L_WALA_CORE_DEBUG				= "wala.core.debug";
	public static final String L_WALA_IR_DUMP					= "wala.dump.ssa";
	public static final String L_WALA_CFG_DUMP					= "wala.dump.cfg";
	public static final String L_WALA_INTERFERENCE_DEBUG		= "wala.interference.debug";
	public static final String L_SIDEEFFECT_DEBUG				= "sideeffect.debug";
	public static final String L_OBJGRAPH_STATS					= "objgraph.stats";
	public static final String L_JSDG_INFO						= "jsdg.info";
	public static final String L_UI_DEBUG						= "ui.debug";
	public static final String L_MOJO_DEBUG						= "mojo.debug";
	public static final String L_MOJO_INFO						= "mojo.info";
	public static final String L_TEST_DEBUG						= "test.debug";

	private static final Map<String, Logger> NAME2LOG = new HashMap<String, Logger>();
	private static final Set<String> DEFAULT_ENABLED = new HashSet<String>();
	static {
		DEFAULT_ENABLED.add(L_ERROR);
	}
	
	private static final String PROP_FILE = "joana-log.properties";
	
	private static final Properties PROP;
	static {
		final InputStream propertyStream = Log.class.getClassLoader().getResourceAsStream(PROP_FILE);
		PROP = new Properties();
		try {
			PROP.load(propertyStream);
		} catch (Throwable e) {
		}	
	}
	
	public static final Logger ERROR = getLogger(L_ERROR);
	private static final Logger DEFAULT_DISABLED = new Logger() {
		@Override
		public void outln(String str, Throwable t) {}
		@Override
		public void outln(String str) {}
		@Override
		public void outln(Object obj, Throwable t) {}
		@Override
		public void outln(Object obj) {}
		@Override
		public void out(String str) {}
		@Override
		public void out(Object obj) {}
		
		@Override
		public boolean isEnabled() {
			return false;
		}
	};
	
	private Log() {
		throw new IllegalStateException("Do not initialize me.");
	}
	
	public static Logger getLogger(final String name) {
		if (NAME2LOG.containsKey(name)) {
			return NAME2LOG.get(name);
		}
		
		final PrintStream outStream = findDefault(name);
		if (outStream != null) {
			final Logger l = new DefaultLogger(outStream);
			NAME2LOG.put(name, l);

			return l;
		} else {
			return DEFAULT_DISABLED;
		}
	}

	private static PrintStream findDefault(final String name) {
		final String sysStr = System.getProperty(name);
		if (sysStr != null) {
			return getStream(sysStr);
		}
		
		final String propStr = PROP.getProperty(name);
		if (propStr != null) {
			return getStream(propStr);
		}
		
		return (DEFAULT_ENABLED.contains(name) ? System.out : null);
	}
	
	private static PrintStream getStream(final String prop) {
		if (prop == null) {
			return System.out;
		} else if (prop.equals("false")) {
			return null;
		} else if (prop.equals("err")) {
			return System.err;
		} else if (prop.startsWith("file:")) {
			final String fileName = prop.substring("file:".length());
			try {
				final PrintStream out = new PrintStream(fileName);
				return out;
			} catch (SecurityException e) {
				ERROR.outln(e.getMessage(), e);
			} catch (FileNotFoundException e) {
				ERROR.outln(e.getMessage(), e);
			}
			ERROR.outln("could not open file '" + fileName + "' - directing output to System.out");
		}
		
		return System.out;
	}
}
