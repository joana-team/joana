/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.mhpoptimization;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.SimpleMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ifc.sdg.mhpoptimization.ThreadAllocation.LoopDetPrec;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;


/**
 * @author giffhorn
 *
 */
public class CSDGPreprocessor {

	private static final Logger info = Log.getLogger(Log.L_MHP_INFO); 
	private static final Logger debug = Log.getLogger(Log.L_MHP_DEBUG); 
	private static final boolean IS_DEBUG = debug.isEnabled();

	public enum MHPPrecision {
		SIMPLE, PRECISE;
	}

	private SDG g;

	public CSDGPreprocessor(SDG g) {
		this.g = g;
	}

	private SDG createCSDG() {
		runMHP(MHPPrecision.PRECISE);

		return this.g;
	}

	public void preprocessSDG() {
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
		ThreadAllocation alloc = new ThreadAllocation(cfg, LoopDetPrec.PRECISE);
		alloc.compute();
		if (IS_DEBUG) debug.outln("	done");

		// 4. compute detailed information about the threads
		if (IS_DEBUG) debug.out("  collect thread infos...");
		CFG icfg = ICFGBuilder.extractICFG(g);
		ThreadsInformation ti = ThreadsInfoCollector.createThreadsInformation(alloc, icfg);
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

	private MHPAnalysis runMHP(MHPPrecision prec) {
		MHPAnalysis mhp = null;
		try {
			preprocessSDG();

			// 8. remove redundant interference edgies
			info.out("start MHP analysis...");
			if (IS_DEBUG) debug.out("  running MHP analysis (" + prec + ")...");
			if (prec == MHPPrecision.PRECISE) {
				mhp = PreciseMHPAnalysis.analyze(g);
			} else {
				mhp = SimpleMHPAnalysis.analyze(g);
			}

			if (IS_DEBUG) debug.outln("		done");
			if (IS_DEBUG) debug.outln("  removing spurious concurrency edges...");
			pruneInterferences(g, mhp);
			info.outln("done");

			// 9. remove FORK_OUT edges
			LinkedList<SDGEdge> l = new LinkedList<SDGEdge>();
			for (SDGEdge e : g.edgeSet()) {
				if (e.getKind() == SDGEdge.Kind.FORK_OUT) {
					l.add(e);
				}
			}
			for (SDGEdge e : l) {
				g.removeEdge(e);
			}

			// 10. add SYNCHRONIZATION edges
			if (IS_DEBUG) debug.out("  adding synch edges...");
			SynchAnalysis sa = new SynchAnalysis();
			if (IS_DEBUG) debug.outln("			done");
			sa.analyze(g);

			if (IS_DEBUG) debug.outln("--> finished postprocessing");

			//        	ctr = 0;
			//        	for (SDGEdge e : g.edgeSet()) {
			//        		if (e.getKind() == SDGEdge.Kind.INTERFERENCE) {
			//        			ctr++;
			//        		}
			//        	}
			if (IS_DEBUG) debug.outln("Thread Regions: " + mhp.getThreadRegions().size());
		} catch (Exception e) {
			Log.ERROR.outln("Creating cSDG failed and skipped", e);
			return null;
		}

		return mhp;
	}
	
	public void propagateThreadIDs(ThreadsInformation ti, SDG graph) {
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
				info.outln(errNode + " " + g.getEntry(errNode).getBytecodeMethod());
			}
		}
	}

	private void pruneInterferences(SDG graph, MHPAnalysis mhp) {
		LinkedList<SDGEdge> remove = new LinkedList<SDGEdge>();
		int all = 0;
		int x = 0;
		for (SDGEdge e : graph.edgeSet()) {
			if (e.getKind() == SDGEdge.Kind.INTERFERENCE
					|| e.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {
				all++;
				// nicht parallel
				if (!mhp.isParallel(e.getSource(), e.getTarget())) {
					remove.add(e);
					x++;
				}
			}
		}

		for (SDGEdge e : remove) {
			if (IS_DEBUG) debug.outln("Edge between " + e.getSource() + " and " + e.getTarget() + " of kind " + e.getKind() + " is spurious.");
			graph.removeEdge(e);
		}

		if (IS_DEBUG) debug.outln("	" + x + " of " + all + " edges removed");
	}

	private void createJoinEdges(SDG graph, ThreadsInformation ti) {
		for (ThreadInstance i : ti) {
			if (i.getJoin() != null) {
				SDGNode source = i.getExit();
				SDGNode target = i.getJoin();
				SDGEdge e = new SDGEdge(source, target, SDGEdge.Kind.JOIN);
				graph.addEdge(e);
			}
		}
	}

	/* Factories */
	public static MHPAnalysis runMHP(SDG g) {
		return runMHP(g, MHPPrecision.PRECISE);
	}
	
	public static void justPrecprocess(SDG g) {
		CSDGPreprocessor p = new CSDGPreprocessor(g);
		p.preprocessSDG();
	}

	public static MHPAnalysis runMHP(SDG g, MHPPrecision prec) {
		CSDGPreprocessor p = new CSDGPreprocessor(g);
		MHPAnalysis mhp = p.runMHP(prec);
		return mhp;
	}

	public static SDG createCSDG(SDG g) {
		CSDGPreprocessor p = new CSDGPreprocessor(g);
		SDG csdg = p.createCSDG();

		return csdg;
	}

	public static SDG createCSDG(String file) throws IOException {
		SDG g = SDG.readFrom(file);
		CSDGPreprocessor p = new CSDGPreprocessor(g);
		SDG csdg = null;
			csdg = p.createCSDG();
		return csdg;
	}

	public static void createAndSaveCSDG(String file, String pathForCSDG) throws IOException {
		SDG csdg = createCSDG(file);

		saveCSDG(csdg, pathForCSDG);
	}

	public static void createAndSaveCSDG(String file) throws IOException {
		SDG csdg = createCSDG(file);

		String content = SDGSerializer.toPDGFormat(csdg);
		File f = new File(file);
		FileWriter w = new FileWriter(f);

		w.write(content);
		w.flush();
		w.close();
	}

	public static void createAndSaveCSDG(SDG sdg, String file) throws IOException {
		CSDGPreprocessor p = new CSDGPreprocessor(sdg);

		SDG csdg = null;

			csdg = p.createCSDG();

		String content = SDGSerializer.toPDGFormat(csdg);
		File f = new File(file);
		FileWriter w = new FileWriter(f);

		w.write(content);
		w.flush();
		w.close();
	}

	private static void saveCSDG(SDG g, String path) throws IOException {
		String name = g.getName();
		String content = SDGSerializer.toPDGFormat(g);
		File f = new File(path + File.separator + name + ".pdg");
		FileWriter w = new FileWriter(f);

		w.write(content);
		w.flush();
		w.close();
	}

	public static void juergenTest(String alt, String neu) throws Exception {
		SDG g = createCSDG(alt);

		String content = SDGSerializer.toPDGFormat(g);
		File f = new File(neu);
		FileWriter w = new FileWriter(f);

		w.write(content);
		w.flush();
		w.close();
	}

	//    public static void main(String[] args) throws IOException {
	//    	String file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/two/conc.ac.AlarmClock.pdg";
	//    	createAndSaveCSDG(file);
	//
	//    	file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/two/conc.bb.ProducerConsumer.pdg";
	//    	createAndSaveCSDG(file);
	//
	//    	file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/two/conc.ds.DiskSchedulerDriver.pdg";
	//    	createAndSaveCSDG(file);
	//
	//    	file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/two/conc.lg.LaplaceGrid.pdg";
	//    	createAndSaveCSDG(file);
	//
	//    	file = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/two/conc.TimeTravel.pdg";
	//    	createAndSaveCSDG(file);
	//    }
}
