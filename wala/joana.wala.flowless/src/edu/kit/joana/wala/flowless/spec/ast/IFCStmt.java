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
public class IFCStmt implements AstElement {

	private int lineNr = 0;
	private final AliasStmt alias;
	private final List<FlowStmt> flow;

	public IFCStmt(AliasStmt alias, List<FlowStmt> flow) {
		this.alias = alias;
		this.flow = (flow == null ? new LinkedList<FlowStmt>() : flow);
	}

	public void setLineNr(final int lineNr) {
		this.lineNr = lineNr;
	}

	public int getLineNr() {
		return lineNr;
	}

	public AliasStmt getAliasStmt() {
		return alias;
	}

	public boolean shouldBeInferred() {
		return alias != null && alias.getType() == Type.ALIAS_INFER;
	}

	public boolean hasAliasStmt() {
		return alias != null;
	}

	public List<FlowStmt> getFlowStmts() {
		return Collections.unmodifiableList(flow);
	}

	public boolean hasFlowStmts() {
		return flow != null && !flow.isEmpty();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		if (alias != null) {
			sb.append(alias.toString());
		}

		if (flow != null) {
			sb.append(" => ");

			for (FlowStmt f : flow) {
				sb.append((f == null ? "null" : f.toString()));
				sb.append(", ");
			}

			sb.delete(sb.length() - 2, sb.length());
		}

		return sb.toString();
	}

	@Override
	public Type getType() {
		return Type.IFC;
	}

	@Override
	public void accept(FlowAstVisitor visitor) throws FlowAstException {
		visitor.visit(this);
	}

}
