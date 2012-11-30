/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;

public class HeapAccessCompound extends NormalNode {

	private FieldAccessNode facc;
	private final Type type;

	public static enum Type { BASE, FIELD, INDEX, VALUE }

	public HeapAccessCompound(int id, Type type) {
		super(id);
		this.type = type;
		super.setLabel(type.name());
	}

	public void setLabel(String label) {
		throw new UnsupportedOperationException();
	}

	public boolean isHeapCompound() {
		return true;
	}

	public Type getType() {
		return type;
	}

	public FieldAccessNode getAccess() {
		return facc;
	}

	public void setAccess(FieldAccessNode facc) {
		this.facc = facc;
	}

}
