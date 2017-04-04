/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.irlsod;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;

/**
 * This provides a computation of the probabilistic influencers of a given node. It relies on a function cdom which
 * outputs a common dominator of any two nodes n,m. The set of probabilistic influencers of n (with respect to cdom) is
 * defined as the least with the following property: Let m be a node which may happen in parallel to n and c =
 * cdom(n,m). Any node in the cfg chop between c and n is a probabilistic influencer of n.
 *
 * @author Martin Mohr&lt;martin.mohr@kit.edu&gt;
 *
 */
public class CDomProbInfComputer extends ProbInfComputer {

	private final CFG icfg;
	private final PreciseMHPAnalysis mhp;
	private final ICDomOracle cdomOracle;

	public CDomProbInfComputer(final SDG sdg, final ICDomOracle cdomOracle) {
		this.icfg = ICFGBuilder.extractICFG(sdg);
		CSDGPreprocessor.preprocessSDG(sdg);
		this.mhp = PreciseMHPAnalysis.analyze(sdg);
		this.cdomOracle = cdomOracle;
	}

	protected Collection<? extends SDGNode> computeProbabilisticInfluencers(final SDGNode n) {
		if (!icfg.containsVertex(n)) {
			return Collections.emptyList();
		}
		Set<SDGNode> cdomList = new HashSet<SDGNode>();
		final Set<SDGNode> ret = new HashSet<SDGNode>();
		final SimpleTCFGChopper tcfgChopper = new SimpleTCFGChopper(icfg);
		for (final int threadN : n.getThreadNumbers()) {
			ThreadRegion trN = mhp.getThreadRegion(n, threadN);
			for (ThreadRegion trM : mhp.getThreadRegions()) {
				if (mhp.isParallel(trN, trM)) {
					for (SDGNode m : trM.getNodes()) {
						SDGNode cdom = cdomOracle.cdom(n, threadN, m, trM.getThread()).getNode();
						if (cdomList.contains(cdom)) continue;
						cdomList.add(cdom);
						final Collection<? extends SDGNode> cfgChop = tcfgChopper
								.chop(cdom, n);
						ret.addAll(cfgChop);
					}
				}
			}
		}
		return ret;
	}
}
