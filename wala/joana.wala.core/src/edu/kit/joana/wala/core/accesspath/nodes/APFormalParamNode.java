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

public final class APFormalParamNode extends APParamNode {

	public static APFormalParamNode createParamInRoot(final int iindex, final PDGNode node) {
		final APFormalParamNode n = new APFormalParamNode(iindex, Type.PARAM_IN, node);

		return n;
	}

	public static APFormalParamNode createParamInStatic(final int iindex, final PDGNode node, final ParameterField field) {
		assert field.isStatic();

		final APFormalParamNode n = new APFormalParamNode(iindex, Type.PARAM_IN, node, field);

		return n;
	}

	public static APFormalParamNode createParamInChild(final APFormalParamNode parent, final int iindex,
			final PDGNode node, final ParameterField field) {
		assert field != null && !field.isStatic();

		final APFormalParamNode n = new APFormalParamNode(iindex, Type.PARAM_IN, node, field);

		parent.addChild(n);

		return n;
	}

	public static APFormalParamNode createParamOutRoot(final int iindex, final PDGNode node) {
		final APFormalParamNode n = new APFormalParamNode(iindex, Type.PARAM_OUT, node);

		return n;
	}

	public static APFormalParamNode createParamOutStatic(final int iindex, final PDGNode node, final ParameterField field) {
		assert field.isStatic();

		final APFormalParamNode n = new APFormalParamNode(iindex, Type.PARAM_OUT, node, field);

		return n;
	}

	public static APFormalParamNode createParamOutChild(final APFormalParamNode parent, final int iindex,
			final PDGNode node, final ParameterField field) {
		assert field != null && !field.isStatic();

		final APFormalParamNode n = new APFormalParamNode(iindex, Type.PARAM_OUT, node, field);

		parent.addChild(n);

		return n;
	}

	public static APFormalParamNode createExit(final int iindex, final PDGNode node) {
		if (node.getKind() != PDGNode.Kind.EXIT) {
			throw new IllegalArgumentException();
		}

		final APFormalParamNode n = new APFormalParamNode(iindex, Type.PARAM_OUT, node);

		return n;
	}

	public static APFormalParamNode createException(final int iindex, final PDGNode node) {
		final APFormalParamNode n = new APFormalParamNode(iindex, Type.PARAM_OUT, node);

		return n;
	}

	// create root node
	private APFormalParamNode(final int iindex, final Type type, final PDGNode node) {
		this(iindex, type, node, null);
	}

	// create child node
	private APFormalParamNode(final int iindex, final Type type, final PDGNode node, final ParameterField field) {
		super(iindex, type, node, field);

		if (type != Type.PARAM_IN && type != Type.PARAM_OUT) {
			throw new IllegalArgumentException();
		}
	}

}
