/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.dictionary.accesspath;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.util.debug.UnimplementedError;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.wala.flowless.pointsto.MatchPTSWithFlowLess.ParameterMatchException;
import edu.kit.joana.wala.flowless.spec.ast.BooleanAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.ExplicitFlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor;
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
import edu.kit.joana.wala.util.ParamNum;
import edu.kit.joana.wala.util.ParamNum.PType;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class FlowLess2SDGMatcher implements Matcher, FlowAstVisitor {

	public static final String BYTECODE_VOID = "V";
	public static boolean printDebugMatches = false;

	private final SDG sdg;
	private final SDGNode methodEntry;
	private final List<SDGNode> methodParams;
	// mapping from parameter to root sdg node
	private final Map<Parameter, SDGNode> flow2sdg;
	// mapping from parameter to directly referenced input node
	private final Map<Parameter, SDGNode> flow2directIn;
	// mapping from parameter to directly referenced output node
	private final Map<Parameter, SDGNode> flow2directOut;
	private final boolean isStatic;

	private static boolean isThisPointer(final SDGNode n) {
		return n.getBytecodeName().equals(BytecodeLocation.ROOT_PARAM_PREFIX + "0") && n.getLabel().equals("this");
	}

	private FlowLess2SDGMatcher(final SDG sdg, final SDGNode methodEntry) {
		this.sdg = sdg;
		this.methodEntry = methodEntry;
		this.flow2sdg = new HashMap<Parameter, SDGNode>();
		this.flow2directIn = new HashMap<Parameter, SDGNode>();
		this.flow2directOut = new HashMap<Parameter, SDGNode>();
		this.methodParams = new LinkedList<SDGNode>();

		boolean mayBeStatic = true;
		for (final SDGEdge e : sdg.outgoingEdgesOf(methodEntry)) {
			if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
				final SDGNode n = e.getTarget();
				switch (n.kind) {
				case ACTUAL_IN:
				case FORMAL_IN:
					if (isThisPointer(n)) {
						mayBeStatic = false;
					}
				case ACTUAL_OUT:
				case FORMAL_OUT:
					methodParams.add(n);
					break;
				case EXIT:
					if (!n.getType().equals(BYTECODE_VOID)) {
						methodParams.add(n);
					}
					break;
				}
			}
		}

		this.isStatic = mayBeStatic;
	}

	public String toString() {
		return "Matcher for " + this.methodEntry.getLabel();
	}

	public static Matcher findMatchingNodes(final SDG sdg, final SDGNode methodEntry,
			final IFCStmt stmt) throws FlowAstException {
		final FlowLess2SDGMatcher matcher = new FlowLess2SDGMatcher(sdg, methodEntry);
		stmt.accept(matcher);

		return matcher;
	}

	@Override
	public SDGNode getMatch(final Parameter param) {
		return flow2sdg.get(param);
	}

	@Override
	public SDGNode getFineMatchIN(final Parameter param) {
		return flow2directIn.get(param);
	}

	@Override
	public SDGNode getFineMatchOUT(final Parameter param) {
		return flow2directOut.get(param);
	}

	@SuppressWarnings("unchecked")
	public Set<SDGNode> getFineReachIN(final Parameter param) {
		final SDGNode fineIn = getFineMatchIN(param);

		if (fineIn != null) {
			return findReachableIn(fineIn);
		}

		return (Set<SDGNode>) Collections.EMPTY_SET;
	}

	public Set<SDGNode> getFineReachOUT(final Parameter param) {
		final SDGNode fineIn = getFineMatchIN(param);
		final SDGNode fineOut = getFineMatchOUT(param);

		Set<SDGNode> out;

		if (fineIn != null) {
			out = findReachableOut(fineIn);
		} else {
			out = new HashSet<SDGNode>();
		}

		if (fineOut != null) {
			out.add(fineOut);
		}

		return out;
	}

	@Override
	public boolean hasMatchFor(final Parameter param) {
		return flow2sdg.containsKey(param);
	}

	@Override
	public Set<Parameter> getResolvedParams() {
		return Collections.unmodifiableSet(flow2sdg.keySet());
	}

	/**
	 * Registers matching sdg nodes for a given flowless parameter.
	 * A mapping from flow parameter tor root node is stored in flow2sdg.
	 * A mapping to the referenced input field node is stored in flow2directIn, iff such a sdg node exists.
	 * If no such node exists, then the corresponding method does not have side-effects concerning this parameter.
	 * A mapping to the referenced output field node is stored in flow2diretOut, iff such a sdg node exists.
	 * Iff the parameter ends with an wildcard, the sdg node matching the direct predecessor of the last part
	 * of the flow param is stored in the mapping (e.g. 'g' of flow param a.f.g.*).
	 * @param n The root node that matches the root part of the parameter.
	 * @param p The flowless parameter.
	 */
	private void addToMap(final SDGNode n, final SimpleParameter p) {
		flow2sdg.put(p, n);
		if (printDebugMatches) { System.err.println("found match: " + p + " <=> " + toStr(n)); }
		final List<Part> parts = p.getParts();
		final Part last = parts.get(parts.size() - 1);
		final Iterator<Part> it = parts.iterator();
		if (p.isStatic()) {
			// drop parts until the one equal to n is found - this is not always only the first part in case
			// of static variables e.g. my.pack.MyClass.field.f2.f1 -> [my, pack, MyClass, field, f2, f1]
			// is skipped until "field" : [f2, f1]
			Part toSkip;
			do {
				toSkip = it.next();
			} while (!toSkip.name.equals(n.getLabel()));
		} else {
			// drop only first part for all other non-static parameters
			it.next();
		}

		// shortcut for root nodes
		if (!it.hasNext()) {
			// add mapping to root node
			if (isInput(n)) {
				if (printDebugMatches) { System.err.println("direct in match: " + p + " <=> " + toStr(n)); }
				flow2directIn.put(p, n);
				if (p.isStatic()) {
					// search and map matching output root
					for (final SDGNode param : methodParams) {
						if (param.getBytecodeIndex() == BytecodeLocation.STATIC_FIELD && !isInput(param)
								&& param.getBytecodeName().equals(n.getBytecodeName())) {
							// found matching out
							if (printDebugMatches) { System.err.println("direct out match: " + p + " <=> " + toStr(param)); }
							flow2directOut.put(p, param);
							break;
						}
					}
				}
			} else {
				if (printDebugMatches) { System.err.println("direct out match: " + p + " <=> " + toStr(n)); }
				flow2directOut.put(p, n);
				if (p.isStatic()) {
					// search and map matching input root
					for (final SDGNode param : methodParams) {
						if (param.getBytecodeIndex() == BytecodeLocation.STATIC_FIELD && isInput(param)
								&& param.getBytecodeName().equals(n.getBytecodeName())) {
							// found matching in
							if (printDebugMatches) { System.err.println("direct in match: " + p + " <=> " + toStr(param)); }
							flow2directIn.put(p, param);
							break;
						}
					}
				}
			}

			return;
		}

		SDGNode current = n;
		while (current != null && it.hasNext()) {
			final Part curPart = it.next();
			// search matching sdg node
			if (curPart == last) {
				if (p.endsWithWildcard()) {
					// add current
					if (printDebugMatches) { System.err.println("direct in * match: " + p + " <=> " + toStr(current)); }
					flow2directIn.put(p, current);
				} else {
					// search part
					final SDGNode in = findChildIn(current, curPart);
					if (in != null) {
						if (printDebugMatches) { System.err.println("direct in match: " + p + " <=> " + toStr(in)); }
						flow2directIn.put(p, in);
					}

					final SDGNode out = findChildOut(current, curPart);
					if (out != null) {
						if (printDebugMatches) { System.err.println("direct out match: " + p + " <=> " + toStr(out)); }
						flow2directOut.put(p, out);
					}
				}
			} else {
				current = findChildIn(current, curPart);
			}
		}
	}

	private static String toStr(final SDGNode n) {
		return n.getKind() + "|" + n.getId() + "|" + n.getLabel();
	}

	private Set<SDGNode> findReachableIn(final SDGNode n) {
		final Set<SDGNode> reach = new HashSet<SDGNode>();

		final Set<SDGNode> visited = new HashSet<SDGNode>();
		final LinkedList<SDGNode> work = new LinkedList<SDGNode>();
		work.add(n);

		while (!work.isEmpty()) {
			final SDGNode cur = work.removeFirst();
			visited.add(cur);
			if (isInput(cur)) {
				reach.add(cur);
			}

			for (final SDGEdge e : sdg.outgoingEdgesOf(cur)) {
				if (e.getKind() == SDGEdge.Kind.PARAMETER_STRUCTURE && !visited.contains(e.getTarget())) {
					work.add(e.getTarget());
				}
			}
		}

		return reach;
	}

	private Set<SDGNode> findReachableOut(final SDGNode n) {
		final Set<SDGNode> reach = new HashSet<SDGNode>();

		final Set<SDGNode> visited = new HashSet<SDGNode>();
		final LinkedList<SDGNode> work = new LinkedList<SDGNode>();
		work.add(n);

		while (!work.isEmpty()) {
			final SDGNode cur = work.removeFirst();
			visited.add(cur);
			if (!isInput(cur)) {
				reach.add(cur);
			}

			for (final SDGEdge e : sdg.outgoingEdgesOf(cur)) {
				if (e.getKind() == SDGEdge.Kind.PARAMETER_STRUCTURE && !visited.contains(e.getTarget())) {
					work.add(e.getTarget());
				}
			}
		}

		return reach;
	}

	private SDGNode findChildIn(final SDGNode n, final Part p) {
		final String partStr = "." + p.name;
		for (final SDGEdge e : sdg.outgoingEdgesOf(n)) {
			if (e.getKind() == SDGEdge.Kind.PARAMETER_STRUCTURE && isInput(e.getTarget())) {
				final SDGNode child = e.getTarget();
				switch (p.getType()) {
				case ARRAY:
					if (child.getBytecodeIndex() == BytecodeLocation.ARRAY_FIELD) {
						return child;
					}
					break;
				case NORMAL:
					if (child.getBytecodeName().endsWith(partStr)) {
						return child;
					}
					break;
				default:
					throw new IllegalStateException("Do not know what to do eith an input child of type: " + p);
				}
			}
		}

		return null;
	}

	private SDGNode findChildOut(final SDGNode n, final Part p) {
		final String partStr = "." + p.name;
		for (final SDGEdge e : sdg.outgoingEdgesOf(n)) {
			if (e.getKind() == SDGEdge.Kind.PARAMETER_STRUCTURE && !isInput(e.getTarget())) {
				final SDGNode child = e.getTarget();
				switch (p.getType()) {
				case ARRAY:
					if (child.getBytecodeIndex() == BytecodeLocation.ARRAY_FIELD) {
						return child;
					}
					break;
				case NORMAL:
					if (child.getBytecodeName().endsWith(partStr)) {
						return child;
					}
					break;
				default:
					throw new IllegalStateException("Do not know what to do eith an input child of type: " + p);
				}
			}
		}

		return null;
	}

	private void findMapping(final SimpleParameter param) {
		switch (param.getRoot().getType()) {
		case ARRAY:
		case STATE:
		case WILDCARD:
			// nothing to match here
			throw new UnimplementedError("Do not know how to match " + param);
		case EXC:
			for (final SDGNode n : methodParams) {
				if (!isInput(n) && n.getBytecodeName().equals(BytecodeLocation.EXCEPTION_PARAM)) {
					addToMap(n, param);
				}
			}
			break;
		case RESULT:
			for (final SDGNode n : methodParams) {
				if (n.kind == Kind.EXIT) {
					addToMap(n, param);
				}
			}
			break;
		case NORMAL:
			if (param.getMappedTo().isUnmapped() || param.isStatic()) {
				// search for matching static variable
				searchAndMapStaticVar(param, true /* search for input parameter */);
			} else {
				final ParamNum paramNum = param.getMappedTo();
				final String toCheck = BytecodeLocation.ROOT_PARAM_PREFIX + paramNum.getNum();

				// we only map root nodes for now.
				for (final SDGNode n : methodParams) {
					if (n.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER && isInput(n)
							&& toCheck.equals(n.getBytecodeName())) {
						addToMap(n, param);
					}
				}
			}
			break;
		default:
			throw new UnimplementedError("Unknown param root type " + param.getRoot().getType() + " - " + param);
		}
	}

	private void searchAndMapStaticVar(final SimpleParameter p, final boolean input) {
		final List<Part> parts = p.getParts();
		for (final SDGNode n : methodParams) {
			if (n.getBytecodeIndex() == BytecodeLocation.STATIC_FIELD
					&& ((input && isInput(n)) || (!input && !isInput(n)))) {
				final LinkedList<String> np = extractBytecodeFieldNameParts(n.getBytecodeName());
				boolean match = true;
				final Iterator<Part> it = parts.iterator();
				for (final String name : np) {
					if (!it.hasNext()) {
						match = false;
						break;
					}
					final Part part = it.next();
					if (!part.name.equals(name)) {
						match = false;
						break;
					}
				}

				if (match) {
					if (!p.isStatic()) {
						p.setMappedTo(ParamNum.createSpecial(PType.STATIC_VAR_NO_NUM));
					}
					addToMap(n, p);
					break;
				}
			}
		}
	}

	private static LinkedList<String> extractBytecodeFieldNameParts(final String bcName) {
		// convert Lsome/package/ClassName.fieldName -> ["some", "package", "ClassName", "fieldName"]
		// convert LClassName.fieldName -> ["ClassName", "fieldName"]
		final LinkedList<String> l = new LinkedList<String>();

		int curPos = 1; //skip 'L'
		while (curPos < bcName.length()) {
			int nextPos = bcName.indexOf('/', curPos);
			if (nextPos <= 0) {
				nextPos = bcName.indexOf('.', curPos);
			}
			if (nextPos <= 0) {
				nextPos = bcName.length();
			}

			final String part = bcName.substring(curPos, nextPos);
			l.add(part);
			curPos = nextPos + 1;
		}

		return l;
	}

	private static boolean isInput(final SDGNode n) {
		return (n.kind == SDGNode.Kind.FORMAL_IN || n.kind == SDGNode.Kind.ACTUAL_IN);
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
		findMapping(param);
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
				SimpleParameter sp = exc.getParam();
				final ParamNum pn = sp.getMappedTo();
				if (!(pn.isException() || pn.isResult())) {
					throw exc;
				} else {
					// ignore unmapped exception and result stuff as they are not part of input alias graphs (only output ones)
				}
			}
		}
	}

	@Override
	public void visit(PureStmt ifc) throws FlowAstException {
		for (final SimpleParameter p : ifc.getParams()) {
			p.accept(this);
		}
	}

	@Override
	public void visit(InferableAliasStmt alias) throws FlowAstException {
		// really nothing to match here
	}

}
