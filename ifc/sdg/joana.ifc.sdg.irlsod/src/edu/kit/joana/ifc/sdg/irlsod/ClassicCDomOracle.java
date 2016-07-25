package edu.kit.joana.ifc.sdg.irlsod;

import edu.kit.joana.graph.dominators.slca.DFSIntervalOrder;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.util.sdg.GraphModifier;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.graphs.Dominators;
import edu.kit.joana.wala.core.graphs.Dominators.DomEdge;

public class ClassicCDomOracle implements ICDomOracle {

	private static final Logger debug = Log.getLogger(Log.L_IFC_DEBUG);
	
	private final Dominators<SDGNode, SDGEdge> dom;
	private final DFSIntervalOrder<SDGNode, DomEdge> dio;
	private final MHPAnalysis mhp;

	public ClassicCDomOracle(final SDG sdg, final MHPAnalysis mhp) {
		final CFG icfg = ICFGBuilder.extractICFG(sdg);
		GraphModifier.removeCallCallRetEdges(icfg);
		this.dom = Dominators.compute(icfg, icfg.getRoot());
		this.dio = new DFSIntervalOrder<SDGNode, DomEdge>(dom.getDominationTree());
		this.mhp = mhp;
	}

	@Override
	public VirtualNode cdom(final SDGNode n1, final int threadN1, final SDGNode n2, final int threadN2) {
		if (this.dio.isLeq(n1, n2)) {
			return new VirtualNode(n1, threadN1);
		} else if (this.dio.isLeq(n2, n1)) {
			return new VirtualNode(n2, threadN2);
		} else {
			SDGNode cur = n1;
			while (!this.dio.isLeq(n2, cur) || mhp.isParallel(n1, cur) || mhp.isParallel(n2, cur)) {
				cur = dom.getIDom(cur);
			}
			debug.outln(String.format("icdom(%s,%s) = %s", n1, n2, cur));
			return new VirtualNode(cur, cur.getThreadNumbers()[0]);
		}
	}

}
