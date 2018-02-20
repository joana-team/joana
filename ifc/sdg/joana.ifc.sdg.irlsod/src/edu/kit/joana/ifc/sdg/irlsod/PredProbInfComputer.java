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
import java.util.stream.Collectors;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;

/**
 * This provides a computation of the probabilistic influencers of a given node. It relies on a function cdom which
 * outputs a common dominator of any two nodes n,m. The set of probabilistic influencers of n (with respect to cdom) is
 * defined as the least with the following property: Let m be a node which may happen in parallel to n and c =
 * cdom(n,m). Any node in the cfg chop between c and n is a probabilistic influencer of n.
 *
 * @author Simon Bischof &lt;simon.bischof@kit.edu&gt;
 *
 */
public class PredProbInfComputer extends ProbInfComputer {

	private final CFG icfg;
	private final MHPAnalysis mhp;
	private final boolean[] influencedRegion;

	/**
	 * Constructs a PredProbInfComputer instance with the given SDG
	 * and a precise MHP analysis. The SDG needs to be preprocessed.
	 * @param sdg preprocessed SDG
	 */
	public PredProbInfComputer(final SDG sdg) {
		this(sdg, PreciseMHPAnalysis.analyze(sdg));
	}

	/**
	 * Constructs a PredProbInfComputer instance with the given SDG
	 * and given MHP analysis.
	 * @param sdg SDG
	 * @param mhp MHP analysis
	 */
	public PredProbInfComputer(final SDG sdg, MHPAnalysis mhp) {
		this.icfg = ICFGBuilder.extractICFG(sdg);
		if (sdg.getThreadsInfo() == null) {
			CSDGPreprocessor.preprocessSDG(sdg);
		}
		this.mhp = mhp;
		this.influencedRegion = new boolean[mhp.getTR().size()];
		calculateInfluencedRegions();
	}

	private void calculateInfluencedRegions() {
		Collection<ThreadRegion> regions = mhp.getThreadRegions();
		for (final ThreadRegion trN : regions) {
			influencedRegion[trN.getID()] = false;
			for (ThreadRegion trM : regions) {
				if (mhp.isParallel(trN, trM) && !trM.getNodes().isEmpty()) {
					influencedRegion[trN.getID()] = true;
				}
			}
		}
	}

	protected Collection<? extends SDGNode> computeProbabilisticInfluencers(final SDGNode n) {
		if (!icfg.containsVertex(n) || !influenced(n)) {
			return Collections.emptyList();
		}
		return icfg.incomingEdgesOf(n).stream().map(SDGEdge::getSource)
				.collect(Collectors.toSet());
	}

	private boolean influenced(SDGNode n) {
		for (final int threadN : n.getThreadNumbers()) {
			ThreadRegion trN = mhp.getThreadRegion(n, threadN);
			if (influencedRegion[trN.getID()]) {
				return true;
			}
		}
		return false;
	}
}