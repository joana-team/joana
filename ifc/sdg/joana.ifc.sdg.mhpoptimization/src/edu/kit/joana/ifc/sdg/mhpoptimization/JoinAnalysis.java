/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 *
 */
package edu.kit.joana.ifc.sdg.mhpoptimization;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.maps.MapUtils;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

/**
 *
 * @author giffhorn
 */
public class JoinAnalysis {
	private static Logger info = Log.getLogger(Log.L_MHP_INFO);
	private static Logger debug = Log.getLogger(Log.L_MHP_DEBUG);
	public static boolean IS_DEBUG = debug.isEnabled();

	/** thread allocation sites */
	private Collection<SDGNode> allocs;

	/** maps calls of Thread.join() to possible thread allocation sites */
	private Map<SDGNode, Set<SDGNode>> callJoin_alloc;

	private Map<SDGNode, Set<SDGNode>> alloc_callJoin; // maps thread
														// allocations to
														// calls of
														// Thread::join

	private Map<SDGNode, Set<SDGNode>> fork_alloc; // maps forks to thread
													// allocations
	private Map<SDGNode, Set<SDGNode>> alloc_fork; // maps thread
													// allocations to forks
	private SDG ipdg;
	private ThreadsInformation ti;

	public JoinAnalysis(SDG ipdg, ThreadsInformation ti, Collection<SDGNode> as) {
		this.ipdg = ipdg;
		this.ti = ti;
		allocs = as;
		alloc_callJoin = new HashMap<SDGNode, Set<SDGNode>>();
		callJoin_alloc = new HashMap<SDGNode, Set<SDGNode>>();
		fork_alloc = new HashMap<SDGNode, Set<SDGNode>>();
		alloc_fork = new HashMap<SDGNode, Set<SDGNode>>();
	}

	public void computeJoins() {
		computeMaps();
		detectMustJoining();
		if (IS_DEBUG) debug.outln("Resulting threads information:\n" + ti);
	}

	private void computeMaps() {
		joinAllocations();
		forkAllocations();
	}

	private static Set<SDGNode> collectSDGNodesFromIds(SDG sdg, int[] ids) {
		Set<SDGNode> ret = new HashSet<SDGNode>(); // possible allocation sites
													// of the thread on which
													// join() is called
		for (int alloc_id : ids) {
			ret.add(sdg.getNode(alloc_id));
		}
		return ret;
	}

	private void joinAllocations() {
		for (SDGNode n : ipdg.vertexSet()) {
			if (n.getKind() == SDGNode.Kind.ENTRY && n.getBytecodeName().equals(JavaMethodSignature.JavaLangThreadJoin)) {
				for (SDGEdge e : ipdg.incomingEdgesOf(n)) {
					if (e.getKind() == SDGEdge.Kind.CALL) {
						SDGNode joinCall = e.getSource(); // call site of a call
															// to Thread.join()
						assert joinCall.getAllocationSites() != null;
						Set<SDGNode> allocs = collectSDGNodesFromIds(ipdg, joinCall.getAllocationSites());
						callJoin_alloc.put(joinCall, allocs);
					}
				}
			}
		}

		alloc_callJoin = MapUtils.invert(callJoin_alloc);
	}

	private void forkAllocations() {
		for (SDGNode fork : ti.getAllForks()) {
			assert fork.getKind() == SDGNode.Kind.CALL;
			if (fork.getAllocationSites() != null) {
				info.outln("fork site with id " + fork + " contains allocation site information.\n");
				Set<SDGNode> allocs = collectSDGNodesFromIds(ipdg, fork.getAllocationSites());
				fork_alloc.put(fork, allocs);
			} else {
				Log.ERROR.outln("fork site with id " + fork + " contains no allocation site information!\n");
			}
		}

		alloc_fork = MapUtils.invert(fork_alloc);
	}

	private void detectMustJoining() {
		for (SDGNode alloc : allocs) {
			// 1. the thread has to be unique
			Set<SDGNode> forks = alloc_fork.get(alloc);
			if (forks == null || forks.size() != 1)
				continue;

			// 2. the thread object has to be unique, too
			if (fork_alloc.get(forks.iterator().next()).size() != 1)
				continue;

			// 3. the thread must not be dynamic
			ThreadInstance thread = ti.getThread(alloc_fork.get(alloc).iterator().next());
			if (thread.isDynamic())
				continue;

			// 4. the joining has to be unique
			Set<SDGNode> joins = alloc_callJoin.get(alloc);
			if (joins == null || joins.size() != 1)
				continue;

			// 5. the thread object has to be unique, too
			if (callJoin_alloc.get(joins.iterator().next()).size() != 1)
				continue;

			// we have found a must-joining!
			thread.setJoin(alloc_callJoin.get(alloc).iterator().next());
		}
	}
}
