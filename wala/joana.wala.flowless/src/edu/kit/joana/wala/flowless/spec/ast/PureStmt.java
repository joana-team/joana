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
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class PureStmt extends FlowStmt {

	private final List<SimpleParameter> params;

	public PureStmt(List<SimpleParameter> params) {
		this.params = params;
	}

	public List<SimpleParameter> getParams() {
		return Collections.unmodifiableList(params);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("pure(");
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


	@Override
	public void accept(FlowAstVisitor visitor) throws FlowAstException {
		visitor.visit(this);
	}

	@Override
	public Type getType() {
		return Type.PURE;
	}

}
