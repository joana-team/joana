/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.interference;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;


public class SynchAnalysis {
	private SDG sdg;
	private TIntObjectHashMap<LinkedList<SDGNode>> monitorEnter;
	private TIntObjectHashMap<LinkedList<SDGNode>> monitorExit;
	private HashMap<SDGNode, HashSet<SDGNode>> mEntryToEnclosedNodes;

	SynchAnalysis() { }

	private void init(SDG g) {
		sdg = g;
		monitorEnter = new TIntObjectHashMap<LinkedList<SDGNode>>();
		monitorExit = new TIntObjectHashMap<LinkedList<SDGNode>>();
		mEntryToEnclosedNodes = new HashMap<SDGNode, HashSet<SDGNode>>();
	}

	void analyze(SDG g) {
		init(g);
		screenForLocks();
		collectEmbeddedStatements();
		createSynchDependence();
	}

	private void screenForLocks() {
		for (SDGNode n : sdg.vertexSet()) {
			if (n.getLabel().contains("MONITOREXIT")) {
				LinkedList<SDGNode> exits = monitorExit.get(n.getProc());

				if (exits == null) {
					exits = new LinkedList<SDGNode>();
					monitorExit.put(n.getProc(), exits);
				}

				exits.add(n);

			} else if (n.getLabel().contains("MONITORENTER")) {
				LinkedList<SDGNode> entries = monitorEnter.get(n.getProc());

				if (entries == null) {
					entries = new LinkedList<SDGNode>();
					monitorEnter.put(n.getProc(), entries);
				}

				entries.add(n);
			}
		}
	}

	/**
	 * Imprecise in case of nested synchronization blocks.
	 * Nested synchronization blocks are rarely encountered in practice.
	 */
	private void collectEmbeddedStatements() {
		for (int proc : monitorExit.keys()) {
			/* backward CFG slice */
			LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
			HashSet<SDGNode> visited = new HashSet<SDGNode>();
			wl.addAll(monitorExit.get(proc));
			visited.addAll(wl);



			while (!wl.isEmpty()) {
				SDGNode next = wl.poll();

				for (SDGEdge e : sdg.incomingEdgesOf(next)) {
					if (!e.getKind().isControlFlowEdge() || !e.getKind().isIntraproceduralEdge()) continue;

					SDGNode source = e.getSource();

					if (visited.add(source)) {
						wl.add(source);
					}
				}
			}

			/* forward CFG slices */
			for (SDGNode n : monitorEnter.get(proc)) {
				HashSet<SDGNode> result = new HashSet<SDGNode>();

				if (visited.contains(n)) {
					wl.add(n);
					result.add(n);
				}

				while (!wl.isEmpty()) {
					SDGNode next = wl.poll();

					for (SDGEdge e : sdg.outgoingEdgesOf(next)) {
						if (!e.getKind().isControlFlowEdge() || !e.getKind().isIntraproceduralEdge()) continue;

						SDGNode target = e.getTarget();

						if (visited.contains(target)) {
							if (result.add(target)) {
								wl.add(target);
							}
						}
					}
				}


				/* map results */

				mEntryToEnclosedNodes.put(n, result);
			}
		}
	}

	private void createSynchDependence() {
		for (SDGNode entry : mEntryToEnclosedNodes.keySet()) {
			for (SDGNode n : mEntryToEnclosedNodes.get(entry)) {
				sdg.addEdge(new SDGEdge(entry, n, SDGEdge.Kind.SYNCHRONIZATION));
			}
		}
	}
}
