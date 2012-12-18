/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.mhpoptimization.loopdet;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;

/**
 * Classes implementing this interface provide a mechanism for determining all contexts in a program, which may be executed in a loop.
 * @author Martin Mohr
 */
public interface LoopDetermination {

	/**
	 * Returns whether the given context may be contained in a loop.
	 * @param c context to check
	 * @return {@code true} if the given context may be contained in a loop,
	 * {@code false} otherwise.
	 */
	public abstract boolean isInALoop(Context c);

}