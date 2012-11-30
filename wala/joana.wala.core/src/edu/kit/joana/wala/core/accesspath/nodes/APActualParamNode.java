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

public final class APActualParamNode extends APParamNode {

	public static APActualParamNode createParamInRoot(final int iindex, final PDGNode node) {
		return new APActualParamNode(iindex, Type.PARAM_IN, node);
	}

	public static APActualParamNode createParamInStatic(final int iindex, final PDGNode node, final ParameterField field) {
		assert field.isStatic();

		return new APActualParamNode(iindex, Type.PARAM_IN, node, field);
	}

	public static APActualParamNode createParamInChild(final APActualParamNode parent, final int iindex,
			final PDGNode node, final ParameterField field) {
		assert field != null && !field.isStatic();

		final APActualParamNode child = new APActualParamNode(iindex, Type.PARAM_IN, node, field);

		parent.addChild(child);

		return child;
	}

	public static APActualParamNode createParamOutRoot(final int iindex, final PDGNode node) {
		return new APActualParamNode(iindex, Type.PARAM_OUT, node);
	}

	public static APActualParamNode createParamOutStatic(final int iindex, final PDGNode node, final ParameterField field) {
		assert field.isStatic();

		return new APActualParamNode(iindex, Type.PARAM_OUT, node, field);
	}

	public static APActualParamNode createParamOutChild(final APActualParamNode parent, final int iindex,
			final PDGNode node, final ParameterField field) {
		assert field != null && !field.isStatic();

		final APActualParamNode child = new APActualParamNode(iindex, Type.PARAM_OUT, node, field);

		parent.addChild(child);

		return child;
	}

	private APActualParamNode(final int iindex, final Type type, final PDGNode node) {
		this(iindex, type, node, null);
	}

	private APActualParamNode(final int iindex, final Type type, final PDGNode node, final ParameterField field) {
		super(iindex, type, node, field);

		if (type != Type.PARAM_IN && type != Type.PARAM_OUT) {
			throw new IllegalArgumentException();
		}
	}

}
