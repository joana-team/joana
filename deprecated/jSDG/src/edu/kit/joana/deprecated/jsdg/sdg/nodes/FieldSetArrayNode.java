/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;

public final class FieldSetArrayNode extends FieldAccessNode {

	private final HeapAccessCompound base;
	private final HeapAccessCompound index;
	private final HeapAccessCompound value;

	public FieldSetArrayNode(int id, ParameterField field, HeapAccessCompound base,
			HeapAccessCompound index, HeapAccessCompound value) {
		super(id, field);

		assert base.getAccess() == null;
		assert index.getAccess() == null;
		assert value.getAccess() == null;

		base.setAccess(this);
		index.setAccess(this);
		value.setAccess(this);

		this.base = base;
		this.index = index;
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
		throw new UnsupportedOperationException("A array field-set reads no field value.");
	}

	@Override
	public AbstractPDGNode getIndexValue() {
		return index;
	}

	@Override
	public AbstractPDGNode getSetValue() {
		return value;
	}

}
