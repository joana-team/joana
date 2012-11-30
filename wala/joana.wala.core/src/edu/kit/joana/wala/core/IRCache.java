/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.IR;

public interface IRCache {

	public IR getIR(IMethod m);

}
