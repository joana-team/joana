/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This Code crashes Wala Callgraphbuilding.
 * @author Martin Hecker (martin.hecker@kit.edu)
 */
public class Reflection {
	public static void main(String[] args) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
	    Method method = Thread.class.getMethod("getContextClassLoader", (Class<?>[])null);

	}
}
