/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.sdg;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

/**
 * @author Martin Mohr
 */
public class GraphModifier {
	
	private static final Logger debug = Log.getLogger(Log.L_IFC_DEBUG);
	
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
	
	public static <V, E> void removeUnreachable(DirectedGraph<V, E> graph, V root) {
		Set<V> unreachable = new HashSet<V>(graph.vertexSet());
		LinkedList<V> wl = new LinkedList<V>();
		wl.add(root);
		while (!wl.isEmpty()) {
			V next = wl.removeFirst();
			if (!unreachable.contains(next)) continue;
			unreachable.remove(next);
			for (E eOut : graph.outgoingEdgesOf(next)) {
				
				wl.add(graph.getEdgeTarget(eOut));
			}
		}
		graph.removeAllVertices(unreachable);
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

	public static void removeCallExcEdges(final JoanaGraph cfg) {
		final List<SDGEdge> toRemove = new LinkedList<SDGEdge>();
		for (final SDGEdge e : cfg.edgeSet()) {
			if ((e.getKind() == Kind.CONTROL_FLOW) && (e.getSource().getKind() == SDGNode.Kind.CALL)
					&& (e.getTarget().getKind() == SDGNode.Kind.ACTUAL_OUT)
					&& e.getTarget().getBytecodeName().equals(BytecodeLocation.EXCEPTION_PARAM)) {
				if (cfg.outDegreeOf(e.getSource()) > 1) {
					toRemove.add(e);
				}
			}
		}
		debug.outln("remove " + toRemove);
		cfg.removeAllEdges(toRemove);
		GraphModifier.removeUnreachable(cfg);
	}
}
