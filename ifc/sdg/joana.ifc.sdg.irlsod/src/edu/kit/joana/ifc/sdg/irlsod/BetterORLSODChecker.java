package edu.kit.joana.ifc.sdg.irlsod;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;

public class BetterORLSODChecker<L> extends ORLSODChecker<L> {

	public BetterORLSODChecker(SDG sdg, IStaticLattice<L> secLattice, ProbInfComputer probInf) {
		super(sdg, secLattice, probInf, null);
	}

	@Override
	public Collection<? extends IViolation<SecurityNode>> checkIFlow() throws NotInLatticeException {
		inferUserAnnotationsOnDemand();
		cl = initCLPartial(true);
		Set<SDGNode> worklistSlice = new HashSet<SDGNode>();
		Set<SDGNode> worklistProb = new HashSet<SDGNode>();
		for (SDGNode n : cl.keySet()) {
			worklistSlice.add(n);
		}
		I2PBackward backw = new I2PBackward(sdg);
		/**
		 * Some notes on this implementation:
		 * - We use slices instead of one PDG edge at a time. Why? The hope is that we can explore the PDG faster this way.
		 * - We have two worklists, one for propagation along the PDG and one for propagation to probablisitic influencers.
		 * Each time we process a node on one worklist, we add new nodes to the other worklist. This should yield a considerable
		 * speed-up: If we compute a backward slice of a node, we put all nodes from the slice to the prob-worklist. This way, we
		 * do not immediately backward slice from them again - this would be highly redundant, since backward slices are transitive,
		 * i.e. if m ->* n and n' \in BS(m), then n' \in BS(n). Hence, if we already have computed the backward slice of n, we do not
		 * need to compute the backward slice of m again since it is contained in the backward slice of n. One could get the idea that
		 * this approach with the two worklists does not cover transitive probablisitic dependences. Actually, it does: Since backward
		 * slices are reflexive (n \in BS(n)) we always put a node which we put on the slice-worklist on the prob worklist afterwards.
		 * So if we put a node on the slice-worklist due to probabilistic propagation, we immediately put it back on the prob-worklist
		 * afterwards for further probabilistic propagation. 
		 * - Since we propagate backwards, we have to dualize the compliance check: Our rules are
		 *   a.) if m ->_PDG n then  cl(m) <= cl(n)
		 *   b.) if m is a probabilistic influencer of n then cl(m) <= cl(n)
		 *   Hence, we use the meet operator to update the security levels. In the end, our compliance check looks for nodes n which violate
		 *   cl(n) >= userAnn(n)
		 *   Basically we look for high nodes for which cl is too low.
		 */
		while (!worklistSlice.isEmpty() || !worklistProb.isEmpty()) {
			while (!worklistSlice.isEmpty()) {
				SDGNode n = worklistSlice.iterator().next();
				worklistSlice.remove(n);
				// 1.) propagate backwards along PDG edges
				// clNew(m) = cl(m) meet cl(n) if m ->_PDG n
				for (SDGNode m : backw.slice(n)) {
					L newLevel = cl.containsKey(m) ? cl.get(m) : secLattice.getTop();
					newLevel = secLattice.greatestLowerBound(newLevel, cl.get(n));
					L oldLevel = cl.put(m, newLevel);
					if (oldLevel == null || !newLevel.equals(oldLevel) && !worklistSlice.contains(m)) {
						worklistProb.add(m);
					}
					if (userAnn.get(m) != null && LatticeUtil.isLeq(secLattice, newLevel, userAnn.get(m))
							&& !newLevel.equals(userAnn.get(m))) {
						System.out.println("Violation detected!");
					}
				}
			}
			while (!worklistProb.isEmpty()) {
				SDGNode n = worklistProb.iterator().next();
				worklistProb.remove(n);
				// 2.) propagate backwards to probabilistic influencers
				// clNew(m) = cl(m) meet cl(n) if m is a probabilistic influencer of n
				for (SDGNode n0 : probInf.getProbabilisticInfluencers(n)) {
					L newLevel = cl.containsKey(n0) ? cl.get(n0) : secLattice.getTop();
					newLevel = secLattice.greatestLowerBound(newLevel, cl.get(n));
					L oldLevel = cl.put(n0, newLevel);
					if (oldLevel == null || !newLevel.equals(oldLevel) && !worklistProb.contains(n0)) {
						worklistSlice.add(n0);
					}
					if (userAnn.get(n0) != null && LatticeUtil.isLeq(secLattice, newLevel, userAnn.get(n0))
							&& !newLevel.equals(userAnn.get(n0))) {
						System.out.println("Violation detected!");
					}
				}
			}
		}
		return checkComplianceDual();
	}
}
