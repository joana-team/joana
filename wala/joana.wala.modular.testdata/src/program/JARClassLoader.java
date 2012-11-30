/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package program;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;
import java.util.Map;

public class JARClassLoader {

	private static Map classHash = new Hashtable();
	// used to mark non-existent classes in class hash
	private static final Object NO_CLASS = new Object();

	private final URLClassLoader loader;
	private final File jar;

	public JARClassLoader(File jar) {
		this.jar = jar;

		URL[] urls;
		try {
			urls = new URL[] { new URL("file://" + jar.getAbsolutePath()) };
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}

		this.loader = URLClassLoader.newInstance(urls);
	}

	public Class loadClass(String clazz) throws ClassNotFoundException {
		Object obj = classHash.get(clazz);

		if(obj == NO_CLASS)	{
			throw new ClassNotFoundException("No class named '" + clazz + "' found in " + jar.getAbsolutePath());
		} else if (obj == null) {
			try {
				obj = loader.loadClass(clazz);
			} catch (ClassNotFoundException cex) {
				classHash.put(clazz, NO_CLASS);
				throw new ClassNotFoundException("No class named '" + clazz + "' found in " + jar.getAbsolutePath());
			}

			if (obj != null) {
				classHash.put(clazz, obj);
			} else {
				// try to load form parent.
				classHash.put(clazz, NO_CLASS);
				throw new ClassNotFoundException("No class named '" + clazz + "' found in " + jar.getAbsolutePath());
			}
		}

		return (Class) obj;
	}

}
