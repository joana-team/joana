/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.mhpoptimization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ifc.sdg.mhpoptimization.ThreadAllocationAnalysis.LoopDetPrec;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;


/**
 * @author giffhorn
 *
 */
public final class CSDGPreprocessor {

	private static final Logger info = Log.getLogger(Log.L_MHP_INFO); 
	private static final Logger debug = Log.getLogger(Log.L_MHP_DEBUG); 
	private static final boolean IS_DEBUG = debug.isEnabled();

	private CSDGPreprocessor() {}
	
	public static final void preprocessSDG(SDG g) {
		// 2. clone Thread::start
		info.out("analyzing threads...");
		if (IS_DEBUG) debug.outln("  duplicating Thread::start...");
		
		info.outln("done");
		if (IS_DEBUG) debug.outln("		done");

		// 3. extract and fold the control flow graph
		if (IS_DEBUG) debug.out("  extracting the CFG...");
		CFG cfg = ICFGBuilder.extractICFG(g);
		if (IS_DEBUG) debug.outln("			done");

		// 3. run thread allocation
		info.out("running thread allocation analysis...");
		if (IS_DEBUG) debug.out("  running thread allocation analysis...");
		ThreadAllocationAnalysis alloc = new ThreadAllocationAnalysis(cfg, LoopDetPrec.PRECISE);
		alloc.compute();
		if (IS_DEBUG) debug.outln("	done");

		// 4. compute detailed information about the threads
		if (IS_DEBUG) debug.out("  collect thread infos...");
		ThreadsInformation ti = ThreadsInfoCollector.createThreadsInformation(alloc, cfg);
		g.setThreadsInfo(ti);
		info.outln("done");
		if (IS_DEBUG) debug.outln("		done");

		// 5. insert and propagate thread IDs
		info.out("propagating thread IDs...");
		if (IS_DEBUG) debug.out("  propagating thread IDs...");
		propagateThreadIDs(ti, g);
		info.outln("done");
		if (IS_DEBUG) debug.outln("		done");

		// 6. analyze join points
		info.out("computing join points...");
		if (IS_DEBUG) debug.out("  computing join points...");
		JoinAnalysis ja = new JoinAnalysis(g, ti);
		ja.computeJoins();
		info.outln("done");
		if (IS_DEBUG) {
			debug.outln("Thread information after join analysis: ");
			for (ThreadInstance thread : ti) {
				debug.outln(thread+"\n");
			}
			debug.outln("		done");
		}
		// 7. insert JOIN-Edges to store the result of the join analysis
		info.out("insert join edges...");
		if (IS_DEBUG) debug.out("  adding join edges...");
		createJoinEdges(g, ti);
		info.outln("done");
		if (IS_DEBUG) debug.outln("			done");
	}
	
	public static final void addSynchEdges(SDG g) {
		if (IS_DEBUG)
			debug.out("  adding synch edges...");
		SynchAnalysis sa = new SynchAnalysis();
		if (IS_DEBUG)
			debug.outln("			done");
		sa.analyze(g);

		if (IS_DEBUG)
			debug.outln("--> finished postprocessing");
	}

	private static final void propagateThreadIDs(ThreadsInformation ti, SDG graph) {
		// adjust the thread IDs in the SDG
		HashMap<SDGNode, LinkedList<Integer>> s = new HashMap<SDGNode, LinkedList<Integer>>();

		for (int t = 0; t < ti.getNumberOfThreads(); t++) {
			SDGNode run = ti.getThreadEntry(t);
			LinkedList<Integer> IDs = s.get(run);
			if (IDs == null) {
				IDs = new LinkedList<Integer>();
				s.put(run, IDs);
			}
			IDs.add(t);
		}

		for (Map.Entry<SDGNode, LinkedList<Integer>> runAndIDs : s.entrySet()) {
			SDGNode run = runAndIDs.getKey();
			LinkedList<Integer> IDs = runAndIDs.getValue();
			int[] array = new int[IDs.size()];
			for (int i = 0; i < array.length; i++) {
				array[i] = IDs.get(i);
			}
			run.setThreadNumbers(array);
		}


		// now propagate the IDs throughout the graph
		Set<SDGNode> threadEntries = s.keySet();
		HashMap<SDGNode, TIntHashSet> ids = new HashMap<SDGNode, TIntHashSet>();

		// iterate over the thread entries and determine all nodes that are reachable thread locally
		for (SDGNode entry : threadEntries) {
			// two worklists
			LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
			// already visited nodes
			HashSet<SDGNode> marked = new HashSet<SDGNode>();
			// the thread instance IDs of the current thread entry
			int[] threads = entry.getThreadNumbers();

			// init the worklist
			worklist.add(entry);

			while (!worklist.isEmpty()) {
				SDGNode next = worklist.poll();
				TIntHashSet current = ids.get(next);
				if (current == null) {
					current = new TIntHashSet();
					ids.put(next, current);
				}
				for (int t : threads) {
					current.add(t);
				}

				// traverse all intra-thread edges
				for (SDGEdge e : graph.outgoingEdgesOf(next)) {
					if (e.getKind().isThreadEdge()) {
						continue;
					} else if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT || e.getKind() == SDGEdge.Kind.RETURN) {
						/**
						 * Following return- or param-out-edges here leads to a wrong result. Suppose we have
						 * two distinguishable threads which both call a common method. If the return edges
						 * from that method to each caller in the different threads are traversed, both thread
						 * IDs are propagated to both threads. In this sense, return- and param-out-edges are
						 * inter-thread egdes. Call-edges are not inter-thread edges, since a call never
						 * changes the active thread (those calls which do are modeled by fork edges).
						 * For some reason, the precise mhp analysis crashes with a NullPointerException, if these edges
						 * are traversed here...
						 * Some debugging showed, that not all nodes are assigned a thread region.
						 */
						continue;
					} else {
						if (marked.add(e.getTarget())) {
							worklist.add(e.getTarget());
						}
					}
				}
			}
		}

		for (Map.Entry<SDGNode, TIntHashSet> p : ids.entrySet()) {
			SDGNode n = p.getKey();
			TIntHashSet set = p.getValue();
			int[] ts = new int[set.size()];
			int i = 0;
			for (TIntIterator iter = set.iterator(); iter.hasNext(); ) {
				int t = iter.next();
				ts[i] = t;
				i++;
			}
			n.setThreadNumbers(ts);
		}

		HashSet<SDGNode> error = new HashSet<SDGNode>();
		for (SDGNode n : graph.vertexSet()) {
			if (n.getThreadNumbers() == null) error.add(n);
		}
		if (!error.isEmpty()) {
			info.outln("dangling nodes? ");
			for (SDGNode errNode : error) {
				info.outln(errNode + " " + graph.getEntry(errNode).getBytecodeMethod());
			}
		}
	}

	private static final void createJoinEdges(SDG graph, ThreadsInformation ti) {
		for (ThreadInstance i : ti) {
			if (i.getJoin() != null) {
				SDGNode source = i.getExit();
				SDGNode target = i.getJoin();
				SDGEdge e = new SDGEdge(source, target, SDGEdge.Kind.JOIN);
				graph.addEdge(e);
			}
		}
	}
}
