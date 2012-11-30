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
public class PrimitiveAliasStmt extends AliasStmt {

	private final List<? extends Parameter> params;
	private final boolean isNegated;

	public PrimitiveAliasStmt(List<? extends Parameter> params, boolean isNegated) {
		this.params = params;
		this.isNegated = isNegated;
	}

	public List<Parameter> getParams() {
		return Collections.unmodifiableList(params);
	}

	public boolean isNegated() {
		return isNegated;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		if (isNegated) {
			sb.append("!");
		}

		sb.append("{");
		printListToBuffer(sb, params);
		sb.append("}");

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
	public Type getType() {
		return Type.ALIAS_PRIMITIVE;
	}

	@Override
	public void accept(FlowAstVisitor visitor) throws FlowAstException {
		visitor.visit(this);
	}

	public boolean contains(Parameter param) {
		for (Parameter p : params) {
			if (p.equals(param)) {
				return true;
			}
		}

		return false;
	}

	public boolean isSubsetOf(PrimitiveAliasStmt other) {
		if (isNegated != other.isNegated || params.size() > other.params.size()) {
			return false;
		}

		for (Parameter p : params) {
			if (!other.contains(p)) {
				return false;
			}
		}

		return true;
	}

	public int hashCode() {
		return getType().hashCode() + ((isNegated ? 13 : 27) * params.size());
	}

	public boolean equals(Object obj) {
		if (obj instanceof PrimitiveAliasStmt) {
			return equals((PrimitiveAliasStmt) obj);
		}

		return false;
	}

	public boolean equals(PrimitiveAliasStmt other) {
		return this.isSubsetOf(other) && other.isSubsetOf(this);
	}

}
