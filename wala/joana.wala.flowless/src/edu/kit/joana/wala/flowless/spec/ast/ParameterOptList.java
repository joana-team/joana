/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec.ast;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ParameterOptList extends Parameter {

	private final List<SimpleParameter> params = new LinkedList<SimpleParameter>();

	public void addParam(SimpleParameter param) {
		params.add(param);
	}

	@Override
	public boolean endsWithWildcard() {
		return false;
	}

	@Override
	public void accept(FlowAstVisitor visitor) throws FlowAstException {
		visitor.visit(this);
	}

	@Override
	public Type getType() {
		return Type.PARAM_OPT_LIST;
	}

	public List<SimpleParameter> getParams() {
		return Collections.unmodifiableList(params);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('[');

		int index = 1;
		for (SimpleParameter p : params) {
			sb.append(p.toString());
			if (index < params.size()) {
				sb.append(", ");
			}
			index++;
		}

		sb.append(']');

		return sb.toString();
	}

	public boolean equals(Parameter other) {
		if (other instanceof ParameterOptList) {
			ParameterOptList otherOpt = (ParameterOptList) other;
			if (params.size() == otherOpt.params.size()) {
				for (SimpleParameter sp : params) {
					boolean found = false;
					for (SimpleParameter spOther : otherOpt.params) {
						if (sp.equals(spOther)) {
							found = true;
							break;
						}
					}

					if (!found) {
						return false;
					}
				}
			}
		}

		return false;
	}
}
