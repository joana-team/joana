/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.mhpoptimization;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;

/**
 * In this implementation of {@link LoopDetermination}, a context is considered to be in a loop iff
 * one of the nodes of its call stack is in a loop.
 * @author Martin Mohr
 */
public abstract class ModularLoopDetermination implements LoopDetermination {

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.mhpoptimization.LoopDetermination#isInALoop(edu.kit.joana.ifc.sdg.graph.slicer.graph.Context)
	 */
	@Override
	public boolean isInALoop(Context c) {
		for (SDGNode n : c.getCallStack()) {
			if (isInALoop(n)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns whether the given sdg node may be contained in a loop.
	 * @param node sdg node to check
	 * @return {@code true} if the given sdg node may be contained in a loop, {@code false} otherwise
	 */
	protected abstract boolean isInALoop(SDGNode n);

}
