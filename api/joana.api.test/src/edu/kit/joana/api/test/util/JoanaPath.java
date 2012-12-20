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

	public static final String JOANA_PATH;

	static {
		String jPath = System.getProperty("joana.base.dir");
		if (jPath != null) {
			JOANA_PATH = jPath;
		} else {
			try {
				InputStream propertyStream = new FileInputStream("project.properties");
				Properties p = new Properties();
				p.load(propertyStream);
				jPath = p.getProperty("joana.base.dir");
			} catch (Throwable t) {
			}
			if (jPath != null) {
				JOANA_PATH = jPath;
			} else {
				throw new IllegalStateException("Property 'joana.base.dir' not provided! Either add a 'project.properties' with an appropriate specification or provide property via -D flag to the jvm!");
			}
		}

	}
}
