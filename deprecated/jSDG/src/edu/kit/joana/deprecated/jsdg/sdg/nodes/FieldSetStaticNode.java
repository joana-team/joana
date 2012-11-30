/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;

public final class FieldSetStaticNode extends FieldAccessNode {

	private final HeapAccessCompound value;

	public FieldSetStaticNode(int id, ParameterField field, HeapAccessCompound value) {
		super(id, field);

		assert value.getAccess() == null;

		value.setAccess(this);

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
		throw new UnsupportedOperationException("A static field-set reads no base value.");
	}

	@Override
	public AbstractPDGNode getFieldValue() {
		throw new UnsupportedOperationException("A static field-set reads no field value.");
	}

	@Override
	public AbstractPDGNode getIndexValue() {
		throw new UnsupportedOperationException("A static field-set has no index value.");
	}

	@Override
	public AbstractPDGNode getSetValue() {
		return value;
	}

}
