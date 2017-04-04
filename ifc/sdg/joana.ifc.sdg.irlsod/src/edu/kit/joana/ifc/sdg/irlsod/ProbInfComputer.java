/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.irlsod;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * This provides a computation of the probabilistic influencers of a given node.
 *
 * @author Simon Bischof &lt;simon.bischof@kit.edu&gt;
 *
 */
public abstract class ProbInfComputer {
	private final Map<SDGNode, Collection<? extends SDGNode>> cache = new HashMap<SDGNode, Collection<? extends SDGNode>>();

	public Collection<? extends SDGNode> getProbabilisticInfluencers(final SDGNode n) {
		if (!cache.containsKey(n)) {
			cache.put(n, computeProbabilisticInfluencers(n));
		}
		return cache.get(n);
	}

	protected abstract Collection<? extends SDGNode> computeProbabilisticInfluencers(final SDGNode n);
}
