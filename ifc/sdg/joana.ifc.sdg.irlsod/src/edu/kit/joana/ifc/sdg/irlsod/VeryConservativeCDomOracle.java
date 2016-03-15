package edu.kit.joana.ifc.sdg.irlsod;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;

/**
 * This cdom oracle chooses the start node of the given cfg as cdom of any two
 * nodes. When plugged into an ORLSOD algorithm, this yields the RLSOD
 * algorithm.
 *
 * @author Martin Mohr&lt;martin.mohr@kit.edu&gt;
 *
 */
public class VeryConservativeCDomOracle implements ICDomOracle {
	private final CFG icfg;

	public VeryConservativeCDomOracle(final CFG icfg) {
		this.icfg = icfg;
	}

	@Override
	public VirtualNode cdom(final SDGNode n1, final int threadN1, final SDGNode n2, final int threadN2) {
		return new VirtualNode(icfg.getRoot(), icfg.getRoot().getThreadNumbers()[0]);
	}
}
