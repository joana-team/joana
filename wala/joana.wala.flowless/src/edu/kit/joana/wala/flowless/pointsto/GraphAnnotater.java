/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.pointsto;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.util.graph.Graph;

import edu.kit.joana.wala.flowless.pointsto.AliasGraph.MayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.NoMayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.MatchPTSWithFlowLess.MatchResult;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier.BasicIFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.Parameter;
import edu.kit.joana.wala.flowless.spec.ast.PrimitiveAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;

/**
 * Annotates a basic alias graph with the information of a basic ifc statement.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class GraphAnnotater {

	private static final boolean PRINT_DEBUG = false;

	/**
	 * Result object for the computation of the maximal and minimal aliasing configuration.
	 * lowerBound is minimal aliasing configuration. upperBound is maximal aliasing.
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class Aliasing {

		public Aliasing(MayAliasGraph lowerBound, MayAliasGraph upperBound) {
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}

		/**
		 * contains the graph with the minimum number of may aliases as defined in the ifc spec
		 */
		public final MayAliasGraph lowerBound;

		/**
		 * contains the graph with the maximum number of may aliases excluding the ones forbidden by the ifc spec.
		 */
		public final MayAliasGraph upperBound;

	}

	public static Aliasing computeBounds(MayAliasGraph maxAliases) {
		MayAliasGraph minAlias = new MayAliasGraph(maxAliases.isStaticMethod());
		for (PtsParameter param : maxAliases) {
			minAlias.addNode(param);
		}

		return new Aliasing(minAlias, maxAliases);
	}

	/**
	 * Computes the maximal and minimal aliasing configurations that are defined
	 * by the given ifc statement and a given maximal aliasing configuration.
	 * The maximal aliasing configuration can be taken from SideEffectApproximator.
	 * It is only limited by the types of the different fields. Only fields with a
	 * compatible type may be aliased.
	 * @param maxAliases An may-alias graph that is used as the maximal possible
	 * alias configuration.
	 * @param stmt An ifc statement. We compute an upper and a lower
	 * bound for it.
	 * @return A result consisting of an upper and an lower bound for the aliasing
	 * configuration as defined by the ifc statement.
	 * Always true: maxAliases >= result.upperBound >= result.lowerBound
	 * @throws FlowAstException Is thrown iff the ifc statement contains an error.
	 * Like referring to a non-existing field.
	 */
	public static Aliasing computeBounds(MayAliasGraph maxAliases, BasicIFCStmt stmt) throws FlowAstException {
		if (maxAliases == null || stmt == null) {
			throw new IllegalArgumentException("Arguments may not be null.");
		}

		List<BasicIFCStmt> list = new LinkedList<BasicIFCStmt>();
		list.add(stmt);

		return computeBounds(maxAliases, list);
	}

	/**
	 * Computes the maximal and minimal aliasing configurations that are defined
	 * by the given ifc statements and a given maximal aliasing configuration.
	 * The maximal aliasing configuration can be taken from SideEffectApproximator.
	 * It is only limited by the types of the different fields. Only fields with a
	 * compatible type may be aliased.
	 * @param maxAliases An may-alias graph that is used as the maximal possible
	 * alias configuration.
	 * @param stmts A collection of ifc statements. We compute an upper and a lower
	 * bound for them.
	 * @return A result consisting of an upper and an lower bound for the aliasing
	 * configuration as defined by the ifc statements.
	 * Always true: maxAliases >= result.upperBound >= result.lowerBound
	 * @throws FlowAstException Is thrown iff an ifc statement contains an error.
	 * Like referring to a non-existing field.
	 */
	public static Aliasing computeBounds(MayAliasGraph maxAliases, Collection<BasicIFCStmt> stmts) throws FlowAstException {
		if (maxAliases == null || stmts == null) {
			throw new IllegalArgumentException("Arguments may not be null.");
		}

		MatchResult match = MatchPTSWithFlowLess.findMatchingParams(maxAliases, stmts);

		MayAliasGraph mayAlias = new MayAliasGraph(maxAliases.isStaticMethod());
		NoMayAliasGraph notMayAlias = new NoMayAliasGraph(maxAliases.isStaticMethod());
		for (PtsParameter param : maxAliases) {
			mayAlias.addNode(param);
			notMayAlias.addNode(param);
		}

		for (BasicIFCStmt ifc : stmts) {
			if (PRINT_DEBUG) {
				System.out.println("Annotating graph with " + ifc);
			}

			annotateForcedAliases(mayAlias, match, maxAliases, ifc.aPlus);
			annotateForbiddenAliases(notMayAlias, match, maxAliases, ifc.aMinus);
		}

		// if mayAlias && notMayAlias share edges, the declaration is not valid
		if (AliasGraph.shareEdges(mayAlias, notMayAlias)) {
			Graph<PtsParameter> shared = AliasGraph.intersectGeneric(mayAlias, notMayAlias);
			for (PtsParameter p : shared) {
				if (shared.getSuccNodeCount(p) > 0) {
					for (Iterator<PtsParameter> it = shared.getSuccNodes(p); it.hasNext();) {
						PtsParameter other = it.next();
						System.err.println("Contradicting alias specification: " + p + " - " + other
								+ " should be aliased and not aliased at the same time...");
					}
				}
			}
		}

		// else new min alias graph := mayAlias & maxAlias, new max alias graph := maxAlias \ notMayAlias
		MayAliasGraph upperBound = maxAliases.clone();
		upperBound.removeEdges(notMayAlias);
		MayAliasGraph lowerBound = AliasGraph.intersect(maxAliases, mayAlias);

		return new Aliasing(lowerBound, upperBound);
	}

	private static void annotateForcedAliases(MayAliasGraph toChange, MatchResult match,
			MayAliasGraph maxAliases, Collection<PrimitiveAliasStmt> stmts) {
		for (PrimitiveAliasStmt alias : stmts) {
			Set<PtsParameter> ptsParams = findTransitiveReachable(match, alias.getParams());
			for (PtsParameter from : ptsParams) {
				for (PtsParameter to : ptsParams) {
					if (from != to) {
						if (maxAliases.hasEdge(from, to)) {
							// add edge iff the type system may allow an alias. Everything else has been filtered before and
							// is not contained in the maximal graph 'graph'.
							toChange.addEdge(from, to);
//						} else {
//							System.err.println("WARN: Could not add specified alias, because of incompatible types: "
//									+ " alias(" + from + ", " + to + ")");
						}
					}
				}
			}
		}
	}

	private static void annotateForbiddenAliases(NoMayAliasGraph toChange, MatchResult match, MayAliasGraph maxAliases, Collection<PrimitiveAliasStmt> stmts) {
		for (PrimitiveAliasStmt alias : stmts) {
			Set<PtsParameter> ptsParams = findMappedParams(match, alias.getParams());
			for (PtsParameter from : ptsParams) {
				for (PtsParameter to : ptsParams) {
					if (from != to && maxAliases.hasEdge(from, to)) {
						// add edge iff the type system may allow an alias. Everything else has been filtered before and
						// is not contained in the maximal graph 'graph'.
						toChange.addEdge(from, to);
					}
				}
			}
		}
	}

	private static Set<PtsParameter> findMappedParams(MatchResult match, List<Parameter> params) {
		Set<PtsParameter> ptsParams = new HashSet<PtsParameter>();

		for (Parameter p : params) {
			if (match.hasMatchFor(p)) {
				PtsParameter pts = match.getMatch(p);
				if (!ptsParams.contains(pts)) {
					ptsParams.add(pts);
				}

				if (p.endsWithWildcard()) {
					// only add transitive reachables iff a wildcard was used
					ptsParams.addAll(pts.getReachableChildren());
				} else {
					// only add primitive children
					for (PtsParameter child : pts.getChildren()) {
						if (child.getType().isPrimitiveType()) {
							ptsParams.add(child);
						}
					}
				}
			} else {
				System.err.println("No match for " + p);
			}
		}

		return ptsParams;
	}

	private static Set<PtsParameter> findTransitiveReachable(MatchResult match, List<Parameter> params) {
		Set<PtsParameter> ptsParams = new HashSet<PtsParameter>();

		for (Parameter p : params) {
			if (match.hasMatchFor(p)) {
				PtsParameter pts = match.getMatch(p);
				if (!ptsParams.contains(pts)) {
					ptsParams.add(pts);
					// aliases influence "per default" their child fields.
					// if two object may point to the same location, their fields definitely have to do so too....
					ptsParams.addAll(pts.getReachableChildren());
				}
			} else {
				System.err.println("No match for " + p);
			}
		}

		return ptsParams;
	}

}
