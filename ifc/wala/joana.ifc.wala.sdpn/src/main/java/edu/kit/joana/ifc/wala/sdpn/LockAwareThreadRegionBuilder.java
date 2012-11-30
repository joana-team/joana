/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.wala.sdpn;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegions;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class LockAwareThreadRegionBuilder {
	private CFG icfg;
	private ThreadsInformation info;
	private LinkedList<ThreadRegion> regions;
	private TIntObjectHashMap<TIntObjectHashMap<ThreadRegion>> map;
	private int id;

	public LockAwareThreadRegionBuilder(CFG icfg, ThreadsInformation info) {
		this.icfg = icfg;
		this.info = info;
		regions = new LinkedList<ThreadRegion>();
		map = new TIntObjectHashMap<TIntObjectHashMap<ThreadRegion>>();
		id = 0;
	}

	public ThreadRegions computeRegions() {
		for (int thread = 0; thread < info.getNumberOfThreads(); thread++) {
			computeRegions(thread);
		}

		return new LockAwareThreadRegions(regions, icfg, map);
	}

	/**
	 * Computes the thread regions of the given CFG. First, all the region
	 * starts are computed using a fix-point iteration, then the ThreadRegions
	 * are initialized. The fix-point iteration is needed to determine the merge
	 * nodes of different thread regions, which are region start nodes
	 * themselves.
	 *
	 */
	private void computeRegions(int thread) {
		HashSet<SDGNode> startNodes = computeStartNodes(thread);
		regions.addAll(computeRegions(startNodes, thread));
	}

	private HashSet<SDGNode> computeStartNodes(int thread) {
		// initial start nodes
		HashSet<SDGNode> init = new HashSet<SDGNode>();
		init.add(info.getThreadEntry(thread));
		Collection<SDGNode> forks = info.getAllForks();

		for (SDGNode fork : forks) {
			if (fork.isInThread(thread)) {
				init.add(fork);
			}
		}

		Collection<SDGNode> joins = info.getAllJoins();

		for (SDGNode join : joins) {
			if (join.isInThread(thread)) {
				init.add(join);
			}
		}

		for (SDGNode sync : icfg.vertexSet()) {
			if (sync.kind == SDGNode.Kind.SYNCHRONIZATION && sync.isInThread(thread)) {
				init.add(sync);
			}
		}

		// refine start nodes
		HashSet<SDGNode> result = new HashSet<SDGNode>();
		result.addAll(init);

		do {
			init.addAll(result);

			HashSet<SDGNode> previouslyMarked = new HashSet<SDGNode>();

			for (SDGNode node : init) {
				LinkedList<SDGNode> w1 = new LinkedList<SDGNode>();
				LinkedList<SDGNode> w2 = new LinkedList<SDGNode>();
				HashSet<SDGNode> marked = new HashSet<SDGNode>();

				w1.add(node);
				marked.add(node);

				while (!w1.isEmpty()) {
					SDGNode next = w1.poll();

					for (SDGEdge edge : icfg.outgoingEdgesOf(next)) {
						if (edge.getKind() == SDGEdge.Kind.FORK
								|| (edge.getKind() == SDGEdge.Kind.RETURN && !edge.getTarget().isInThread(thread))
								|| edge.getTarget().kind == SDGNode.Kind.SYNCHRONIZATION) {
							// don't leave the thread
							continue;

						} else {
							SDGNode reached = edge.getTarget();
							if (!reached.isInThread(thread))
								throw new RuntimeException("Error at edge " + edge);

							// don't cross thread region borders
							if (init.contains(reached))
								continue;

							// we've found another thread region start node
							if (previouslyMarked.contains(reached)) {
								result.add(reached);
								continue;
							}

							if (marked.add(reached)) {
								// 2-phase slicing
								if (edge.getKind() != SDGEdge.Kind.CALL) {
									w1.addFirst(reached);

								} else {
									w2.addFirst(reached);
								}
							}
						}
					}
				}

				while (!w2.isEmpty()) {
					SDGNode next = w2.poll();

					for (SDGEdge edge : icfg.outgoingEdgesOf(next)) {
						if (edge.getKind() == SDGEdge.Kind.FORK || edge.getKind() == SDGEdge.Kind.RETURN
								|| edge.getTarget().kind == SDGNode.Kind.SYNCHRONIZATION) {
							// don't leave the thread, don't leave procedures do not go past locks
							continue;

						} else {
							SDGNode reached = edge.getTarget();
							if (!reached.isInThread(thread))
								throw new RuntimeException("Error at edge " + edge);

							// don't cross thread region borders
							if (init.contains(reached))
								continue;

							// we've found another thread region start node
							if (previouslyMarked.contains(reached)) {
								result.add(reached);
								continue;
							}

							if (marked.add(reached)) {
								w2.addFirst(reached);
							}
						}
					}
				}

				previouslyMarked.addAll(marked);
			}

		} while (result.size() > init.size());

		return result;
	}

	private LinkedList<ThreadRegion> computeRegions(HashSet<SDGNode> startNodes, int thread) {
		LinkedList<ThreadRegion> result = new LinkedList<ThreadRegion>();

		for (SDGNode node : startNodes) {
			LinkedList<SDGNode> w1 = new LinkedList<SDGNode>();
			LinkedList<SDGNode> w2 = new LinkedList<SDGNode>();
			HashSet<SDGNode> marked = new HashSet<SDGNode>();

			w1.add(node);
			marked.add(node);

			while (!w1.isEmpty()) {
				SDGNode next = w1.poll();

				for (SDGEdge edge : icfg.outgoingEdgesOf(next)) {
					if (edge.getTarget().kind == SDGNode.Kind.SYNCHRONIZATION || edge.getKind() == SDGEdge.Kind.FORK
							|| (edge.getKind() == SDGEdge.Kind.RETURN && !edge.getTarget().isInThread(thread))) {
						// don't leave the thread
						continue;

					} else {
						SDGNode reached = edge.getTarget();
						if (!reached.isInThread(thread))
							throw new RuntimeException("Error at edge " + edge);

						// don't cross thread region borders
						if (startNodes.contains(reached))
							continue;

						if (marked.add(reached)) {
							// 2-phase slicing
							if (edge.getKind() != SDGEdge.Kind.CALL) {
								w1.addFirst(reached);

							} else {
								w2.addFirst(reached);
							}
						}
					}
				}
			}

			while (!w2.isEmpty()) {
				SDGNode next = w2.poll();

				for (SDGEdge edge : icfg.outgoingEdgesOf(next)) {
					if (edge.getTarget().kind != SDGNode.Kind.SYNCHRONIZATION && edge.getKind() != SDGEdge.Kind.FORK
							&& edge.getKind() != SDGEdge.Kind.RETURN) {
						// don't leave the thread, don't leave procedures
						SDGNode reached = edge.getTarget();
						if (!reached.isInThread(thread))
							throw new RuntimeException("Error at edge " + edge);

						// don't cross thread region borders
						if (startNodes.contains(reached))
							continue;

						if (marked.add(reached)) {
							w2.addFirst(reached);
						}
					}
				}
			}

			// marked contains the nodes of the thread region
			ThreadRegion tr = new ThreadRegion(id, node, thread, info.isDynamic(thread));
			tr.setNodes(marked);
			result.addLast(tr);

			TIntObjectHashMap<ThreadRegion> mappy = map.get(thread);
			if (mappy == null) {
				mappy = new TIntObjectHashMap<ThreadRegion>();
				map.put(thread, mappy);
			}

			for (SDGNode n : marked) {
				mappy.put(n.getId(), tr);
			}

			id++;
		}

		return result;
	}
}
