/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec.ast;

import java.util.Collections;
import java.util.List;

import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;

/**
 * A the unique property for an parameter or parameter fields means, that this
 * field is not aliased to anything else.
 *
 * Example:
 *
 * unique(a) // or 1(a) or uniq(a)
 * void foo(A a, A b)
 *
 * a is not aliased to b or any field of b or any field of itself.
 *
 * Example 2:
 *
 * unique(a.*) // or 1(a.*) or uniq(a.*)
 * void foo(A a, A b)
 *
 * a and all fields reachable through a are not aliased to anything else.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class UniqueStmt extends AliasStmt {

	private final List<? extends Parameter> params;

	public UniqueStmt(List<? extends Parameter> params) {
		this.params = params;
	}

	@Override
	public void accept(FlowAstVisitor visitor) throws FlowAstException {
		visitor.visit(this);
	}

	@Override
	public Type getType() {
		return Type.UNIQUE;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("1(");
		printListToBuffer(sb, params);
		sb.append(")");

		return sb.toString();
	}

	private static void printListToBuffer(StringBuffer sb, List<? extends Parameter> list) {
		int index = 1;
		for (Parameter p : list) {
			sb.append(p.toString());
			if (index < list.size()) {
				sb.append(", ");
			}
			index++;
		}
	}

	public List<Parameter> getParams() {
		return Collections.unmodifiableList(params);
	}

	public boolean contains(Parameter param) {
		for (Parameter p : params) {
			if (p.equals(param)) {
				return true;
			}
		}

		return false;
	}

}
