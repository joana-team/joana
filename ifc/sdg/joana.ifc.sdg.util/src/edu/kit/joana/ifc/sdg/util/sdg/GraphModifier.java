/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.sdg;

import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;

/**
 * @author Martin Mohr
 */
public class GraphModifier {
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
	}
	

}
