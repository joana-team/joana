package edu.kit.joana.ifc.orlsod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.util.maps.MapUtils;

public class OptORLSODChecker<L> extends ORLSODChecker<L> {

	/** a backward slicer needed for the dependency computation */
	private I2PBackward backw;

	/** maps each node to all the nodes which must be updated when the security level of the node has changed */
	private Map<SDGNode, Set<SDGNode>> forwDep;

	/** maps each node to the nodes from which the security level of the node can be computed */
	private Map<SDGNode, Set<SDGNode>> backwDep;

	public OptORLSODChecker(SDG sdg, IStaticLattice<L> secLattice, Map<SDGNode, L> srcAnn, Map<SDGNode, L> snkAnn, ProbInfComputer probInf) {
		super(sdg, secLattice, srcAnn, snkAnn, probInf);
		this.backw = new I2PBackward(sdg);
	}

	@Override
	public int check() {
		cl = initCL(false);
		backwDep = computeBackwardDep();
		forwDep = MapUtils.invert(backwDep);
		LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
		for (SDGNode src : srcAnn.keySet()) {
			worklist.add(src);
		}
		while (!worklist.isEmpty()) {
			SDGNode next = worklist.poll();
			// update security level of next
			L oldLevel = cl.get(next);
			L newLevel = srcAnn.containsKey(next)?srcAnn.get(next):oldLevel;
			for (SDGNode m : backwDep.get(next)) {
				newLevel = secLattice.leastUpperBound(newLevel, cl.get(m));
			}
			if (!newLevel.equals(oldLevel)) {
				cl.put(next, newLevel);
				if (forwDep.get(next) == null) continue;
				for (SDGNode depNext : forwDep.get(next)) {
					if (!worklist.contains(depNext)) {
						worklist.add(depNext);
					}
				}
			}
		}
		return checkCompliance();
	}

	protected Map<SDGNode, Set<SDGNode>> computeBackwardDep() {

		Map<SDGNode, Set<SDGNode>> ret = new HashMap<SDGNode, Set<SDGNode>>();
		for (SDGNode n : sdg.vertexSet()) {
			ret.put(n, computeBackwardDeps(n));
		}
		return ret;
	}

	protected Set<SDGNode> computeBackwardDeps(SDGNode n) {
		Set<SDGNode> dep = new HashSet<SDGNode>();
		dep.addAll(backw.slice(n));
		dep.addAll(probInf.getProbabilisticInfluencers(n));
		dep.remove(n);
		return dep;
	}

}
