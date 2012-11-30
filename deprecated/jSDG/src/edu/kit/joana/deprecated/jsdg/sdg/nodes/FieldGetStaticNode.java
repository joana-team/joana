/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;

public final class FieldGetStaticNode extends FieldAccessNode {

	private final HeapAccessCompound field;

	public FieldGetStaticNode(int id, ParameterField field, HeapAccessCompound fieldVal) {
		super(id, field);

		assert fieldVal.getAccess() == null;

		fieldVal.setAccess(this);

		this.field = fieldVal;
	}

	@Override
	public boolean isGet() {
		return true;
	}

	@Override
	public boolean isSet() {
		return false;
	}

	@Override
	public AbstractPDGNode getBaseValue() {
		throw new UnsupportedOperationException("A static field-get has no base object.");
	}

	@Override
	public AbstractPDGNode getFieldValue() {
		return field;
	}

	@Override
	public AbstractPDGNode getIndexValue() {
		throw new UnsupportedOperationException("A static field-get has no index value.");
	}

	@Override
	public AbstractPDGNode getSetValue() {
		throw new UnsupportedOperationException("A static field-get sets no value.");
	}

}
