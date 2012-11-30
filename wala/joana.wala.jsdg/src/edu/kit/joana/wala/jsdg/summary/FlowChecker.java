/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.jsdg.summary;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.wala.flowless.spec.ast.BooleanAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.ExplicitFlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor;
import edu.kit.joana.wala.flowless.spec.ast.FlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.InferableAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.ParameterOptList;
import edu.kit.joana.wala.flowless.spec.ast.PrimitiveAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.PureStmt;
import edu.kit.joana.wala.flowless.spec.ast.SimpleParameter;
import edu.kit.joana.wala.flowless.spec.ast.UniqueStmt;
import edu.kit.joana.wala.jsdg.summary.IntraprocSummaryEdges.SummaryGraph;

public final class FlowChecker implements FlowAstVisitor {

	private final Map<SimpleParameter, Set<SDGNode>> param2node;
	private final SummaryGraph summary;
	private final Set<FlowEdge> illegal;

	private FlowChecker(Map<SimpleParameter, Set<SDGNode>> param2node, SummaryGraph summary) {
		this.param2node = param2node;
		this.summary = summary;
		this.illegal = new HashSet<FlowEdge>();
	}

	public static Set<FlowEdge> searchIllegalFlow(Map<SimpleParameter, Set<SDGNode>> param2node,
			SummaryGraph summary, FlowStmt flow) throws FlowAstException {
		FlowChecker checker = new FlowChecker(param2node, summary);

		flow.accept(checker);

		return checker.illegal;
	}

	@Override
	public void visit(PrimitiveAliasStmt alias) throws FlowAstException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(UniqueStmt unique) throws FlowAstException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(BooleanAliasStmt alias) throws FlowAstException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(SimpleParameter param) throws FlowAstException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(ParameterOptList param) throws FlowAstException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(InferableAliasStmt alias) throws FlowAstException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(IFCStmt ifc) throws FlowAstException {
		for (FlowStmt flow : ifc.getFlowStmts()) {
			flow.accept(this);
		}
	}

	@Override
	public void visit(ExplicitFlowStmt ifc) throws FlowAstException {
		for (SimpleParameter from : ifc.getFrom()) {
			Set<SDGNode> fromNodes = param2node.get(from);

			if (fromNodes == null) {
				continue;
			}

			for (SimpleParameter to : ifc.getTo()) {
				Set<SDGNode> toNodes = param2node.get(to);

				if (toNodes == null) {
					continue;
				}

				final boolean edgeFound = summary.containsEdge(fromNodes, toNodes);
				if (ifc.isNoFlow() && edgeFound) {
					// illegal flow
					illegal.add(new FlowEdge(from, to, true));
				} else if (!ifc.isNoFlow() && !edgeFound) {
					// expected flow, but not found.
					illegal.add(new FlowEdge(from, to, false));
				}
			}
		}
	}

	@Override
	public void visit(PureStmt ifc) throws FlowAstException {
		for (SimpleParameter pureParam : ifc.getParams()) {
			Set<SDGNode> pureNodes = param2node.get(pureParam);
			boolean illegalAccess = false;
			for (SDGNode pure : pureNodes) {
				assert pure.kind == SDGNode.Kind.FORMAL_OUT;

				if (!summary.edgesOf(pure).isEmpty()) {
					illegalAccess = true;
					break;
				}
			}

			if (illegalAccess) {
				// TODO find all incoming flow to pureParam.....
				illegal.add(new FlowEdge(pureParam, pureParam, true));
			}
		}
	}

    public static class FlowEdge {
        public final SimpleParameter source;
        public final SimpleParameter target;
        public final boolean flowExists;

        private FlowEdge(SimpleParameter s, SimpleParameter t, boolean flowExists) {
            this.source = s;
            this.target = t;
            this.flowExists = flowExists;
        }

        public boolean equals(Object o) {
            if (o instanceof FlowEdge) {
                FlowEdge e = (FlowEdge) o;
                return (e.source == source && e.target == target && e.flowExists == flowExists);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return (flowExists ? 4711 : 1337) + (source.hashCode() | target.hashCode() << 16);
        }

        public String toString() {
            return source + (flowExists ? " -> " : " -!> ")+ target;
        }
    }

}
