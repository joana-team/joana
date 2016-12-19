/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;

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
	public static IMethod findMethod(IClassHierarchy cha, String mainClass) {
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
}
