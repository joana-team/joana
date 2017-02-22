/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized runtime options for joana. Contains utility methods to read in properties either from command line
 * or property files.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public final class Config {

	public static final String C_OBJGRAPH_CUT_OFF_UNREACHABLE 			= "objgraph.cut-off-unreachable";
	public static final String C_OBJGRAPH_MERGE_EXCEPTIONS 				= "objgraph.merge-exceptions";
	public static final String C_OBJGRAPH_CUT_OFF_IMMUTABLE 			= "objgraph.cut-off-immutable";
	public static final String C_OBJGRAPH_MERGE_ONE_FIELD_PER_PARENT 	= "objgraph.merge-one-field-per-parent";
	public static final String C_OBJGRAPH_MERGE_PRUNED_CALL_NODES 		= "objgraph.merge-pruned-call-nodes";
	public static final String C_OBJGRAPH_ADVANCED_INTERPROC_PROP 		= "objgraph.advanced-interproc-prop";
	public static final String C_OBJGRAPH_MAX_NODES_PER_INTERFACE 		= "objgraph.max-nodes-per-interface";
	public static final String C_OBJGRAPH_CONVERT_TO_OBJTREE 			= "objgraph.convert-to-objtree";
	public static final String C_OBJGRAPH_DO_STATIC_FIELDS 				= "objgraph.do-static-fields";
	public static final String C_SIDEEFFECT_DETECTOR 					= "sideeffect.detector";
	public static final String C_SIDEEFFECT_DETECTOR_VAR 				= "sideeffect.detector-var";
	public static final String C_SDG_DATAFLOW_FOR_GET_FROM_FIELD		= "sdg.dataflow-for-get-from-field";
	
	private static final String PROP_FILE = "joana-options.properties";
	
	private static final Properties PROP;
	static {
		final InputStream propertyStream = Log.class.getClassLoader().getResourceAsStream(PROP_FILE);
		PROP = new Properties();
		try {
			PROP.load(propertyStream);
		} catch (Throwable e) {
		}	
	}
	
	private Config() {
		throw new UnsupportedOperationException();
	}

	public static boolean isDefined(final String option) {
		return (PROP.getProperty(option) != null || System.getProperty(option) != null);
	}
	
	public static boolean getBool(final String option) {
		if (!isDefined(option)) {
			throw new IllegalStateException("'" + option +"' is not defined. Check first with isDefined().");
		}
		
		final String value = getString(option);
		
		return value == null || !value.equals("false");
	}

	public static boolean getBool(final String option, final boolean defaultValue) {
		return (isDefined(option) ? getBool(option) : defaultValue);
	}

	public static int getInt(final String option, final int defaultValue) {
		if (!isDefined(option)) {
			throw new IllegalStateException("'" + option +"' is not defined. Check first with isDefined().");
		}
		
		final String value = getString(option);
		
		int intValue;
		try { 
			intValue = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			Log.ERROR.outln("Could not parse number '" + value + "' for option '" + option + "'. Using default: "
					+ defaultValue);
			intValue = defaultValue;
		}
		
		return intValue;
	}

	public static String getString(final String option) {
		if (!isDefined(option)) {
			throw new IllegalStateException("'" + option +"' is not defined. Check first with isDefined().");
		}
		
		String value = System.getProperty(option);
		if (value == null) {
			value = PROP.getProperty(option);
		}
		
		return value;
	}
	
}
