/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * Controls the creation of specific WALA Entrypoints.
 * @author Martin Mohr
 */
public interface EntryPointFactory {
	
	/**
	 * @return a new Entrypoint from a given IMethod, using the given class hierarchy
	 */
	Entrypoint make(IMethod m, IClassHierarchy cha);
}
