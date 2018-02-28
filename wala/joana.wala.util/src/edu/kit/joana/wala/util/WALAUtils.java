/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.JarStreamModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;

import joana.contrib.lib.Contrib;

/**
 * Various utility methods which are often needed.
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public final class WALAUtils {
	private WALAUtils() {
	}
	/**
	 * @param cha class hierarchy to look for main method in
	 * @param mainClass name of the class which to get main method of
	 * @return the IMethod which represents the main method of the given class in the given class hierarchy
	 * @throws RuntimeException if a class with the given name is not found in the given class hierarchy or if such a class is found but does not declare a main method
	 */
	public static IMethod findMainMethod(IClassHierarchy cha, String mainClass) {
		IClass cl = cha.lookupClass(TypeReference.findOrCreate(
				ClassLoaderReference.Application, mainClass));
		if (cl == null) {
			throw new RuntimeException("class not found: " + mainClass);
		}
		IMethod m = cl.getMethod(Selector.make("main([Ljava/lang/String;)V"));
		if (m == null) {
			throw new RuntimeException("main method of class " + cl + " not found!");
		}
		return m;
	}
	public static Module findJarModule(final String path) throws IOException {
		return findJarModule(null, path);
	}
	/**
	 * Search file in filesystem. If not found, try to load from classloader (e.g. from inside the jarfile).
	 */
	public static Module findJarModule(final PrintStream out, final String path) throws IOException {
		final File f = new File(path);
		if (f.exists()) {
			if (out != null) out.print("(from file " + path + ") ");
			return new JarFileModule(new JarFile(f));
		} else {
			final URL url = Contrib.class.getClassLoader().getResource(path);
			final URLConnection con = url.openConnection();
			final InputStream in = con.getInputStream();
			if (out != null) out.print("(from jar stream " + path + ") ");
			return new JarStreamModule(in);
		}
	}
}
