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

public abstract class APSimplePropagationNode extends APNode {

	public APSimplePropagationNode(final int iindex, final APNode.Type type, final PDGNode node) {
		super(iindex, type, node);
	}

	@Override
	public final AP propagate(AP ap) {
		return ap;
	}

}
