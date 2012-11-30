/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath.nodes;

import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.accesspath.AP;

public final class APNewNode extends APSimplePropagationNode {

	public static APNewNode create(final int iindex, final PDGNode node, final AP initial) {
		final APNewNode n = new APNewNode(iindex, node);
		n.addPath(initial);

		return n;
	}

	private APNewNode(final int iindex, final PDGNode node) {
		super(iindex, Type.NEW, node);

		if (node.getKind() != PDGNode.Kind.NEW) {
			throw new IllegalArgumentException();
		}
	}

}
