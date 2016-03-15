package edu.kit.joana.ifc.sdg.irlsod;

import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.graph.dominators.slca.DFSIntervalOrder;
import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.sdg.GraphModifier;
import edu.kit.joana.wala.core.graphs.Dominators;
import edu.kit.joana.wala.core.graphs.Dominators.DomEdge;

public class ClassicCDomOracle implements ICDomOracle {

	private Dominators<SDGNode, SDGEdge> dom;
	private DFSIntervalOrder<SDGNode, DomEdge> dio;
	private MHPAnalysis mhp;

	public ClassicCDomOracle(SDG sdg, MHPAnalysis mhp) {
		CFG icfg = ICFGBuilder.extractICFG(sdg);
		GraphModifier.removeCallCallRetEdges(icfg);
		this.dom = Dominators.compute(icfg, icfg.getRoot());
		this.dio = new DFSIntervalOrder<SDGNode, DomEdge>(dom.getDominationTree());
		this.mhp = mhp;
	}

	public static void removeCallExcEdges(JoanaGraph cfg) {
		List<SDGEdge> toRemove = new LinkedList<SDGEdge>();
		for (SDGEdge e : cfg.edgeSet()) {
			if (e.getKind() == Kind.CONTROL_FLOW && e.getSource().getKind() == SDGNode.Kind.CALL && e.getTarget().getKind() == SDGNode.Kind.ACTUAL_OUT && e.getTarget().getBytecodeName().equals(BytecodeLocation.EXCEPTION_PARAM)) {
				if (cfg.outDegreeOf(e.getSource()) > 1) {
					toRemove.add(e);
				}
			}
		}
		System.out.println("remove " + toRemove);
		cfg.removeAllEdges(toRemove);
		GraphModifier.removeUnreachable(cfg);
	}

	@Override
	public VirtualNode cdom(SDGNode n1, int threadN1, SDGNode n2, int threadN2) {
		if (this.dio.isLeq(n1, n2)) {
			return new VirtualNode(n1, threadN1);
		} else if (this.dio.isLeq(n2, n1)) {
			return new VirtualNode(n2, threadN2);
		} else {
			SDGNode cur = n1;
			while (!this.dio.isLeq(n2, cur) || mhp.isParallel(n1, cur) || mhp.isParallel(n2, cur)) {
				cur = dom.getIDom(cur);
			}
			System.out.println(String.format("icdom(%s,%s) = %s", n1, n2, cur));
			return new VirtualNode(cur, cur.getThreadNumbers()[0]);
		}
	}

}
