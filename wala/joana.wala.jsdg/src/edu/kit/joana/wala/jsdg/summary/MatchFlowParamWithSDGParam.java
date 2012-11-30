/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.jsdg.summary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.wala.flowless.spec.ast.BooleanAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.ExplicitFlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.flowless.spec.ast.FlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.InferableAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.Parameter;
import edu.kit.joana.wala.flowless.spec.ast.ParameterOptList;
import edu.kit.joana.wala.flowless.spec.ast.PrimitiveAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.PureStmt;
import edu.kit.joana.wala.flowless.spec.ast.SimpleParameter;
import edu.kit.joana.wala.flowless.spec.ast.SimpleParameter.Part;
import edu.kit.joana.wala.flowless.spec.ast.UniqueStmt;
import edu.kit.joana.wala.util.NotImplementedException;
import edu.kit.joana.wala.util.ParamNum;

public class MatchFlowParamWithSDGParam {

	public static Map<SimpleParameter, Set<SDGNode>> mapParams2Nodes(SDG sdg, Set<SDGNode> nodesIn,
			Set<SDGNode> nodesOut, FlowStmt flow) throws FlowAstException {
		ParamMatchVisitor visitor = new ParamMatchVisitor(sdg, nodesIn, nodesOut);
		flow.accept(visitor);

		return visitor.param2nodes;
	}

	private static final class ParamMatchVisitor implements FlowAstVisitor {

		private final SDG sdg;
		private final Set<SDGNode> nodesIn;
		private final Set<SDGNode> nodesOut;

		private final Map<SimpleParameter, Set<SDGNode>> param2nodes;

		private boolean currentIsInParams = true;

		private ParamMatchVisitor(SDG sdg, Set<SDGNode> paramIn, Set<SDGNode> paramOut) {
			this.sdg = sdg;
			this.nodesIn = paramIn;
			this.nodesOut = new HashSet<SDGNode>();
			nodesOut.addAll(paramOut);
			nodesOut.addAll(paramIn);
			this.param2nodes = new HashMap<SimpleParameter, Set<SDGNode>>();
		}

		public void reset() {
			param2nodes.clear();
		}

		@Override
		public void visit(PrimitiveAliasStmt alias) throws FlowAstException {
			for (Parameter p : alias.getParams()) {
				p.accept(this);
			}
		}

		@Override
		public void visit(UniqueStmt unique) throws FlowAstException {
			for (Parameter p : unique.getParams()) {
				p.accept(this);
			}
		}

		@Override
		public void visit(BooleanAliasStmt alias) throws FlowAstException {
			alias.left.accept(this);
			alias.right.accept(this);
		}

		private static final SDGNode searchLabel(Set<SDGNode> nodes, String label) {
			for (SDGNode node : nodes) {
				if (label.equals(node.getLabel())) {
					return node;
				}
			}

			return null;
		}

		private static final SDGNode searchBCLabel(Set<SDGNode> nodes, final String label) {
			for (SDGNode node : nodes) {
				if (label.equals(node.getBytecodeName())) {
					return node;
				}
			}

			return null;
		}

		private static final class MatchTuple {
			public final SDGNode node;
			public final int part;

			public MatchTuple(SDGNode node, int part) {
				this.node = node;
				this.part = part;
			}
		}

		private void match(final SimpleParameter param, final SDGNode root) {
			final List<Part> parts = param.getParts();

			final LinkedList<MatchTuple> work = new LinkedList<MatchFlowParamWithSDGParam.ParamMatchVisitor.MatchTuple>();
			work.add(new MatchTuple(root, 1));

			while (!work.isEmpty()) {
				final MatchTuple cur = work.removeFirst();
				if (parts.size() <= cur.part) {
					// when we cant get deeper, we add the last node matched
					addMatch(param, cur.node);
					// match also all fields reachable from this node, because a -> b
					// means no flow from a or any of its fields to b or any if b's
					// fields.
					matchReachable(param, cur.node);
					continue;
				}
				final Part part = parts.get(cur.part);

				// add match if no sdg node child has been found...
				// CHANGE see below
//				boolean matchFound = false;

				switch (part.getType()) {
				case ARRAY: {
					for (final SDGEdge edge : sdg.outgoingEdgesOf(cur.node)) {
						if (edge.getKind() == Kind.PARAMETER_STRUCTURE) {
							SDGNode child = edge.getTarget();
							if (child.getBytecodeIndex() == BytecodeLocation.ARRAY_FIELD) {
								work.add(new MatchTuple(child, cur.part + 1));
//								matchFound = true;
							}
						}
					}
				} break;
				case NORMAL: {
					for (final SDGEdge edge : sdg.outgoingEdgesOf(cur.node)) {
						if (edge.getKind() == Kind.PARAMETER_STRUCTURE) {
							SDGNode child = edge.getTarget();
							if (child.getBytecodeIndex() == BytecodeLocation.OBJECT_FIELD
									&& namesMatch(child.getBytecodeName(), part.name)) {
								work.add(new MatchTuple(child, cur.part + 1));
//								matchFound = true;
							}
						}
					}
				} break;
				case WILDCARD: {
					// add all nodes reachable from current node
					matchReachable(param, cur.node);
//					matchFound = true;
				} break;
				case EXC:
				case RESULT:
				case STATE:
				default:
					throw new IllegalStateException("Did not expect to find a parameter middle part of type " + part.getType());
				}

//				if (!matchFound) {
					// no sdgnode matching the defined access path has been found.
					// in order to have conservative approximation, we tie the parameter specification to the last
					// sdg node that matched part of the access path.
					// CHANGE: we do not use this any longer, as we loose field sensitivity
					// e.g. when a field is never accessed by the method we still would add its parent.
//					addMatch(param, cur.node);
//				}
			}
		}

		private void matchReachable(final SimpleParameter param, final SDGNode node) {
			final Set<SDGNode> reachable = new HashSet<SDGNode>();

			final LinkedList<SDGNode> work = new LinkedList<SDGNode>();
			work.add(node);

			while (!work.isEmpty()) {
				final SDGNode cur = work.removeFirst();

				if (!reachable.contains(cur)) {
					reachable.add(cur);

					for (final SDGEdge edge : sdg.outgoingEdgesOf(cur)) {
						if (edge.getKind() == SDGEdge.Kind.PARAMETER_STRUCTURE) {
							work.add(edge.getTarget());
						}
					}
				}
			}

			// all nodes reachable from node except the node itself
			reachable.remove(node);

			addMatch(param, reachable);
		}

		/*
		 * bcName is a bytecode field anme like: "Ljava/lang/Throwable.elms [Ljava/lang/StackTraceElement"
		 * fName is the name of the concrete field like: "elms"
		 * This methods tries to find out if the concrete field name matches the bytecode name.
		 */
		private static boolean namesMatch(final String bcName, final String fName) {
			final int indexBlank = bcName.indexOf(' ');
			final int indexPoint = bcName.indexOf('.');
			if (indexBlank <= 0 || indexPoint <= 0) {
				return false;
			}

			assert indexBlank > indexPoint;
			final String bcFieldName = bcName.substring(indexPoint + 1, indexBlank);

			return fName.equals(bcFieldName);
		}

		@Override
		public void visit(SimpleParameter param) throws FlowAstException {
			final ParamNum mapped = param.getMappedTo();

			final Set<SDGNode> toSearch = (currentIsInParams ? nodesIn : nodesOut);

			if (toSearch.isEmpty()) {
				// special short cut to speed things up.
				return;
			}

			if (mapped.isNormalParam()) {
				// parameter is mapped to method parameter
				// search for root node with label "param <i>"

				final SDGNode root = searchLabel(toSearch, "param " + mapped.getNum());
				if (root != null) {
					assert root.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER;
					match(param, root);
				} else {
					// for out-parameters it is completely ok to not exist, iff they are never changed.
					// input root parameters however must be present.
					if (currentIsInParams) {
						throw new IllegalStateException("Could not find parameter with label '" + "param " + mapped + "'");
					}
				}
			} else if (mapped.isThis()) {
				// search for node with label "this"
				final SDGNode root = searchLabel(toSearch, "this");

				if (root != null) {
					assert root.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER;
					match(param, root);
				} else {
					// for out-parameters it is completely ok to not exist, iff they are never changed.
					// input root parameters however must be present.
					if (currentIsInParams) {
						throw new IllegalStateException("Could not find parameter with label 'this'");
					}
				}
			} else if (mapped.isException()) {
				assert !currentIsInParams;
				final SDGNode root = searchBCLabel(toSearch, BytecodeLocation.EXCEPTION_PARAM);
				if (root != null) {
					assert root.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER;
					match(param, root);
				} else {
					throw new IllegalStateException("Could not find an exception value node");
				}
			} else if (mapped.isResult()) {
				assert !currentIsInParams;
				final SDGNode root = searchBCLabel(toSearch, BytecodeLocation.RETURN_PARAM);
				if (root != null) {
					assert root.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER;
					match(param, root);
				} else {
					throw new IllegalStateException("Could not find a return value node");
				}
			} else if (mapped.isState()) {
				// map parameter to all in- or out nodes. - except the root nodes
				// and the nodes only reachable from the return value.
				for (SDGNode node : toSearch) {
					if (node.getBytecodeIndex() != BytecodeLocation.ROOT_PARAMETER
							&& !onlyReachableFromResult(node)) {
						addMatch(param, node);
					}
				}
			} else if (mapped.isAll()) {
				// add all params to the match set...
				for (SDGNode node : toSearch) {
					addMatch(param, node);
				}
			} else {
				throw new IllegalStateException("No default case for this parameter: " + param);
			}
		}

		private boolean onlyReachableFromResult(SDGNode node) {
			Set<SDGNode> reachable = new HashSet<SDGNode>();
			LinkedList<SDGNode> work = new LinkedList<SDGNode>();
			work.add(node);
			reachable.add(node);

			while (!work.isEmpty()) {
				SDGNode cur = work.removeFirst();

				for (SDGEdge edge : sdg.incomingEdgesOf(cur)) {
					if (edge.getKind() == SDGEdge.Kind.PARAMETER_STRUCTURE) {
						SDGNode src = edge.getSource();
						if (!reachable.contains(src)) {
							if (src.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER
									&& !src.getBytecodeName().equals(BytecodeLocation.RETURN_PARAM)) {
								// found a root parameter that is not the return value.
								return false;
							}

							work.add(src);
							reachable.add(src);
						}
					}
				}
			}

			return true;
		}

		private void addMatch(final SimpleParameter p, final SDGNode node) {
			assert p != null;
			assert node != null;

			if (currentIsInParams && (node.kind != SDGNode.Kind.ACTUAL_IN && node.kind != SDGNode.Kind.FORMAL_IN)) {
				return;
			} else if (!currentIsInParams && (node.kind == SDGNode.Kind.ACTUAL_IN || node.kind == SDGNode.Kind.FORMAL_IN)) {
				return;
			}

			Set<SDGNode> nodes = param2nodes.get(p);
			if (nodes == null) {
				nodes = new HashSet<SDGNode>();
				param2nodes.put(p, nodes);
			}

			nodes.add(node);
		}

		private void addMatch(final SimpleParameter p, final Set<SDGNode> sdgnodes) {
			assert p != null;
			assert sdgnodes != null && !sdgnodes.isEmpty();

			Set<SDGNode> nodes = param2nodes.get(p);
			if (nodes == null) {
				nodes = new HashSet<SDGNode>(sdgnodes);
				param2nodes.put(p, nodes);
			} else {
				nodes.addAll(sdgnodes);
			}
		}

		@Override
		public void visit(ParameterOptList param) throws FlowAstException {
			throw new NotImplementedException();
		}

		@Override
		public void visit(IFCStmt ifc) throws FlowAstException {
			if (ifc.hasAliasStmt()) {
				ifc.getAliasStmt().accept(this);
			}

			for (FlowStmt flow : ifc.getFlowStmts()) {
				flow.accept(this);
			}
		}

		@Override
		public void visit(ExplicitFlowStmt ifc) throws FlowAstException {
			for (SimpleParameter p : ifc.getFrom()) {
				p.accept(this);
			}

			currentIsInParams = false;
			for (SimpleParameter p : ifc.getTo()) {
				p.accept(this);
			}
			currentIsInParams = true;
		}

		@Override
		public void visit(PureStmt ifc) throws FlowAstException {
			for (SimpleParameter p : ifc.getParams()) {
				p.accept(this);
			}
		}

		@Override
		public void visit(InferableAliasStmt alias) throws FlowAstException {
		}

	}

}
