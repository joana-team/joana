package edu.kit.joana.ifc.sdg.irlsod;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.util.maps.MapUtils;

public class OptORLSODChecker<L> extends ORLSODChecker<L> {

	/** a backward slicer needed for the dependency computation */
	private final I2PBackward backw;

	/**
	 * maps each node to all the nodes which must be updated when the security level of the node has changed
	 */
	private Map<SDGNode, Set<SDGNode>> forwDep;

	/**
	 * maps each node to the nodes from which the security level of the node can be computed
	 */
	private Map<SDGNode, Set<SDGNode>> backwDep;

	public OptORLSODChecker(final SDG sdg, final IStaticLattice<L> secLattice, final ProbInfComputer probInf) {
		this(sdg, secLattice, null, probInf);
	}

	public OptORLSODChecker(final SDG sdg, final IStaticLattice<L> secLattice, final Map<SDGNode, L> userAnn,
			final ProbInfComputer probInf) {
		super(sdg, secLattice, userAnn, probInf, null);
		this.backw = new I2PBackward(sdg);
	}

	@Override
	public Collection<? extends IViolation<SecurityNode>> checkIFlow() throws NotInLatticeException {
		inferUserAnnotationsOnDemand();
		
		final IStaticLattice<L> secLattice = this.getLattice();
		cl = initCL(false);
		backwDep = computeBackwardDep();
		forwDep = MapUtils.invert(backwDep);
		final LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
		for (final SDGNode n : userAnn.keySet()) {
			worklist.add(n);
			cl.put(n, userAnn.get(n));
		}
		while (!worklist.isEmpty()) {
			final SDGNode next = worklist.poll();
			L level = cl.get(next);
			if (forwDep.get(next) == null) {
				continue;
			}
			
			for (final SDGNode depNext : forwDep.get(next)) {
				final L oldLevel = cl.get(depNext);
				L newLevel = secLattice.leastUpperBound(oldLevel, level);
				if (!newLevel.equals(oldLevel) && !worklist.contains(depNext)) {
					worklist.add(depNext);
					cl.put(depNext, newLevel);
				}
			}
		}
		return checkCompliance();
	}

	protected Map<SDGNode, Set<SDGNode>> computeBackwardDep() {
		final SDG sdg = this.getSDG();
		
		final Map<SDGNode, Set<SDGNode>> ret = new HashMap<SDGNode, Set<SDGNode>>();
		for (final SDGNode n : sdg.vertexSet()) {
			ret.put(n, computeBackwardDeps(n));
		}
		return ret;
	}

	protected Set<SDGNode> computeBackwardDeps(final SDGNode n) {
		final Set<SDGNode> dep = new HashSet<SDGNode>();
		dep.addAll(backw.slice(n));
		dep.addAll(probInf.getProbabilisticInfluencers(n));
		dep.remove(n);
		return dep;
	}

}
