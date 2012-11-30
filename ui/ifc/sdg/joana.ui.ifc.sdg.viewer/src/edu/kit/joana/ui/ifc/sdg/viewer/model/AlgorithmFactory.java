/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import edu.kit.joana.ui.ifc.sdg.viewer.algorithms.Algorithm;


/** This utility class offers methods to load algorithms and wrappers dynamically.
 *
 * @author giffhorn
 *
 */
public final class AlgorithmFactory {

	/** This method loads wrappers of type 'AbstractAlgorithm'
	 * The name of the demanded wrapper is contained in Algorithm 'alg'.
	 *
	 * @param alg    Contains the name of the demanded wrapper.
	 * @param graph  The graph that the analysis shall analyze.
	 * @return       The wrapper.
	 */
	@SuppressWarnings("unchecked")
    public static Run createAlgorithm(Algorithm alg, Graph g) {
		// the name of the wrapper
		String categoryClass = alg.getCategory();

		try {
			// load the class
			Class<?> cl = Class.forName(categoryClass);
			// get the constructors
			Constructor[] cons = cl.getConstructors();
			// the wrappers contain only one constructor
			Object o = cons[0].newInstance(alg.getClassName(), g);

			// return the wrapper
			return (Run) o;

		} catch(ClassNotFoundException ex) {
			ex.printStackTrace();

		} catch(InvocationTargetException ex) {
			ex.printStackTrace();

		} catch(InstantiationException ex) {
			ex.printStackTrace();

		} catch(IllegalAccessException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	/** Loads an algorithm of the Joana framework.
	 *
	 * @param className  The name of the algorithm
	 * @param graph      The graph to analyze.
	 * @return
	 */
	@SuppressWarnings("unchecked")
    public static Object loadClass(String className, Graph graph) {
		try {
			// load the class
			Class<?> cl = Class.forName(className);

		    // get the constructors
			Constructor[] cons = cl.getConstructors();

			// search for the constructor that need an SDG as parameter
			for (Constructor<?> c : cons) {
				Class[] params = c.getParameterTypes();

				if (params.length == 1
						&& params[0].getName().equals("edu.kit.joana.ifc.sdg.graph.SDG")) {

					// constructor found; create object and return
					Object o = c.newInstance(new Object[]{graph.getGraph()});

					return o;
				}
			}

		} catch(ClassNotFoundException ex) {
			ex.printStackTrace();

		} catch(InvocationTargetException ex) {
			ex.printStackTrace();

		} catch(InstantiationException ex) {
			ex.printStackTrace();

		} catch(IllegalAccessException ex) {
			ex.printStackTrace();
		}

		return null;
	}
}
