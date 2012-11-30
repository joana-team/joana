/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;


public abstract class FieldAccessNode extends ExpressionNode {

	private final ParameterField field;

	FieldAccessNode(int id, ParameterField field) {
		super(id);
		if (field == null) {
			throw new IllegalArgumentException();
		}

		this.field = field;
	}


	public abstract boolean isGet();
	public abstract boolean isSet();

	public abstract AbstractPDGNode getBaseValue();
	public abstract AbstractPDGNode getFieldValue();
	public abstract AbstractPDGNode getIndexValue();
	public abstract AbstractPDGNode getSetValue();

	public boolean isArrayAccess() {
		return field.isArray();
	}

	public boolean isFieldAccess() {
		return field.isField();
	}

	public boolean isStaticFieldAccess() {
		return field.isField() && field.isStatic();
	}

	public ParameterField getField() {
		return field;
	}

}
