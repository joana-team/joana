/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.sdg;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;

/**
 * Standard implementation of {@link SDGNodePredicate}. According to this implementation, an sdg node is interesting,
 * if it is an entry, exit, exception, return or call node, or if it represents a bytecode instruction.
 * @author Martin Mohr
 *
 */
public class DefaultSDGNodePredicate implements SDGNodePredicate {

	@Override
	public boolean isInteresting(SDGNode node) {
		final boolean isInteresting;
		switch (node.getKind()) {
		case ENTRY:
		case EXIT:
		case CALL:
			isInteresting = true;
			break;
		default:
			if (node.getBytecodeIndex() >= 0) {
				isInteresting = true;
			} else if (BytecodeLocation.EXCEPTION_PARAM.equals(node.getBytecodeName())
					|| BytecodeLocation.RETURN_PARAM.equals(node.getBytecodeName())) {
				isInteresting = true;
			} else {
				isInteresting = false;
			}
		}

		return isInteresting;
	}

}
