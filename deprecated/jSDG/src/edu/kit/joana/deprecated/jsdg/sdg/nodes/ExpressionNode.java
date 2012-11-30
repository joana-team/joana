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
public class ExpressionNode extends AbstractPDGNode {

	public static enum Type { NORMAL, BINARY, CHECKCAST, COMPARE, CONVERT,
		CATCHEXCEPTION, SYNC };

	private Type type;

	ExpressionNode(int id) {
		super(id);
		this.type = Type.NORMAL;
	}

	public void accept(IPDGNodeVisitor visitor) {
		visitor.visitExpression(this);
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public boolean isGet() {
		return false;
	}

	public boolean isSet() {
		return false;
	}

	public boolean isArrayAccess() {
		return false;
	}

	public boolean isFieldAccess() {
		return false;
	}

	public boolean isStaticFieldAccess() {
		return false;
	}

	public ParameterField getField() {
		throw new UnsupportedOperationException("No field for standard expression.");
	}

	public AbstractPDGNode getBaseValue() {
		throw new UnsupportedOperationException("No field access.");
	}

	public AbstractPDGNode getFieldValue() {
		throw new UnsupportedOperationException("No field access.");
	}

	public AbstractPDGNode getIndexValue() {
		throw new UnsupportedOperationException("No field access.");
	}

	public AbstractPDGNode getSetValue() {
		throw new UnsupportedOperationException("No field access.");
	}

}
