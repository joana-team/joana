package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

import java.util.ArrayList;
import java.util.List;

public class JoanaUtil {

	public static List<SDGEdge> cut(SDG sdg, List<SDGNode> nodes) {
		List<SDGEdge> cutEdges = new ArrayList<>();
		for (int i = 0; i < nodes.size(); i++) {
			sdg.getOutgoingEdgesOfKind(nodes.get(i), SDGEdge.Kind.DATA_DEP).forEach(e -> {
				if (!nodes.contains(e.getTarget())) {
					cutEdges.add(e);
				}
			});
		}
		return cutEdges;
	}
}