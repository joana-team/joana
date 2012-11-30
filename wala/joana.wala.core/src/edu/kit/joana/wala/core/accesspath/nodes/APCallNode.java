/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath.nodes;

import edu.kit.joana.wala.core.PDGNode;

public final class APCallNode extends APCallOrEntryNode {

	public APCallNode(final int iindex, final PDGNode call, final APActualParamNode[] paramIn,
			final APActualParamNode ret, final APActualParamNode exc) {
		super(iindex, call, paramIn, ret, exc, Type.CALL);

		if (call.getKind() != PDGNode.Kind.CALL) {
			throw new IllegalArgumentException();
		}
	}

}
