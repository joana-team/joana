package edu.kit.joana.ifc.sdg.irlsod;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;

/**
 * This provides a computation of the probabilistic influencers of a given node.
 * It relies on a function cdom which outputs a common dominator of any two
 * nodes n,m. The set of probabilistic influencers of n (with respect to cdom)
 * is defined as the least with the following property:
 * Let m be a node which may happen in parallel to n and c = cdom(n,m). Any node
 * in the cfg chop between c and n is a probabilistic influencer of n.
 * @author Martin Mohr&lt;martin.mohr@kit.edu&gt;
 *
 */
public class ProbInfComputer {

	private CFG icfg;
	private PreciseMHPAnalysis mhp;
	private ICDomOracle cdomOracle;
	private Map<SDGNode, Collection<? extends SDGNode>> cache = new HashMap<SDGNode, Collection<? extends SDGNode>>();

	public ProbInfComputer(SDG sdg, ICDomOracle cdomOracle) {
		this.icfg = ICFGBuilder.extractICFG(sdg);
		CSDGPreprocessor.preprocessSDG(sdg);
		this.mhp = PreciseMHPAnalysis.analyze(sdg);
		this.cdomOracle = cdomOracle;
	}

	public Collection<? extends SDGNode> getProbabilisticInfluencers(SDGNode n) {
		if (!cache.containsKey(n)) {
			cache.put(n, computeProbabilisticInfluencers(n));
		}
		return cache.get(n);
	}

	private Collection<? extends SDGNode> computeProbabilisticInfluencers(SDGNode n) {
		if (!icfg.containsVertex(n)) {
			return Collections.emptyList();
		} else {
			Set<SDGNode> ret = new HashSet<SDGNode>();
			SimpleTCFGChopper tcfgChopper = new SimpleTCFGChopper(icfg);
			for (int threadN : n.getThreadNumbers()) {
				for (SDGNode m : icfg.vertexSet()) {
					for (int threadM : m.getThreadNumbers()) {
						if (mhp.isParallel(n, threadN, m, threadM)) {
							System.out.println(String.format("MHP(%s,%s)", n, m));
							Collection<? extends SDGNode> cfgChop = tcfgChopper.chop(cdomOracle.cdom(n, threadN ,m, threadM).getNode(), n);
							System.out.println(String.format("CFGChop(%s,%s) = %s", cdomOracle.cdom(n, threadN, m, threadM), n, cfgChop));
							ret.addAll(cfgChop);
						}
					}
				}
			}
			return ret;
		}
	}
}
