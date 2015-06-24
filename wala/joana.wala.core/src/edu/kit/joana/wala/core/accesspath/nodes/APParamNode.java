/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath.nodes;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.types.TypeReference;

import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.accesspath.AP;

public abstract class APParamNode extends APSimplePropagationNode {

	private List<APParamNode> children;
	private final ParameterField field;

	public APParamNode(final int iindex, final APNode.Type type, final PDGNode node, final ParameterField field) {
		super(iindex, type, node);
		this.field = field;
	}

	public TypeReference getType() {
		return node.getTypeRef();
	}
	
	public boolean isRoot() {
		return field == null || field.isStatic();
	}

	public final boolean isOutput() {
		return type == APNode.Type.PARAM_OUT;
	}

	public final boolean isInput() {
		return type == APNode.Type.PARAM_IN;
	}

	public boolean addPath(AP ap) {
		final boolean change = super.addPath(ap);

		if (change && hasChildren()) {
			changed = true;

			for (final APParamNode child : children) {
				child.propagateDown(ap);
			}
		}

		return change;
	}

	/**
	 * Propagate changes to the access path down the parameter node tree. Append current field and
	 * propagate to children.
	 * @param path A path that corresponds to the base pointer of this node.
	 * @return true if any node in the tree has change its set of access paths.
	 */
	private void propagateDown(final AP path) {
		if (field != null) {
			AP sub = path.getSubPathTo(field);
	
			if (sub == null) {
				sub = path.append(field);
			}
	
			addPath(sub);
		} else {
			addPath(path);
		}
	}

	public boolean hasChildren() {
		return children != null && !children.isEmpty();
	}

	public void addChild(final APParamNode child) {
		assert child.field != null;
		assert !child.field.isStatic();

		if (children == null) {
			children = new LinkedList<APParamNode>();
		}

		children.add(child);
	}

	/**
	 * These children do not contain backlinks - there are no references to previously parents
	 */
	public Iterable<APParamNode> getChildren() {
		return (children != null ? Collections.unmodifiableList(children) : null);
	}

	public APParamNode findChild(final int bcIndex, final String bcName, final boolean isIn) {
		if (children != null) {
			for (final APParamNode child : children) {
				final PDGNode cn = child.node;
				if (nodeMatches(cn, bcIndex, bcName, isIn)) {
					return child;
				}
			}
		}

		return null;
	}

	private static final boolean nodeMatches(final PDGNode n, final int bcIndex, final String bcName, final boolean isIn) {
		return n.getBytecodeIndex() == bcIndex && bcName.equals(n.getBytecodeName())
				&& ((isIn && isInput(n)) || (!isIn && isOutput(n)));
	}

	public static boolean isInput(final PDGNode n) {
		switch (n.getKind()) {
		case ACTUAL_IN:
		case FORMAL_IN:
			return true;
		default: // nothing to do here
		}

		return false;
	}

	public static boolean isOutput(final PDGNode n) {
		switch (n.getKind()) {
		case ACTUAL_OUT:
		case FORMAL_OUT:
			return true;
		case EXIT:
			return n.getTypeRef() != TypeReference.Void;
		default: // nothing to do here
		}

		return false;
	}

}
