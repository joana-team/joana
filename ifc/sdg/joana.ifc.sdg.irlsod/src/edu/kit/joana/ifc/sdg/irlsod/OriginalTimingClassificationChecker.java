package edu.kit.joana.ifc.sdg.irlsod;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.conc.OrderConflict;
import edu.kit.joana.ifc.sdg.core.violations.ConflictEdge;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.UnaryViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGForward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.maps.MapUtils;

/**
 * This class contains the original version of the TimingClassificationChecker,
 * before multiple optimizations.
 * 
 * The only optimization implemented here is to only calculate timing (clt) information
 * for pairs that are ever queried, since calculating it for every pair (n,m)
 * of nodes that may happen in parallel leads to a massive performance overhead.
 */
public class OriginalTimingClassificationChecker<L> extends AnnotationMapChecker<L> {
	
	private static Logger debug = Log.getLogger(Log.L_IFC_DEBUG);
	
	protected final CFG icfg;
	protected final Map<SDGNode, Collection<SDGNode>> transClosure;

	protected final Map<SDGNode, Set<SDGNode>> timingDependence;
	protected final Map<Pair<SDGNode, SDGNode>, Collection<? extends SDGNode>> chops;

	protected final SimpleTCFGChopper tcfgChopper;
	protected final CFGForward tcfgForwardSlicer;

	private final ICDomOracle cdomOracle;

	protected final MHPAnalysis mhp;

	/**
	 * "classical" classification of a node, i.e.: cl(n) == l if * the values of variables used ad n, * or whether (
	 * "how often") n is executed is influences by level l
	 */
	protected Map<SDGNode, L> cl;

	/**
	 * "timing" classification of a pair of node, i.e.: cl(n,m) == l if the "relative timing" (which one may be executed
	 * before the other) is influences by level l
	 */
	protected Map<Pair<SDGNode, SDGNode>, L> clt;

	/**
	 * check == true iff check() has been called already.
	 */
	protected boolean checked = false;

	/**
	 * Mode of determining predecessor during propagation along the sdg.
	 */
	protected final PredecessorMethod predecessorMethod;

	public OriginalTimingClassificationChecker(final SDG sdg, final IStaticLattice<L> secLattice,
			final MHPAnalysis mhp, final ICDomOracle cdomOracle) {
		this(sdg, secLattice, null, mhp, cdomOracle, PredecessorMethod.SLICE);
	}
	public OriginalTimingClassificationChecker(final SDG sdg, final IStaticLattice<L> secLattice, final Map<SDGNode, L> userAnn,
			final MHPAnalysis mhp, final ICDomOracle cdomOracle, final PredecessorMethod predecessorMethod) {
		super(sdg, secLattice, userAnn);
		this.mhp = mhp;
		this.cdomOracle = cdomOracle;

		this.predecessorMethod = predecessorMethod;

		this.transClosure = new HashMap<>();

		// TODO: try not to annoy Jürgen by "doing Work" in a constructor!
		this.icfg = ICFGBuilder.extractICFG(sdg);

		this.tcfgChopper = new SimpleTCFGChopper(icfg);
		this.tcfgForwardSlicer = new CFGForward(icfg);
		this.chops = new HashMap<>();

		final Map<SDGNode, Set<SDGNode>> timingDependence = new HashMap<>();
		for (final SDGNode n : icfg.vertexSet()) {
			final List<SDGEdge> edges = icfg.outgoingEdgesOf(n).stream()
					.filter(e -> !e.getKind().equals(SDGEdge.Kind.FORK)).collect(Collectors.toList());

			final int nr = edges.size();

			// ENTRY Nodes may have three successors:
			// i) A formal-in node, which eventually leads to the procedured
			// "real" control flow
			// ii) The EXIT node, as required for control-deps
			// iii) A formal-out node, also leading to to EXIT Node
			// TODO: possibly fix this in the graph construction

			// The following still doesn't hold
			// TODO: characterize exceptions to this assertion.
			// assert (nr==0 || nr == 1 || nr == 2 ||
			// n.kind.equals(SDGNode.Kind.ENTRY));

			if (nr == 2) {
				final SDGNode n2 = edges.get(0).getTarget();
				final SDGNode n3 = edges.get(1).getTarget();

				transClosure.computeIfAbsent(n2, tcfgForwardSlicer::slice);
				transClosure.computeIfAbsent(n3, tcfgForwardSlicer::slice);

				final Set<SDGNode> dependentNodes = transClosure.get(n2).stream().filter(transClosure.get(n3)::contains)
						.collect(Collectors.toSet());
				timingDependence.put(n, dependentNodes);
			} else {
				timingDependence.put(n, Collections.emptySet());
			}
		}
		this.timingDependence = MapUtils.invert(timingDependence);

	}

	protected Map<SDGNode, L> initCL() {
		final Map<SDGNode, L> ret = new HashMap<SDGNode, L>();
		for (final SDGNode n : g.vertexSet()) {
			if (userAnn.containsKey(n)) {
				ret.put(n, userAnn.get(n));
			} else {
				ret.put(n, l.getBottom());
			}
		}
		return ret;
	}

	protected Map<Pair<SDGNode, SDGNode>, L> initCLT() {
		final Map<Pair<SDGNode, SDGNode>, L> ret = new HashMap<>();
		// relative timing of conflicts
		for (final SDGEdge e : g.edgeSet()) {
			if (e.getKind() == SDGEdge.Kind.INTERFERENCE
					|| e.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {
				SDGNode m = e.getSource();
				SDGNode n = e.getTarget();
				if (mhp.isParallel(m, n)) {
					ret.put(Pair.pair(n, m), l.getBottom());
					// TODO do we need this?
					ret.put(Pair.pair(m, n), l.getBottom());
				}
			}
		}
		// relative timing of annotated nodes
		for (final SDGNode n : userAnn.keySet()) {
			for (final SDGNode m : userAnn.keySet()) {
				if (mhp.isParallel(m, n)) {
					ret.put(Pair.pair(n, m), l.getBottom());
				}
			}
		}
		return ret;
	}

	public Map<SDGNode, L> getCL() {
		if (!checked) {
			throw new IllegalStateException();
		}
		return cl;
	}

	public Map<Pair<SDGNode, SDGNode>, L> getCLT() {
		if (!checked) {
			throw new IllegalStateException();
		}
		return clt;
	}

	/**
	 * Solely used to state some assumptions on the sdg
	 *
	 * @param n
	 * @return true iff any INTERFERENCE_WRITE edge from n to some m is matched by an INTERFERENCE_WRITE edge from m to
	 *         n.
	 */
	private boolean interferenceWriteUndirected(final SDGNode n) {
		for (final SDGEdge e : g.getOutgoingEdgesOfKind(n, SDGEdge.Kind.INTERFERENCE_WRITE)) {
			boolean found = false;
			for (final SDGEdge e2 : g.getOutgoingEdgesOfKind(e.getTarget(), SDGEdge.Kind.INTERFERENCE_WRITE)) {
				if (e2.getTarget().equals(n)) {
					found = true;
				}
			}
			return found;
		}
		return true;
	}

	@Override
	public Collection<? extends IViolation<SecurityNode>> checkIFlow() throws NotInLatticeException {
		inferUserAnnotationsOnDemand();
		final I2PBackward backw = new I2PBackward(g);
		// 1.) initialize classification: we go from the bottom up, so every
		// node is classified as low initially
		// except for the sources: They are classified with the user-annotated
		// source level
		cl = initCL();
		clt = initCLT();
		// 2.) fixed-point iteration
		int numIters = 0;
		boolean change;
		do {
			change = false;
			for (final SDGNode n : g.vertexSet()) {
				final L oldLevel = cl.get(n);
				// nothing changes if current level is top already
				if (l.getTop().equals(oldLevel)) {
					continue;
				}
				L newLevel = oldLevel;

				// 2a.) propagate from sdg predecessors
				final Collection<SDGNode> predecessors;
				switch (predecessorMethod) {
				case EDGE:
					// @formatter:off
					predecessors = g.incomingEdgesOf(n).stream()
					                                   .filter((e) -> e.getKind().isSDGEdge())
					                                   .map(SDGEdge::getSource)
					                                   .collect(Collectors.toSet());
					// @formatter:on
					debug.outln(String.format("BS(%s) = %s", n, predecessors));
					break;
				case SLICE:
					predecessors = backw.slice(n);
					debug.outln(String.format("PRED(%s) = %s", n, predecessors));
					break;
				default:
					throw new IllegalArgumentException(predecessorMethod.toString());
				}
				for (final SDGNode m : predecessors) {
					newLevel = l.leastUpperBound(newLevel, cl.get(m));
					if (l.getTop().equals(newLevel)) {
						break; // we can abort the loop here - level cannot get
						// any higher
					}
				}

				// 2b.) propagate security levels from the relative timing of
				// i) n, and
				// ii) nodes m from other threads whose execution write some
				// value used at n
				// (i.e.: there is an interference dependence from m to n, or
				// a interference-write dependence between n and m)
				// Reminder: INTERFERENCE-Dependencies can hold between non-mhp
				// nodes (e.g.: when a write in the main
				// thread is seen from a Thread started afterwards. Then heir
				// then their relative timing is fixed!
				// TODO: find out if the situation is the same for
				// INTERFERENCE_WRITE.
				assert(interferenceWriteUndirected(n));

				for (final SDGEdge e : g.getIncomingEdgesOfKind(n, SDGEdge.Kind.INTERFERENCE)) {
					final SDGNode m = e.getSource();
					if (!mhp.isParallel(n, m)) {
						continue;
					}
					L timingLevel = clt.get(Pair.pair(n, m));
					if (timingLevel == null) {
						throw new IllegalStateException(
								String.format("No timing classification for nodes %s and %s!",
								m, n));
					}
					newLevel = l.leastUpperBound(newLevel, timingLevel);
					if (l.getTop().equals(newLevel)) {
						break; // we can abort the loop here - level cannot get
						// any higher
					}
				}
				for (final SDGEdge e : g.getIncomingEdgesOfKind(n, SDGEdge.Kind.INTERFERENCE_WRITE)) {
					final SDGNode m = e.getSource();
					L timingLevel = clt.get(Pair.pair(n, m));
					if (timingLevel == null) {
						throw new IllegalStateException(
								String.format("No timing classification for nodes %s and %s!",
								m, n));
					}
					newLevel = l.leastUpperBound(newLevel, timingLevel);
					if (l.getTop().equals(newLevel)) {
						break; // we can abort the loop here - level cannot get
						// any higher
					}
				}
				if (!newLevel.equals(oldLevel)) {
					cl.put(n, newLevel);
					change = true;
				}
			}

			for (final Pair<SDGNode, SDGNode> nm : clt.keySet()) {
				final L oldLevel = clt.get(nm);
				// nothing changes if current level is top already
				if (l.getTop().equals(oldLevel)) {
					continue;
				}
				L newLevel = oldLevel;

				final SDGNode n = nm.getFirst();
				final SDGNode m = nm.getSecond();

				transClosure.computeIfAbsent(n, tcfgForwardSlicer::slice);
				transClosure.computeIfAbsent(m, tcfgForwardSlicer::slice);

				for (final int threadN : n.getThreadNumbers()) {
					for (final int threadM : m.getThreadNumbers()) {
						// @formatter:off
						final SDGNode c = cdomOracle.cdom(n, threadN, m, threadM).getNode();
						chops.computeIfAbsent(
							Pair.pair(c, n),
							pair -> tcfgChopper.chop(pair.getFirst(), pair.getSecond())
						);
						chops.computeIfAbsent(
							Pair.pair(c, m),
							pair -> tcfgChopper.chop(pair.getFirst(), pair.getSecond())
						);
						final List<? extends SDGNode> relevant =
							Stream.concat(chops.get(Pair.pair(c, n)).stream().filter(timingDependence.get(n)::contains),
							              chops.get(Pair.pair(c, m)).stream().filter(timingDependence.get(m)::contains))
							      .collect(Collectors.toList());
						// @formatter:on
						for (final SDGNode c2 : relevant) {
							newLevel = l.leastUpperBound(newLevel, cl.get(c2));
							if (l.getTop().equals(newLevel)) {
								break; // we can abort the loop here - level
								// cannot get any higher
							}
						}
					}
				}
				if (!newLevel.equals(oldLevel)) {
					clt.put(nm, newLevel);
					change = true;
				}

			}

			numIters++;
		} while (change);
		debug.outln(String.format("needed %d iteration(s).", numIters));

		checked = true;

		// 3.) check that sink levels comply
		return checkCompliance();
	}

	protected final Collection<? extends IViolation<SecurityNode>> checkCompliance() {
		final LinkedList<IViolation<SecurityNode>> violations = new LinkedList<>();
		for (final Map.Entry<SDGNode, L> obsEntry : userAnn.entrySet()) {
			final SDGNode obs = obsEntry.getKey();
			final L obsLevel = obsEntry.getValue();
			if (!l.isLeq(cl.get(obs), obsLevel)) {
				violations.add(new UnaryViolation<SecurityNode, L>(new SecurityNode(obs), obsLevel, cl.get(obs)));
			}
		}
		for (final Pair<SDGNode, SDGNode> nm : clt.keySet()) {
			final SDGNode n = nm.getFirst();
			final SDGNode m = nm.getSecond();

			if (userAnn.containsKey(n) && userAnn.containsKey(m)) {
				final L attackerLevel = l.greatestLowerBound(userAnn.get(n), userAnn.get(m));
				if (!l.isLeq(clt.get(nm), attackerLevel)) {
					violations.add(new OrderConflict<SecurityNode>(
							new ConflictEdge<SecurityNode>(new SecurityNode(n), new SecurityNode(m)),
							attackerLevel.toString()));
				}
			}
		}
		return violations;
	}
}
