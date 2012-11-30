/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;


/**
 * This is the original iterated two-phase slicer from Nanda. Use it for
 * debugging or as a skeleton for more sophisticated algorithms.
 *
 * @author giffhorn
 */
public abstract class NandaI2PSlicer implements Slicer {
	protected SDG g;
	private ThreadsInformation ti;
	private EdgeListener listener;

	public NandaI2PSlicer(SDG g) {
		this(g, new TrivialEdgeListener());
	}

	public NandaI2PSlicer(SDG g, EdgeListener listener) {
		if (g == null) {
			throw new IllegalArgumentException("NandaI2PSlicer: sdg must not be null!");
		}
		if (g.getThreadsInfo() == null) {
			throw new IllegalArgumentException("NandaI2PSlicer: sdg must provide valid threads information!");
		}
		if (listener == null) {
			throw new IllegalArgumentException("NandaI2PSlicer: edge listener must not be null!");
		}

		this.g = g;
		this.ti = g.getThreadsInfo();
		this.listener = listener;
	}

	public void setListener(EdgeListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("NandaI2PSlicer: edge listener must not be null!");
		} else {
			this.listener = listener;
		}
	}

	protected abstract Collection<SDGEdge> edgesToTraverse(SDGNode n);

	protected abstract SDGNode getAdjacentNode(SDGEdge e);

	protected abstract boolean isDescendingEdge(SDGEdge.Kind k);

	protected abstract boolean isAscendingEdge(SDGEdge.Kind k);

	public void setGraph(SDG g) {
		this.g = g;
		this.ti = g.getThreadsInfo();
	}

	public Collection<SDGNode> slice(SDGNode c) {
		return slice(Collections.singleton(c));
	}

	public Collection<SDGNode> slice(Collection<SDGNode> c) {
		HashSet<SDGNode> slice = new HashSet<SDGNode>();

		LinkedList<VirtualNode> worklist_1 = new LinkedList<VirtualNode>();
		LinkedList<VirtualNode> worklist_2 = new LinkedList<VirtualNode>();
		LinkedList<VirtualNode> worklist_0 = new LinkedList<VirtualNode>();
		HashSet<VirtualNode> outer = new HashSet<VirtualNode>();
		HashSet<VirtualNode> inner = new HashSet<VirtualNode>();
		listener.init();

		for (SDGNode n : c) {
			slice.add(n);

			for (int t : n.getThreadNumbers()) {
				VirtualNode v = new VirtualNode(n, t);
				worklist_0.add(v);
				outer.add(v);
			}
		}

		while (!worklist_0.isEmpty()) {
			// init the next iteration
			inner.clear();
			worklist_1.add(worklist_0.poll());

			// === phase 1 ===
			// only ascend to calling procedures
			while (!worklist_1.isEmpty()) {
				VirtualNode w = worklist_1.poll();
				SDGNode n = w.getNode();
				int t = w.getNumber();

				for (SDGEdge edge : edgesToTraverse(n)) {
					listener.edgeEncountered(edge);
					if (!edge.getKind().isSDGEdge())
						continue;

					SDGNode m = getAdjacentNode(edge);

					if (edge.getKind() == SDGEdge.Kind.INTERFERENCE || edge.getKind() == SDGEdge.Kind.FORK_IN
							|| edge.getKind() == SDGEdge.Kind.FORK_OUT || edge.getKind() == SDGEdge.Kind.FORK) {
						// handle inter-thread edges: create new elements for
						// worklist_0

						for (int u : m.getThreadNumbers()) {
							// leave the thread!
							if (u == t && !ti.isDynamic(u))
								continue;

							VirtualNode v = new VirtualNode(m, u);

							if (outer.add(v)) {
								worklist_0.add(v);
								slice.add(m);
							}
						}

					} else if (isDescendingEdge(edge.getKind())) {
						// descend into a called procedure
						VirtualNode v = new VirtualNode(m, t);

						if (!outer.contains(v) && inner.add(v)) {
							slice.add(m);
							worklist_2.add(v);
						}

					} else if (isAscendingEdge(edge.getKind())) {
						// ascend to a calling procedure
						if (!m.isInThread(t))
							continue; // stay in this thread!

						VirtualNode v = new VirtualNode(m, t);

						if (outer.add(v)) {
							slice.add(m);
							worklist_1.add(v);
						}

					} else {
						// intra-procedural edges
						VirtualNode v = new VirtualNode(m, t);

						if (outer.add(v)) {
							slice.add(m);
							worklist_1.add(v);
						}
					}
				}
			}

			// === phase 2 ===
			// visit all transitively called procedures
			while (!worklist_2.isEmpty()) {
				VirtualNode w = worklist_2.poll();
				SDGNode n = w.getNode();
				int t = w.getNumber();

				for (SDGEdge edge : edgesToTraverse(n)) {
					listener.edgeEncountered(edge);
					if (!edge.getKind().isSDGEdge() || isAscendingEdge(edge.getKind()))
						continue;

					SDGNode m = getAdjacentNode(edge);

					if (edge.getKind() == SDGEdge.Kind.INTERFERENCE || edge.getKind() == SDGEdge.Kind.FORK_IN
							|| edge.getKind() == SDGEdge.Kind.FORK_OUT || edge.getKind() == SDGEdge.Kind.FORK) {
						// handle inter-thread edges: create new elements for
						// worklist_0

						for (int u : m.getThreadNumbers()) {
							// leave the thread!
							if (u == t && !ti.isDynamic(u))
								continue;

							VirtualNode v = new VirtualNode(m, u);

							if (outer.add(v)) {
								worklist_0.add(v);
								slice.add(m);
							}
						}

					} else {
						// an intra-procedural or param-out edge
						VirtualNode v = new VirtualNode(m, t);

						if (!outer.contains(v) && inner.add(v)) {
							slice.add(m);
							worklist_2.add(v);
						}
					}
				}
			}
		}

		return slice;
	}
}
