/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.pointsto;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.ibm.wala.util.debug.UnimplementedError;

import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.RootParameter;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier.BasicFlowStmt;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier.BasicIFCStmt;
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
import edu.kit.joana.wala.flowless.spec.ast.SimpleParameter.ArrayContent;
import edu.kit.joana.wala.flowless.spec.ast.SimpleParameter.NormalPart;
import edu.kit.joana.wala.flowless.spec.ast.SimpleParameter.Part;
import edu.kit.joana.wala.flowless.spec.ast.UniqueStmt;
import edu.kit.joana.wala.util.ParamNum;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class MatchPTSWithFlowLess {

	private MatchPTSWithFlowLess() {
	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class ParameterMatchException extends FlowAstException {

		private static final long serialVersionUID = 1775598081057983188L;

		private final SimpleParameter param;

		public ParameterMatchException(SimpleParameter param, String message) {
			super(param + ": " + message);
			this.param = param;
		}

		public ParameterMatchException(SimpleParameter param, Throwable cause) {
			super(param + ": " + cause.getMessage(), cause);
			this.param = param;
		}

		public SimpleParameter getParam() {
			return param;
		}

	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public interface MatchResult {
		PtsParameter getMatch(Parameter param);
		boolean hasMatchFor(Parameter param);
		Set<Parameter> getResolvedParams();
	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	private static class ParameterMatcher implements FlowAstVisitor, MatchResult {
		
		private final Logger debug = Log.getLogger(Log.L_MOJO_DEBUG);
		private final AliasGraph graph;
		private final Map<Parameter, PtsParameter> mapping = new HashMap<Parameter, PtsParameter>();

		private ParameterMatcher(AliasGraph graph) {
			this.graph = graph;
		}

		public Set<Parameter> getResolvedParams() {
			return Collections.unmodifiableSet(mapping.keySet());
		}

		public PtsParameter getMatch(Parameter param) {
			return mapping.get(param);
		}

		public boolean hasMatchFor(Parameter param) {
			return mapping.containsKey(param);
		}

		@Override
		public void visit(PrimitiveAliasStmt alias) throws FlowAstException {
			for (Parameter param : alias.getParams()) {
				param.accept(this);
			}
		}

		@Override
		public void visit(UniqueStmt unique) throws FlowAstException {
			for (Parameter param : unique.getParams()) {
				param.accept(this);
			}
		}

		@Override
		public void visit(BooleanAliasStmt alias) throws FlowAstException {
			alias.left.accept(this);
			alias.right.accept(this);
		}

		@Override
		public void visit(SimpleParameter param) throws FlowAstException {
			findMatch(param);
		}

		@Override
		public void visit(ParameterOptList param) throws FlowAstException {
			for (SimpleParameter sp : param.getParams()) {
				sp.accept(this);
			}
		}

		@Override
		public void visit(IFCStmt ifc) throws FlowAstException {
			if (ifc.hasAliasStmt()) {
				ifc.getAliasStmt().accept(this);
			}

			if (ifc.hasFlowStmts()) {
				for (FlowStmt flow : ifc.getFlowStmts()) {
					flow.accept(this);
				}
			}
		}

		@Override
		public void visit(ExplicitFlowStmt ifc) throws FlowAstException {
			for (SimpleParameter from : ifc.getFrom()) {
				from.accept(this);
			}

			for (SimpleParameter to : ifc.getTo()) {
				try {
					to.accept(this);
				} catch (ParameterMatchException exc) {
					final SimpleParameter sp = exc.getParam();
					final ParamNum val = sp.getMappedTo();
					if (!(val.isException() || val.isResult())) {
						throw exc;
					} else {
						// ignore unmapped exception and result stuff as they are not part of input alias graphs (only output ones)
					}
				}
			}
		}

		@Override
		public void visit(PureStmt pure) throws FlowAstException {
			for (SimpleParameter from : pure.getParams()) {
				from.accept(this);
			}
		}

		private void findMatch(SimpleParameter param) throws FlowAstException {
			if (param.getRoot().getType() == Part.Type.STATE || param.getRoot().getType() == Part.Type.WILDCARD) {
				// nothing to match for state.
				throw new UnimplementedError();
			}

			RootParameter match = null;

			for (RootParameter root : graph.getRoots()) {
				if (root.getParamNum().equals(param.getMappedTo())) {
					// found match
					match = root;
					break;
				}
			}

			if (match != null) {
				debug.outln("Match: " + param + " <-> " + match);

				Stack<Part> partStack = new Stack<Part>();
				List<Part> invertedParts = new LinkedList<Part>(param.getParts());
				Collections.reverse(invertedParts);
				for (Part part : invertedParts) {
					partStack.push(part);
				}
				// remove root part that is already matched.
				partStack.pop();

				findFieldMatches(param, partStack, match);
			} else {
				throw new ParameterMatchException(param, "No match for root parameter: " + param);
			}
		}

		private void findFieldMatches(SimpleParameter param, Stack<Part> parts, PtsParameter parent) throws FlowAstException {
			if (parts.isEmpty()) {
				addMatch(param, parent);
				return;
			}

			final Part part = parts.pop();

			switch (part.getType()) {
			case ARRAY: {
				ArrayContent arr = (ArrayContent) part;
				PtsParameter child = parent.getArrayFieldChild();
				if (child != null) {
					findFieldMatches(param, parts, child);
				} else {
					throw new ParameterMatchException(param, "No array field child found for part " + arr);
				}
			} break;
			case NORMAL: {
				NormalPart norm = (NormalPart) part;
				PtsParameter child = parent.getChild(norm.name);
				if (child != null) {
					findFieldMatches(param, parts, child);
				} else {
					throw new ParameterMatchException(param, "No child found for part " + norm);
				}
			} break;
			case WILDCARD: {
				addMatch(param, parent);

				if (!parts.isEmpty()) {
					throw new ParameterMatchException(param, "Wildcard parameter part has further children.");
				}
			} break;
			default:
				throw new ParameterMatchException(param, "Middle parameter part is not a [], * or a normal parameter: " + part.getType());
			}
		}

		private void addMatch(Parameter param, PtsParameter match) {
			if (mapping.containsKey(param)) {
				throw new IllegalStateException("A match for parameter " + param + " already exists.");
			}

			mapping.put(param, match);
		}

		@Override
		public void visit(InferableAliasStmt alias) throws FlowAstException {
			// no parameter to match in here
		}

	}

	public static MatchResult findMatchingParams(AliasGraph graph, IFCStmt stmt) throws FlowAstException {
		ParameterMatcher matcher = new ParameterMatcher(graph);
		stmt.accept(matcher);

		return matcher;
	}

	public static MatchResult findMatchingParams(AliasGraph graph, Collection<BasicIFCStmt> stmts) throws FlowAstException {
		ParameterMatcher matcher = new ParameterMatcher(graph);

		for (BasicIFCStmt stmt : stmts) {
			for (PrimitiveAliasStmt aPlus : stmt.aPlus) {
				aPlus.accept(matcher);
			}

			for (PrimitiveAliasStmt aMinus : stmt.aMinus) {
				aMinus.accept(matcher);
			}

			for (ExplicitFlowStmt fPlus : stmt.flow.fPlus) {
				fPlus.accept(matcher);
			}

			for (ExplicitFlowStmt fMinus : stmt.flow.fMinus) {
				fMinus.accept(matcher);
			}
		}

		return matcher;
	}

	public static MatchResult findMatchingParams(AliasGraph graph, BasicIFCStmt stmt) throws FlowAstException {
		ParameterMatcher matcher = new ParameterMatcher(graph);

		for (PrimitiveAliasStmt aPlus : stmt.aPlus) {
			aPlus.accept(matcher);
		}

		for (PrimitiveAliasStmt aMinus : stmt.aMinus) {
			aMinus.accept(matcher);
		}

		return matcher;
	}

	public static MatchResult findMatchingParams(AliasGraph graph, BasicFlowStmt flow) throws FlowAstException {
		ParameterMatcher matcher = new ParameterMatcher(graph);

		for (ExplicitFlowStmt fPlus : flow.fPlus) {
			fPlus.accept(matcher);
		}

		for (ExplicitFlowStmt fMinus : flow.fMinus) {
			fMinus.accept(matcher);
		}

		return matcher;
	}

}
