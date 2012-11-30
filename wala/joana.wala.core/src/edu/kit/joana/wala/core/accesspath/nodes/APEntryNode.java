/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath.nodes;

import edu.kit.joana.wala.core.PDGNode;


public final class APEntryNode extends APCallOrEntryNode {

	public APEntryNode(final int iindex, final PDGNode node, final APFormalParamNode[] paramIn,
			final APFormalParamNode ret, final APFormalParamNode exc) {
		super(iindex, node, paramIn, ret, exc, Type.ENTRY);

		if (node.getKind() != PDGNode.Kind.ENTRY) {
			throw new IllegalArgumentException();
		}
	}

}
