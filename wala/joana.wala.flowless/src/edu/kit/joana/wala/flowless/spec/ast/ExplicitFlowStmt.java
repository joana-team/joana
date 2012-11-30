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
public class ExplicitFlowStmt extends FlowStmt {

	private final List<SimpleParameter> from;
	private final List<SimpleParameter> to;
	private boolean noFlow = false;

	public ExplicitFlowStmt() {
		this.from = new LinkedList<SimpleParameter>();
		this.to = new LinkedList<SimpleParameter>();
	}

	public ExplicitFlowStmt(List<SimpleParameter> from, List<SimpleParameter> to) {
		this.from = from;
		this.to = to;
	}

	public void addFrom(SimpleParameter p) {
		from.add(p);
	}

	public void addTo(SimpleParameter p) {
		to.add(p);
	}

	public boolean isNoFlow() {
		return noFlow;
	}

	public void setNoFlow(boolean noFlow) {
		this.noFlow = noFlow;
	}

	public void negateNoFlow() {
		this.noFlow = ! this.noFlow;
	}

	public List<SimpleParameter> getFrom() {
		return Collections.unmodifiableList(from);
	}

	public List<SimpleParameter> getTo() {
		return Collections.unmodifiableList(to);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("(");
		printListToBuffer(sb, from);
		sb.append(noFlow ? ")-!>(" : ")->(");
		printListToBuffer(sb, to);
		sb.append(")");

		return sb.toString();
	}

	private static void printListToBuffer(StringBuffer sb, List<SimpleParameter> list) {
		if (list.size() > 1) {
			for (SimpleParameter p : list) {
				sb.append(p.toString());
				sb.append(", ");
			}

			sb.delete(sb.length() - 2, sb.length());
		} else {
			sb.append(list.get(0).toString());
		}
	}

	public static void negateNoFlow(List<ExplicitFlowStmt> list) {
		for (ExplicitFlowStmt flow : list) {
			flow.negateNoFlow();
		}
	}

	@Override
	public Type getType() {
		return Type.FLOW;
	}

	@Override
	public void accept(FlowAstVisitor visitor) throws FlowAstException {
		visitor.visit(this);
	}

}
