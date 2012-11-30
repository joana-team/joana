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
 * An inferable alias statement '?' should trigger the alias inference computation in order to compute
 * alias configurations under which the corresponding flow statement is satisfied.
 * e.g.: ? => a-!>b
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class InferableAliasStmt extends AliasStmt {

	private InferableAliasStmt() {}

	private static InferableAliasStmt stmt = null;

	public static InferableAliasStmt getInstance() {
		if (stmt == null) {
			stmt = new InferableAliasStmt();
		}

		return stmt;
	}

	@Override
	public Type getType() {
		return Type.ALIAS_INFER;
	}

	@Override
	public void accept(final FlowAstVisitor visitor) throws FlowAstException {
		visitor.visit(this);
	}

	public String toString() {
		return "?";
	}

}
