/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Martin Mohr
 */
public class JoanaPath {

	public static final String JOANA_API_TEST_DATA_CLASSPATH;
	public static final String JOANA_API_TEST_DATA_GRAPHS;
	public static final String JOANA_MANY_SMALL_PROGRAMS_CLASSPATH;
	public static final String ANNOTATIONS_IGNORE_CLASSPATH;
	public static final String ANNOTATIONS_PASSON_CLASSPATH;
	
	static {
		String propFile = loadPropertyOrFallback("classpaths.prop-file", "classpaths.properties");
		JOANA_API_TEST_DATA_CLASSPATH = tryToLoadProperty("joana.api.testdata.classpath", propFile);
		JOANA_API_TEST_DATA_GRAPHS = tryToLoadProperty("joana.api.testdata.graphs", propFile);
		JOANA_MANY_SMALL_PROGRAMS_CLASSPATH = tryToLoadProperty("joana.many.small.programs.classpath", propFile);
		ANNOTATIONS_IGNORE_CLASSPATH = tryToLoadProperty("annotations.ignore.classpath", propFile);
		ANNOTATIONS_PASSON_CLASSPATH = tryToLoadProperty("annotations.passon.classpath", propFile);
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
				throw new IllegalStateException("Property '"+ key + "' not provided! Either add file '" + propertiesFile + "' with an appropriate specification or provide property via -D flag to the jvm!");
			}
		}
	}
}
