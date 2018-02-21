/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.declass;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;


public final class MinimalCut {

	private MinimalCut() {}

	private static SDG g;

	public static Collection<SDGEdge> findMinimalCut(SDG sdg, Collection<SDGNode> subGraph, SDGNodeTuple range) {
		//Integer[3] = UebrigerFluss, VerwendeterFluss, Durchsucht}
		HashMap<SDGEdge, int[]> fluss = new HashMap<SDGEdge, int[]>();
		g = sdg.clone();

		assert subGraph.contains(range.getFirstNode());
		assert subGraph.contains(range.getSecondNode());
		assert range.getFirstNode() != range.getSecondNode();

		LinkedList<SDGNode> ref = new LinkedList<SDGNode>();
		//Kanten finden
		for (SDGNode n : subGraph) {
			if (n == range.getFirstNode() || n == range.getSecondNode()) {
				for (SDGEdge e : g.outgoingEdgesOf(n)) {
					if (subGraph.contains(e.getTarget())) {
						int[] i = {1, 0, 0};

//						if (fluss.keySet().contains(e)) {
//							fluss.get(e)[0] += i[0];
//
//						} else {
							fluss.put(e, i);
//						}
					}
				}

			} else {
				SDGNode nn =  n.clone();
				g.addVertex(nn);
				ref.add(nn);

				for (SDGEdge e : g.outgoingEdgesOf(n)) {
					e.setSource(nn);

					if (subGraph.contains(e.getTarget())) {
						int[] i = {1, 0, 0};

//						if (fluss.keySet().contains(e)) {
//							fluss.get(e)[0] += i[0];
//
//						} else {
							fluss.put(e, i);
//						}
					}
				}

				int[] i = {1, 0, 0};
				SDGEdge ne =  Kind.HELP.newEdge(n, nn);
				fluss.put(ne, i);
			}
		}
		subGraph.addAll(ref);

		//Pfade finden und maximalen Fluss berechnen
		LinkedList<SDGEdge> path = findPath(fluss, range);
		while (path != null){
			int min = 1;
			for (SDGEdge e : path) {
				min = Math.min(min, fluss.get(e)[0]);
			}
			assert min > 0;
			for (SDGEdge e : path) {
				fluss.get(e)[0] = fluss.get(e)[0] - min;
				fluss.get(e)[1] = fluss.get(e)[1] + min;
				SDGEdge ne =  e.getKind().newEdge(e.getTarget(), e.getSource());
				g.addEdge(ne);
				int[] i = {min, 0, 0};
				fluss.put(ne, i);
			}
			for (SDGEdge e : fluss.keySet()) { //Durchsucht reset
				fluss.get(e)[2] = 0;
			}
			path = findPath(fluss, range);
		}

		//Minimal Cut finden
		Set<SDGNode> cutNodes = new HashSet<SDGNode>();
		LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
		cutNodes.add(range.getFirstNode());
		worklist.add(range.getFirstNode());
		while (!worklist.isEmpty()) {
			for (SDGEdge e : g.outgoingEdgesOf(worklist.remove())) {
				if (fluss.containsKey(e) && fluss.get(e)[0] > 0) {
					if (cutNodes.add(e.getTarget())) {
						worklist.add(e.getTarget());
					}
				}
			}
		}

		LinkedList<SDGEdge> ret = new LinkedList<SDGEdge>();

		for (SDGNode n : cutNodes) {
			for (SDGEdge e : g.outgoingEdgesOf(n)) {
				if (subGraph.contains(e.getTarget()) && !cutNodes.contains(e.getTarget())) {
					ret.add(e);
				}
			}
		}

		return ret;
	}

	private static LinkedList<SDGEdge> findPath(HashMap<SDGEdge, int[]> fluss, SDGNodeTuple range) {
		LinkedList<SDGEdge> path = new LinkedList<SDGEdge>();

		SDGEdge e = pickEdge(range.getFirstNode(), fluss, path);
		if (e == null) {
			return null;
		}
		path.add(e);

		while (path.getLast().getTarget() != range.getSecondNode()) {
			e = pickEdge(path.getLast().getTarget(), fluss, path);
			if (e == null) {
				fluss.get(path.removeLast())[2] = 1;
				if (path.isEmpty()) {
					e = pickEdge(range.getFirstNode(), fluss, path);
					if (e == null) {
						return null;
					}
					path.add(e);
				}
			} else {
				path.add(e);
			}
		}

		return path;
	}

	private static SDGEdge pickEdge(SDGNode source, HashMap<SDGEdge, int[]> fluss, LinkedList<SDGEdge> path) {

		for (SDGEdge e : g.outgoingEdgesOf(source)) {
			if (fluss.containsKey(e) && fluss.get(e)[0] > 0 && fluss.get(e)[2] == 0 && !path.contains(e)) {
				return e;
			}
		}
		return null;
	}
}
