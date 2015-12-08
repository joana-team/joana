package edu.kit.joana.util;

import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.sdg.ReducedCFGBuilder;

public class Util {

	public static void removeCallCallRetEdges(JoanaGraph cfg) {
		List<SDGEdge> toRemove = new LinkedList<SDGEdge>();
		for (SDGEdge e : cfg.edgeSet()) {
			if (e.getKind() == Kind.CONTROL_FLOW && e.getSource().getKind() == SDGNode.Kind.CALL && BytecodeLocation.isCallRetNode(e.getTarget())) {
				if (cfg.outDegreeOf(e.getSource()) > 1) {
					toRemove.add(e);
				}
			}
		}
		cfg.removeAllEdges(toRemove);
		removeUnreachable(cfg);
	}

	public static void removeUnreachable(JoanaGraph cfg) {
		// TODO Auto-generated method stub

	}

	public static void removeEntryExitExcConnections(CFG icfg) {
		// TODO Auto-generated method stub

	}

}
