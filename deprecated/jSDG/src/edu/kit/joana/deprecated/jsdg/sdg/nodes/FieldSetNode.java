/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;

public final class FieldSetNode extends FieldAccessNode {

	private final HeapAccessCompound base;
	private final HeapAccessCompound value;

	public FieldSetNode(int id, ParameterField field, HeapAccessCompound base, HeapAccessCompound value) {
		super(id, field);

		assert base.getAccess() == null;
		assert value.getAccess() == null;

		base.setAccess(this);
		value.setAccess(this);

		this.base = base;
		this.value = value;
	}

	@Override
	public boolean isGet() {
		return false;
	}

	@Override
	public boolean isSet() {
		return true;
	}

	@Override
	public AbstractPDGNode getBaseValue() {
		return base;
	}

	@Override
	public AbstractPDGNode getFieldValue() {
		throw new UnsupportedOperationException("A field-set reads no field value.");
	}

	@Override
	public AbstractPDGNode getIndexValue() {
		throw new UnsupportedOperationException("A field-set has no index value.");
	}

	@Override
	public AbstractPDGNode getSetValue() {
		return value;
	}

}
