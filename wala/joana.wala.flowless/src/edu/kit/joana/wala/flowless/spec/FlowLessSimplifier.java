/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.flowless.spec.ast.AliasStmt;
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
import edu.kit.joana.wala.flowless.spec.ast.UniqueStmt;
import edu.kit.joana.wala.flowless.spec.ast.AstElement.Type;
import edu.kit.joana.wala.flowless.spec.ast.BooleanAliasStmt.Operator;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.flowless.spec.ast.SimpleParameter.Wildcard;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class FlowLessSimplifier {

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class BasicIFCStmt {

		public final List<PrimitiveAliasStmt> aPlus = new LinkedList<PrimitiveAliasStmt>();
		public final List<PrimitiveAliasStmt> aMinus = new LinkedList<PrimitiveAliasStmt>();
		public final boolean shouldBeInferred;
		public final BasicFlowStmt flow;

		public BasicIFCStmt(final BasicFlowStmt flow, final boolean shouldBeInferred) {
			this.flow = flow;
			this.shouldBeInferred = shouldBeInferred;
		}

		public boolean isUndefined() {
			return aPlus.isEmpty() && aMinus.isEmpty();
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();

			if (shouldBeInferred) {
				sb.append("?");
				if (!aPlus.isEmpty() || !aMinus.isEmpty()) {
					sb.append(" & ");
				}
			}

			int index = 1;
			for (PrimitiveAliasStmt a : aPlus) {
				sb.append(a.toString());
				if (index < aPlus.size()) {
					sb.append(" & ");
				}
				index++;
			}

			if (!aPlus.isEmpty() && !aMinus.isEmpty()) {
				sb.append(" & ");
			}

			index = 1;
			for (PrimitiveAliasStmt a : aMinus) {
				sb.append(a.toString());
				if (index < aMinus.size()) {
					sb.append(" & ");
				}
				index++;
			}

			sb.append(" => ");
			sb.append(flow.toString());

			return sb.toString();
		}
	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class BasicFlowStmt {

		public final List<ExplicitFlowStmt> fPlus = new LinkedList<ExplicitFlowStmt>();
		public final List<ExplicitFlowStmt> fMinus = new LinkedList<ExplicitFlowStmt>();
		public final List<PureStmt> pure = new LinkedList<PureStmt>();

		public String toString() {
			StringBuffer sb = new StringBuffer();

			int index = 1;
			for (ExplicitFlowStmt a : fPlus) {
				sb.append(a.toString());
				if (index < fPlus.size()) {
					sb.append(" & ");
				}
				index++;
			}

			if (!fPlus.isEmpty() && !fMinus.isEmpty()) {
				sb.append(" & ");
			}

			index = 1;
			for (ExplicitFlowStmt a : fMinus) {
				sb.append(a.toString());
				if (index < fMinus.size()) {
					sb.append(" & ");
				}
				index++;
			}

			if (!fPlus.isEmpty() && !fMinus.isEmpty() && !pure.isEmpty()) {
				sb.append(" & ");
			}

			index = 1;
			for (PureStmt a : pure) {
				sb.append(a.toString());
				if (index < pure.size()) {
					sb.append(" & ");
				}
				index++;
			}

			return sb.toString();
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof BasicFlowStmt) {
				// BasicFlowStmt other = (BasicFlowStmt) obj;
				throw new IllegalStateException("Not implemented.");
			}

			return false;
		}

	}

	private FlowLessSimplifier() {
	}

	/**
	 * Computes a lower bound of a set of basic ifc statements with
	 * respect to the alias statements. The flow statements of the basic ifc
	 * statements have to be all equal.
	 * The lower bound is found by including only primitive alias set, that
	 * are part of all ifc statements in the provided list. If two ifc statemtnts
	 * have a alias set that is subset of the other one, the subset is added to the result.
	 *
	 * TODO: If an alias set has to be removed, but an subset of this set isnt contained
	 * in all ifc statements, this subset is used.
	 *
	 * <pre>
	 * Example:
	 * Input:
	 * 		{{x, y}, {v, w}} & {!{a, b}}    => {a->b, x->c} & {c-!>d}
	 * 		{{c, d}, {x, y}} & {!{a, b, c}} => {a->b, x->c} & {c-!>d}
	 * Output:
	 * 		{{x, y}}         & {!{a, b}}    => {a->b, x->c} & {c-!>d}
	 * </pre>
	 * @param stmts List of basic ifc statments a lower bound is computed from
	 * @return an basic ifc statement that is a lower bound
	 */
	public static BasicIFCStmt lowerBound(List<BasicIFCStmt> stmts) {
		if (!allStmtsHaveSameFlow(stmts)) {
			throw new IllegalArgumentException("All statements should have the same flow statement.");
		}

		final BasicFlowStmt flow;
		final boolean infere;
		if (stmts.size() > 0) {
			final BasicIFCStmt first = stmts.get(0);
			flow = first.flow;
			infere = first.shouldBeInferred;
		} else {
			infere = false;
			flow = new BasicFlowStmt();
		}

		final BasicIFCStmt infimum = new BasicIFCStmt(flow, infere);

		// initialize with sets from first stmt
		if (stmts.size() > 0) {
			BasicIFCStmt first = stmts.get(0);
			infimum.aPlus.addAll(first.aPlus);
			infimum.aMinus.addAll(first.aMinus);
		}

		for (BasicIFCStmt stmt : stmts) {
			adjustSetToContainInfimum(infimum.aPlus, stmt.aPlus);
			adjustSetToContainInfimum(infimum.aMinus, stmt.aMinus);
		}

		return infimum;
	}

	private static boolean adjustSetToContainInfimum(List<PrimitiveAliasStmt> set, List<PrimitiveAliasStmt> other) {
		boolean changed = false;

		// 1. add elements that are subsets of already contained elements
		Set<PrimitiveAliasStmt> toAdd = new HashSet<PrimitiveAliasStmt>();
		for (PrimitiveAliasStmt setStmt : set) {
			boolean shouldBeAdded = false;

			for (PrimitiveAliasStmt otherStmt : other) {
				if (otherStmt.isSubsetOf(setStmt) && !setStmt.isSubsetOf(otherStmt)) {
					// add real subsets
					shouldBeAdded = true;
					break;
				}
			}

			if (shouldBeAdded) {
				toAdd.add(setStmt);
			}
		}

		if (!toAdd.isEmpty()) {
			set.addAll(toAdd);
			changed = true;
		}

		// 2. remove elements that are not contained  in other
		Set<PrimitiveAliasStmt> toRemove = new HashSet<PrimitiveAliasStmt>();
		for (PrimitiveAliasStmt setStmt : set) {
			boolean canStayInSet = false;

			for (PrimitiveAliasStmt otherStmt : other) {
				if (setStmt.isSubsetOf(otherStmt)) {
					// remove elements that are not contained in the other set
					canStayInSet = true;
					break;
				}
			}

			if (!canStayInSet) {
				toRemove.add(setStmt);
			}
		}

		if (!toRemove.isEmpty()) {
			set.removeAll(toRemove);
			changed = true;
		}

		return changed;
	}

	/**
	 * Computes an upper bound of a set of basic ifc statements with
	 * respect to the alias statements. The flow statements of the basic ifc
	 * statements have to be all equal.
	 * The upper bound is found by adding each primitive alias set, that
	 * is not already included in the result. If the result contains a alias set
	 * that is a subset of another element, the subset is removed.
	 *
	 * <pre>
	 * Example:
	 * Input:
	 * 		{{x, y}, {v, w}}         & {!{a, b}}    => {a->b, x->c} & {c-!>d}
	 * 		{{c, d}, {x, y}}         & {!{a, b, c}} => {a->b, x->c} & {c-!>d}
	 * Output:
	 * 		{{c, d}, {x, y}, {v, w}} & {!{a, b, c}} => {a->b, x->c} & {c-!>d}
	 * </pre>
	 * @param stmts List of basic ifc statments an upper bound is computed from
	 * @return an basic ifc statement that is an upper bound
	 */
	public static BasicIFCStmt upperBound(List<BasicIFCStmt> stmts) {
		if (!allStmtsHaveSameFlow(stmts)) {
			throw new IllegalArgumentException("All statements should have the same flow statement.");
		}

		final BasicFlowStmt flow;
		final boolean infere;
		if (stmts.size() > 0) {
			final BasicIFCStmt first = stmts.get(0);
			flow = first.flow;
			infere = first.shouldBeInferred;
		} else {
			infere = false;
			flow = new BasicFlowStmt();
		}

		final BasicIFCStmt supremum = new BasicIFCStmt(flow, infere);

		for (BasicIFCStmt stmt : stmts) {
			for (PrimitiveAliasStmt other : stmt.aPlus) {
				adjustSetToContainSupremum(supremum.aPlus, other);
			}

			for (PrimitiveAliasStmt other : stmt.aMinus) {
				adjustSetToContainSupremum(supremum.aMinus, other);
			}
		}

		return supremum;
	}

	private static boolean adjustSetToContainSupremum(List<PrimitiveAliasStmt> set, PrimitiveAliasStmt other) {
		boolean changed = false;
		boolean mustNotBeInserted = false;

		Set<PrimitiveAliasStmt> toExchange = new HashSet<PrimitiveAliasStmt>();

		for (PrimitiveAliasStmt contained : set) {
			if (other.isSubsetOf(contained)) {
				// we can ignore this alias
				mustNotBeInserted = true;
			} else if (contained.isSubsetOf(other)) {
				toExchange.add(contained);
			}
		}

		if (!toExchange.isEmpty()) {
			changed = true;
			set.removeAll(toExchange);
		}

		if (!mustNotBeInserted) {
			changed = true;
			set.add(other);
		}

		return changed;
	}

	private static boolean allStmtsHaveSameFlow(List<BasicIFCStmt> set) {
		if (set.size() > 0) {
			BasicFlowStmt flow = set.get(0).flow;

			for (BasicIFCStmt stmt : set) {
				if (!flow.equals(stmt.flow)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Computes a set of basic ifc statements from an ifc statement. A basic
	 * ifc statements contains no boolean alias statments (like {a, b} || {c, d}).
	 * It only consists of a list of primitive alias sets ({{a,b}, {c, d}, ..})
	 * and a list of primitive no-alias sets ({!{x, y}, !{v, w}, ..}).
	 * The flow statements are also split up in a list of primitive negated and
	 * primitive non-negated flow statemtents. This basic ifc statements may
	 * then be used to compute the initial points-to sets for the following ifc
	 * analysis of the described component.
	 *
	 * Example:
	 * <pre>
	 * Input:
	 * 		(!{a, b} | {c, d}) & {x, y}  => a->b, c-!>d, x->c
	 * Output:
	 * 		{{x, y}}         & {!{a, b}} => {a->b, x->c} & {c-!>d}
	 * 		{{c, d}, {x, y}} & {}        => {a->b, x->c} & {c-!>d}
	 * </pre>
	 * @param ifc The normal (non-basic) ifc statement that should be converted.
	 * @return A list of simplified basic ifc statements that conform to the
	 * provided ifc statement.
	 * @throws FlowAstException
	 */
	public static List<BasicIFCStmt> simplify(final IFCStmt ifc) throws FlowAstException {
		List<BasicIFCStmt> result = new LinkedList<BasicIFCStmt>();
		final Logger debug = Log.getLogger(Log.L_MOJO_DEBUG);

		debug.outln("BEFORE:    " + ifc.toString());

		/*
		 * 1. Convert parameter opt list to boolean or expression
		 * 2. Build value table for boolean expression
		 * 3. Extract normal form from table
		 */
		final IFCStmt ifcNoOpt = removeParameterOptList(ifc);

		debug.outln("AFTER (1): " + ifcNoOpt.toString());

//		List<PrimitiveAliasStmt> primitives = extractPrimitives(ifcNoOpt);
//		for (PrimitiveAliasStmt pri : primitives) {
//			System.out.println("\tPRIMITIVE: " + pri.toString());
//		}

		final BasicFlowStmt flow = new BasicFlowStmt();
		for (FlowStmt fstmt : ifc.getFlowStmts()) {
			if (fstmt.getType() == Type.FLOW) {
				ExplicitFlowStmt efstmt = (ExplicitFlowStmt) fstmt;
				if (efstmt.isNoFlow()) {
					flow.fMinus.add(efstmt);
				} else {
					flow.fPlus.add(efstmt);
				}
			} else if (fstmt.getType() == Type.PURE) {
				PureStmt pstmt = (PureStmt) fstmt;
				flow.pure.add(pstmt);
			} else {
				throw new IllegalStateException("Unknown type of flow stmt: " + fstmt.getType());
			}
		}

		List<List<PrimitiveAliasStmt>> variants = extractVariants(ifcNoOpt);
		for (List<PrimitiveAliasStmt> variant : variants) {
//			System.out.print("\tRELEVANT: " );

			final BasicIFCStmt basic = new BasicIFCStmt(flow, ifcNoOpt.shouldBeInferred());

			for (PrimitiveAliasStmt stmt : variant) {
				if (stmt.isNegated()) {
					basic.aMinus.add(stmt);
				} else {
					basic.aPlus.add(stmt);
				}

//				System.out.print(stmt.toString() + " & ");
			}

			result.add(basic);

			debug.outln("\t-> " + basic.toString());
		}

		if (variants.isEmpty()) {
			final BasicIFCStmt basic = new BasicIFCStmt(flow,  ifcNoOpt.shouldBeInferred());
			result.add(basic);
			debug.outln("\t-> " + basic.toString());
		}

		return result;
	}

	private static List<List<PrimitiveAliasStmt>> extractVariants(IFCStmt ifc) {
		List<List<PrimitiveAliasStmt>> result = new LinkedList<List<PrimitiveAliasStmt>>();

		if (ifc.hasAliasStmt()) {
			result = extractVariants(ifc.getAliasStmt());
		}

		return result;
	}

	private static List<List<PrimitiveAliasStmt>> extractVariants(AliasStmt stmt) {
		List<List<PrimitiveAliasStmt>> result = new LinkedList<List<PrimitiveAliasStmt>>();
		switch (stmt.getType()) {
		case ALIAS_BOOL: {
			BooleanAliasStmt bstmt = (BooleanAliasStmt) stmt;
			switch (bstmt.op) {
			case AND: {
				List<List<PrimitiveAliasStmt>> left = extractVariants(bstmt.left);
				List<List<PrimitiveAliasStmt>> right = extractVariants(bstmt.right);
				for (List<PrimitiveAliasStmt> l : left) {
					for (List<PrimitiveAliasStmt> r : right) {
						List<PrimitiveAliasStmt> list = new LinkedList<PrimitiveAliasStmt>();
						list.addAll(l);
						list.addAll(r);
						result.add(list);
					}
				}
			} break;
			case OR: {
				List<List<PrimitiveAliasStmt>> left = extractVariants(bstmt.left);
				result.addAll(left);
				List<List<PrimitiveAliasStmt>> right = extractVariants(bstmt.right);
				result.addAll(right);
			} break;
			default: {
				throw new IllegalStateException();
			}
			}
		} break;
		case ALIAS_PRIMITIVE: {
			List<PrimitiveAliasStmt> list = new LinkedList<PrimitiveAliasStmt>();
			list.add((PrimitiveAliasStmt) stmt);
			result.add(list);
		} break;
		case UNIQUE: {
			UniqueStmt unique = (UniqueStmt) stmt;
			List<PrimitiveAliasStmt> list = new LinkedList<PrimitiveAliasStmt>();

			/*
			 * Build not-alias statements from unique statements
			 * unique(a, b, c) == !{a, *} & !{b, *} & !{c, *}
			 */
			for (Parameter p : unique.getParams()) {
				List<Parameter> params = new LinkedList<Parameter>();
				params.add(p);
				SimpleParameter wildcard = new SimpleParameter(new Wildcard());
				params.add(wildcard);

				PrimitiveAliasStmt notAlias = new PrimitiveAliasStmt(params, true);
				list.add(notAlias);
			}

			result.add(list);
		} break;
		case ALIAS_INFER: {
			// no predefined alias configuration here, so nothing to add
		} break;
		default: {
			throw new IllegalStateException();
		}
		}

		return result;
	}

	@Deprecated
	private static class CollectPrimitivesVisitor implements FlowAstVisitor {

		private final List<PrimitiveAliasStmt> list;

		public CollectPrimitivesVisitor(List<PrimitiveAliasStmt> list) {
			this.list = list;
		}

		@Override
		public void visit(PrimitiveAliasStmt alias) throws FlowAstException {
			list.add(alias);
		}

		@Override
		public void visit(BooleanAliasStmt alias) throws FlowAstException {
			alias.left.accept(this);
			alias.right.accept(this);
		}

		@Override
		public void visit(SimpleParameter param) throws FlowAstException {
		}

		@Override
		public void visit(ParameterOptList param) throws FlowAstException {
		}

		@Override
		public void visit(IFCStmt ifc) throws FlowAstException {
			if (ifc.hasAliasStmt()) {
				ifc.getAliasStmt().accept(this);
			}
		}

		@Override
		public void visit(ExplicitFlowStmt ifc) throws FlowAstException {
		}

		@Override
		public void visit(UniqueStmt unique) throws FlowAstException {
			/*
			 * Build not-alias statements from unique statements
			 * unique(a, b, c) == !{a, *} & !{b, *} & !{c, *}
			 */
			for (Parameter p : unique.getParams()) {
				List<Parameter> params = new LinkedList<Parameter>();
				params.add(p);
				SimpleParameter wildcard = new SimpleParameter(new Wildcard());
				params.add(wildcard);

				PrimitiveAliasStmt notAlias = new PrimitiveAliasStmt(params, true);
				list.add(notAlias);
			}
		}

		@Override
		public void visit(PureStmt ifc) throws FlowAstException {
		}

		@Override
		public void visit(InferableAliasStmt alias) throws FlowAstException {
			// an inferable statement is empty
		}

	}

	@SuppressWarnings("unused")
	private static List<PrimitiveAliasStmt> extractPrimitives(IFCStmt ifc) throws FlowAstException {
		List<PrimitiveAliasStmt> primitives = new LinkedList<PrimitiveAliasStmt>();
		CollectPrimitivesVisitor visitor = new CollectPrimitivesVisitor(primitives);

		ifc.accept(visitor);

		return primitives;
	}

	private static IFCStmt removeParameterOptList(IFCStmt ifc) {
		IFCStmt result = ifc;

		if (ifc.hasAliasStmt()) {
			AliasStmt alias = removeParameterOptList(ifc.getAliasStmt());
			if (alias != ifc.getAliasStmt()) {
				result = new IFCStmt(alias, ifc.getFlowStmts());
			}
		}

		return result;
	}

	private static AliasStmt removeParameterOptList(AliasStmt alias) {
		AliasStmt result = alias;

		switch (alias.getType()) {
		case ALIAS_BOOL: {
			BooleanAliasStmt bstmt = (BooleanAliasStmt) alias;
			AliasStmt newLeft = removeParameterOptList(bstmt.left);
			AliasStmt newRight = removeParameterOptList(bstmt.right);
			result = new BooleanAliasStmt(newLeft, newRight, bstmt.op);
		} break;
		case ALIAS_PRIMITIVE: {
			PrimitiveAliasStmt pstmt = (PrimitiveAliasStmt) alias;
			List<List<SimpleParameter>> resolved = resolveOptList(pstmt.getParams());
			List<PrimitiveAliasStmt> statements = new LinkedList<PrimitiveAliasStmt>();
			expandParamsToPrimitiveAlias(resolved, new LinkedList<SimpleParameter>(), statements, pstmt.isNegated());

			// build boolean or-expressions from list
			if (statements.size() > 1) {
				BooleanAliasStmt boolStmt = null;
				PrimitiveAliasStmt firstStmt = null;
				Collections.reverse(statements);

				for (PrimitiveAliasStmt stmt : statements) {
					if (firstStmt == null) {
						firstStmt = stmt;
					} else if (boolStmt == null) {
						boolStmt = new BooleanAliasStmt(stmt, firstStmt, Operator.OR);
					} else {
						BooleanAliasStmt newBoolStmt = new BooleanAliasStmt(stmt, boolStmt, Operator.OR);
						boolStmt = newBoolStmt;
					}
				}

//				System.out.println("Converted parameter opt list ....");
//				System.out.println("INPUT:  " + alias.toString());
//				System.out.println("OUTPUT: " + boolStmt.toString());
				// replace this alias stmt with the new BooleanAliasStmt

				result = boolStmt;
			}

		} break;
		case UNIQUE: {
			UniqueStmt unique = (UniqueStmt) alias;
			List<List<SimpleParameter>> resolved = resolveOptList(unique.getParams());
			List<UniqueStmt> simpleUniq = new LinkedList<UniqueStmt>();
			expandParamsToSimpleUnique(resolved, new LinkedList<SimpleParameter>(), simpleUniq);

			// build boolean or-expressions from list
			if (simpleUniq.size() > 1) {
				BooleanAliasStmt boolStmt = null;
				UniqueStmt firstStmt = null;
				Collections.reverse(simpleUniq);

				for (UniqueStmt stmt : simpleUniq) {
					if (firstStmt == null) {
						firstStmt = stmt;
					} else if (boolStmt == null) {
						boolStmt = new BooleanAliasStmt(stmt, firstStmt, Operator.OR);
					} else {
						BooleanAliasStmt newBoolStmt = new BooleanAliasStmt(stmt, boolStmt, Operator.OR);
						boolStmt = newBoolStmt;
					}
				}

				result = boolStmt;
			}
		} break;
		case ALIAS_INFER: {
			// nothing to do here
		} break;
		default:
			throw new IllegalStateException();
		}

		return result;
	}

	/*
	 * Creates a list of parameter lists, that resolve the optional parameter feature.
	 * Example:
	 * Input parameter list: {a, b, [c, d], e}
	 * Output: {{a}, {b}, {c, d}, {e}}
	 */
	private static List<List<SimpleParameter>> resolveOptList(List<Parameter> params) {
		List<List<SimpleParameter>> resolved = new LinkedList<List<SimpleParameter>>();

		for (Parameter param : params) {
			List<SimpleParameter> pList = new LinkedList<SimpleParameter>();

			switch (param.getType()) {
			case PARAM: {
				pList.add((SimpleParameter) param);
			} break;
			case PARAM_OPT_LIST: {
				ParameterOptList pOptList = (ParameterOptList) param;
				pList.addAll(pOptList.getParams());
			} break;
			default:
				throw new IllegalStateException();
			}

			resolved.add(pList);
		}

		return resolved;
	}

	/*
	 * Creates a list of primitive alias statements (without opt param lists - output in parameter result)
	 * from a list of parameter lists. Each list entry corresponds to one option.
	 *
	 * Example input: (originating from a statement like: !alias(a, b, [c, d] e))
	 *     params: Parameter list: {{a}, {b}, {c. d}, {e}}
	 *     current: Simple parameter list - Current status of parameter configuration (for recursion): {}
	 *     result: {}
	 *     isNegated: true (-> create negated primitive alias stmts from parameter list)
	 *
	 * Output of example:
	 *     result: {!alias(a, b, c, e), !alias(a, b, d, e)}
	 */
	private static void expandParamsToPrimitiveAlias(List<List<SimpleParameter>> params, List<SimpleParameter> current,
			List<PrimitiveAliasStmt> result, boolean isNegated) {
		if (params.size() == 0) {
			PrimitiveAliasStmt stmt = new PrimitiveAliasStmt(current, isNegated);
			result.add(stmt);
		} else {
			List<SimpleParameter> optParams = params.get(0);
			List<List<SimpleParameter>> newParams = new LinkedList<List<SimpleParameter>>(params);
			newParams.remove(0);
			for (SimpleParameter p : optParams) {
				List<SimpleParameter> newCurrent = new LinkedList<SimpleParameter>(current);
				newCurrent.add(p);
				expandParamsToPrimitiveAlias(newParams, newCurrent, result, isNegated);
			}
		}
	}

	/*
	 * Does the same for unique statements
	 */
	private static void expandParamsToSimpleUnique(List<List<SimpleParameter>> params, List<SimpleParameter> current, List<UniqueStmt> result) {
		if (params.size() == 0) {
			UniqueStmt stmt = new UniqueStmt(current);
			result.add(stmt);
		} else {
			List<SimpleParameter> optParams = params.get(0);
			List<List<SimpleParameter>> newParams = new LinkedList<List<SimpleParameter>>(params);
			newParams.remove(0);
			for (SimpleParameter p : optParams) {
				List<SimpleParameter> newCurrent = new LinkedList<SimpleParameter>(current);
				newCurrent.add(p);
				expandParamsToSimpleUnique(newParams, newCurrent, result);
			}
		}
	}

//	/*
//	 * Build not-alias statements from unique statements
//	 * unique(a, b, c) == !{a, *} & !{b, *} & !{c, *}
//	 */
//	private static List<PrimitiveAliasStmt> convertSimpleUniqueToPrimitiveAlias(List<UniqueStmt> simpleUniq) {
//		List<PrimitiveAliasStmt> result = new LinkedList<PrimitiveAliasStmt>();
//
//		for (UniqueStmt unique : simpleUniq) {
//			for (Parameter p : unique.getParams()) {
//				assert p.getType() == Type.PARAM && p.getType() != Type.PARAM_OPT_LIST; // no opt list allowed. This is redundant by will.
//
//				SimpleParameter wildcard = new SimpleParameter(new Wildcard());
//				List<SimpleParameter> params = new LinkedList<SimpleParameter>();
//				params.add((SimpleParameter) p);
//				params.add(wildcard);
//				PrimitiveAliasStmt noAlias = new PrimitiveAliasStmt(params, true);
//				result.add(noAlias);
//			}
//
//		}
//
//		return result;
//	}


}
