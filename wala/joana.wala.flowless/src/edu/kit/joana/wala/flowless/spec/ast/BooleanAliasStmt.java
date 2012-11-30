/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec.ast;

import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class BooleanAliasStmt extends AliasStmt {

	public static enum Operator { OR, AND };

	public final AliasStmt left;
	public final AliasStmt right;
	public final Operator op;

	public BooleanAliasStmt(AliasStmt left, AliasStmt right, Operator op) {
		this.left = left;
		this.right = right;
		this.op = op;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("(" + left.toString() + ")");
//		sb.append(left.toString());

		switch (op) {
		case AND:
			sb.append(" && ");
			break;
		case OR:
			sb.append(" || ");
			break;
		default:
			throw new IllegalStateException("Unknown boolean operator: " + op.name());
		}

		sb.append("(" + right.toString() + ")");
//		sb.append(right.toString());

		return sb.toString();
	}

	@Override
	public Type getType() {
		return Type.ALIAS_BOOL;
	}

	@Override
	public void accept(FlowAstVisitor visitor) throws FlowAstException {
		visitor.visit(this);
	}

}
