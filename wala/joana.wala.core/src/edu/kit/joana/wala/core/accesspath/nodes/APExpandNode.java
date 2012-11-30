/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath.nodes;

import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.accesspath.AP;

public abstract class APExpandNode extends APNode {

	private final ParameterField field;

	public APExpandNode(final int iindex, final APNode.Type type, final PDGNode node, final ParameterField field) {
		super(iindex, type, node);
		this.field = field;
	}

	@Override
	public final AP propagate(AP ap) {
		final AP subPath = ap.getSubPathTo(field);
		if (subPath != null) {
			return subPath;
		}

		return ap.append(field);
	}

}
