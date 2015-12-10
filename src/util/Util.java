package util;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;

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
		Set<SDGNode> unreachable = new HashSet<SDGNode>(cfg.vertexSet());
		LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
		wl.add(cfg.getRoot());
		while (!wl.isEmpty()) {
			SDGNode next = wl.removeFirst();
			if (!unreachable.contains(next)) continue;
			unreachable.remove(next);
			for (SDGEdge eOut : cfg.outgoingEdgesOf(next)) {
				wl.add(eOut.getTarget());
			}
		}
		cfg.removeAllVertices(unreachable);
	}

}
