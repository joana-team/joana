package edu.kit.joana.ifc.sdg.irlsod;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.kit.joana.ifc.sdg.core.IFC;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGForward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.maps.MapUtils;

public class TimimgClassificationCheckerDomPaths<L> extends IFC<L> {

	/** user-provided annotations */
	protected final Map<SDGNode, L> userAnn;

	protected final CFG icfg;
	protected final Map<SDGNode, Collection<SDGNode>> transClosure;

	protected final Map<SDGNode, Set<SDGNode>> timingDependence;
	protected final Map<Pair<SDGNode, SDGNode>, Collection<? extends SDGNode>> chops;

	protected final SimpleTCFGChopper tcfgChopper;
	protected final CFGForward tcfgForwardSlicer;

	@SuppressWarnings("unused")
	private final ICDomOracle cdomOracle;

	protected final PreciseMHPAnalysis mhp;

	/**
	 * "classical" classification of a node, i.e.: cl(n) == l if * the values of variables used ad n, * or whether (
	 * "how often") n is executed is influences by level l
	 */
	protected Map<SDGNode, L> cl;

	protected Map<Pair<VirtualNode, VirtualNode>, L> cle;

	/**
	 * check == true iff check() has been called already.
	 */
	protected boolean checked = false;

	/**
	 * Mode of determining predecessor during propagation along the sdg.
	 */
	protected final PredecessorMethod predecessorMethod;

	public TimimgClassificationCheckerDomPaths(final SDG sdg, final IStaticLattice<L> secLattice,
			final Map<SDGNode, L> userAnn, final PreciseMHPAnalysis mhp, final ICDomOracle cdomOracle,
			final PredecessorMethod predecessorMethod) {
		super(sdg, secLattice);
		this.userAnn = userAnn;
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

	protected Map<Pair<VirtualNode, VirtualNode>, L> initCLE() {
		final Map<Pair<VirtualNode, VirtualNode>, L> ret = new HashMap<>();
		for (final SDGNode n : icfg.vertexSet()) {
			for (final SDGNode m : icfg.vertexSet()) {
				for (final int threadN : n.getThreadNumbers()) {
					for (final int threadM : m.getThreadNumbers()) {
						final VirtualNode vn = new VirtualNode(n, threadN);
						final VirtualNode vm = new VirtualNode(m, threadM);
						if (mhp.isParallel(vn, vm)) {
							ret.put(Pair.pair(vn, vm), l.getBottom());
						}
					}
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

	public Map<Pair<VirtualNode, VirtualNode>, L> getCLE() {
		if (!checked) {
			throw new IllegalStateException();
		}
		return cle;
	}

	@Override
	public Collection<? extends IViolation<SecurityNode>> checkIFlow() throws NotInLatticeException {
		// TODO Auto-generated method stub
		return null;
	}
}
