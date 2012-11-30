/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class SyncNode extends FieldAccessNode {

	SyncNode(int id) {
		super(id, ParameterFieldFactory.getFactory().getLockField());
		setType(Type.SYNC);
	}

	public void accept(IPDGNodeVisitor visitor) {
		visitor.visitSync(this);
	}

	@Override
	public boolean isGet() {
		return true;
	}

	@Override
	public boolean isSet() {
		return true;
	}

	@Override
	public AbstractPDGNode getBaseValue() {
		return this;
	}

	@Override
	public AbstractPDGNode getFieldValue() {
		return this;
	}

	@Override
	public AbstractPDGNode getIndexValue() {
		throw new UnsupportedOperationException("A sync node has no index value.");
	}

	@Override
	public AbstractPDGNode getSetValue() {
		return this;
	}

}
