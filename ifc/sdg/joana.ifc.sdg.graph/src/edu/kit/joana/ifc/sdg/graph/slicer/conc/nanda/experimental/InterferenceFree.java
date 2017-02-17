/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PForward;


public final class InterferenceFree {

	private InterferenceFree() {}

	public static Collection<SDGNode> computeBackward(SDG g) {
		HashSet<SDGNode> result = new HashSet<SDGNode>();
		LinkedList<SDGNode> interfering = new LinkedList<SDGNode>();

		for (SDGNode n : g.vertexSet()) {
			for (SDGEdge f : g.incomingEdgesOf(n)) {
				if (f.getKind().isSDGEdge() && f.getKind().isThreadEdge()) {
					interfering.add(n);
					break;
				}
			}
		}

		I2PForward slicer = new I2PForward(g);
		Collection<SDGNode> slice = slicer.slice(interfering);

		result.addAll(g.vertexSet());
		result.removeAll(slice);
		return result;
	}

	public static Collection<SDGNode> computeBackwardTest(SDG g) {
		HashSet<SDGNode> result = new HashSet<SDGNode>();
		LinkedList<SDGNode> interfering = new LinkedList<SDGNode>();

		for (SDGNode n : g.vertexSet()) {
			for (SDGEdge f : g.incomingEdgesOf(n)) {
				if (f.getKind().isSDGEdge()
						&& f.getKind().isThreadEdge()
						&& f.getKind() != SDGEdge.Kind.FORK
						&& f.getKind() != SDGEdge.Kind.FORK_IN) {
					interfering.add(n);
					break;
				}
			}
		}

		I2PForward slicer = new I2PForward(g);
		Collection<SDGNode> slice = slicer.slice(interfering);

		result.addAll(g.vertexSet());
		result.removeAll(slice);
		return result;
	}
}
