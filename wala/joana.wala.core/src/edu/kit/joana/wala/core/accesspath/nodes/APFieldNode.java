/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath.nodes;

import edu.kit.joana.wala.core.PDGField;
import edu.kit.joana.wala.core.PDGNode;

public final class APFieldNode extends APExpandNode {

	public static APFieldNode createFieldGet(final int iindex, final PDGField field, final PDGNode base) {
		if (field.base != base) {
			throw new IllegalArgumentException();
		}

		return new APFieldNode(iindex, field, base, Type.FIELD_GET);
	}

	public static APFieldNode createFieldSet(final int iindex, final PDGField field, final PDGNode base) {
		if (field.base != base) {
			throw new IllegalArgumentException();
		}

		return new APFieldNode(iindex, field, base, Type.FIELD_SET);
	}

	private APFieldNode(final int iindex, final PDGField field, final PDGNode base, final Type type) {
		super(iindex, type, base, field.field);

		if (field.field.isStatic()) {
			throw new IllegalArgumentException();
		} else if (type != Type.FIELD_GET && type != Type.FIELD_SET) {
			throw new IllegalArgumentException();
		}
	}

}
