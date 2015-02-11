/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import edu.kit.joana.util.Stubs;

/**
 * Loads configuration of paths to .jars etc from properties file. Copied from JoanaPath in joana.api.test.
 *  
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class EvalPaths {

	public static final String OUTPUT_DIR; 
	public static final Stubs JC_STUBS;
	public static final String JC_CORPORATECARD;
	public static final String JC_WALLET;
	public static final String JC_PURSE;
	public static final String JC_SAFE;
	public static final Stubs JAVAGRANDE_STUBS;
	public static final String JAVAGRANDE_CP;
	public static final Stubs J2ME_STUBS;
	public static final String J2ME_BARCODE;
	public static final String J2ME_SAFE;
	public static final String J2ME_KEEPASS;
	public static final String J2ME_BEXPLORE;
	public static final String J2ME_ONETIMEPASS;
	public static final String JRE14_HSQLDB;

	static {
		final String propFile = loadPropertyOrFallback("joana.eval.prop-file", "eval.properties");
		OUTPUT_DIR = tryToLoadProperty("joana.eval.output", propFile);
		{
			final String jcStubStr = tryToLoadProperty("joana.jc.stubs", propFile); 
			JC_STUBS = Stubs.getStubForStr(jcStubStr);
			JC_CORPORATECARD = tryToLoadProperty("joana.jc.corporatecard", propFile);
			JC_WALLET = tryToLoadProperty("joana.jc.wallet", propFile);
			JC_PURSE = tryToLoadProperty("joana.jc.purse", propFile);
			JC_SAFE = tryToLoadProperty("joana.jc.safe", propFile);
		}
		{
			final String javagrandeStubStr = tryToLoadProperty("joana.javagrande.stubs", propFile); 
			JAVAGRANDE_STUBS = Stubs.getStubForStr(javagrandeStubStr);
			JAVAGRANDE_CP = tryToLoadProperty("joana.javagrande.cp", propFile);
		}
		{
			final String j2meStubStr = tryToLoadProperty("joana.j2me.stubs", propFile); 
			J2ME_STUBS = Stubs.getStubForStr(j2meStubStr);
			J2ME_BARCODE = tryToLoadProperty("joana.j2me.barcode", propFile);
			J2ME_SAFE = tryToLoadProperty("joana.j2me.safe", propFile);
			J2ME_KEEPASS = tryToLoadProperty("joana.j2me.keepass", propFile);
			J2ME_BEXPLORE = tryToLoadProperty("joana.j2me.bexplore", propFile);
			J2ME_ONETIMEPASS = tryToLoadProperty("joana.j2me.onetimepass", propFile);
		}
		{
			JRE14_HSQLDB = tryToLoadProperty("joana.jre14.hsqldb", propFile);
		}
	}
	
	public static String getOutputPath(final String fileName) throws EvalException {
		final String fullPath =
			OUTPUT_DIR + (!OUTPUT_DIR.isEmpty() && !OUTPUT_DIR.endsWith("/") ? "/" : "")  + fileName;
		if (fullPath.contains("/")) {
			final String pathToFile = fullPath.substring(0, fullPath.lastIndexOf("/"));
			final File path = new File(pathToFile);
			if (!path.exists()) {
				// try to create directory
				try {
					if (!path.mkdirs()) {
						// error failed to create output dir.
						throw new EvalException("could not create output dir '" + pathToFile + "'");
					}
				} catch (final SecurityException cause) {
					throw new EvalException("could not create output dir '" + pathToFile + "'", cause);
				}
			}

			if (!path.isDirectory()) {
				throw new EvalException("output dir '" + pathToFile + "' already exists, but is not a directory.");
			}
			
			if (!path.canWrite()) {
				throw new EvalException("output dir '" + pathToFile + "' is not writeable.");
			}
		}
		
		return fullPath;
	}
	
	private static String loadPropertyOrFallback(String key, String fallback) {
		String ret = System.getProperty(key);
		if (ret != null) {
			return ret;
		} else {
			return fallback;
		}
	}
	
	private static String tryToLoadProperty(String key, String propertiesFile) {
		String jPath = System.getProperty(key);
		if (jPath != null) {
			return jPath;
		} else {
			try {
				InputStream propertyStream = new FileInputStream(propertiesFile);
				Properties p = new Properties();
				p.load(propertyStream);
				jPath = p.getProperty(key);
			} catch (Throwable t) {
			}
			if (jPath != null) {
				return jPath;
			} else {
				throw new IllegalStateException("Property '"+ key + "' not provided! Either add file '"+ propertiesFile
					+ "' with an appropriate specification or provide property via -D flag to the jvm!");
			}
		}
	}

}
