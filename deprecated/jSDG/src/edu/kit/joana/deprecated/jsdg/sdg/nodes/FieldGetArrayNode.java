/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;

public final class FieldGetArrayNode extends FieldAccessNode {

	private final HeapAccessCompound base;
	private final HeapAccessCompound index;
	private final HeapAccessCompound field;

	public FieldGetArrayNode(int id, ParameterField field, HeapAccessCompound base,
			HeapAccessCompound index, HeapAccessCompound fieldVal) {
		super(id, field);

		assert base.getAccess() == null;
		assert index.getAccess() == null;
		assert fieldVal.getAccess() == null;

		base.setAccess(this);
		index.setAccess(this);
		fieldVal.setAccess(this);

		this.base = base;
		this.field = fieldVal;
		this.index = index;
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
		return base;
	}

	@Override
	public AbstractPDGNode getFieldValue() {
		return field;
	}

	@Override
	public AbstractPDGNode getIndexValue() {
		return index;
	}

	@Override
	public AbstractPDGNode getSetValue() {
		throw new UnsupportedOperationException("An array-get instruction sets no value.");
	}

}
