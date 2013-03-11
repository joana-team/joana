/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.interference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;

import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphModifier;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;

/**
 * @author giffhorn
 *
 */
public class CSDGPreprocessor {
	private static final boolean DEBUG = false;

    private SDG g;

    private CSDGPreprocessor(SDG g) {
        this.g = g;
    }

    private SDG createCSDG(IProgressMonitor cancel) throws CancelException {
    	runMHP(cancel);

    	return this.g;
    }

    private MHPAnalysis runMHP(IProgressMonitor cancel) throws CancelException {
    	MHPAnalysis mhp = null;

        try {
        	long time = 0L;
        	long tmp = 0L;
        	Log.info("\nPostprocessing concurrent SDG of "+g.getName());
        	if (DEBUG) System.out.println("Postprocessing concurrent SDG of "+g.getName());
            // 1. fix control flow
        	if (DEBUG) System.out.print("  fixing control flow...");
            GraphModifier.repairForkSiteEdges(g);
            fixControlFlow(g);
//            fixSDG(g);
            if (DEBUG) System.out.println("		done");

            if (cancel.isCanceled()) {
                throw CancelException.make("Operation aborted.");
            }

            // 2. clone Thread::start
            Log.info("analyzing threads...");
            if (DEBUG) System.out.print("  duplicating Thread::start...");
            ThreadStartDuplicator duplicator = new ThreadStartDuplicator(g, ThreadStartDuplicator.JUERGEN);
            duplicator.dupe();
            Log.info("done\n");
            if (DEBUG) System.out.println("		done");

            // 3. extract and fold the control flow graph
            if (DEBUG) System.out.print("  extracting the CFG...");
            CFG cfg = ICFGBuilder.extractICFG(g);
            FoldedCFG folded = GraphFolder.foldIntraproceduralSCC(cfg);
            if (DEBUG) System.out.println("			done");

            // 3. run thread allocation
            Log.info("running thread allocation analysis...");
            if (DEBUG) System.out.print("  running thread allocation analysis...");
            tmp = System.currentTimeMillis();
            ThreadAllocation alloc = new ThreadAllocation(g, cfg, folded);
            alloc.compute();
            time += System.currentTimeMillis() - tmp;
            if (DEBUG) System.out.println("	done");

            // 4. compute detailed information about the threads
            if (DEBUG) System.out.print("  collect thread infos...");
            CFG icfg = ICFGBuilder.extractICFG(g);
            tmp = System.currentTimeMillis();
            ThreadsInformation ti = ThreadsInfoCollector.createThreadsInformation(alloc, icfg);
            g.setThreadsInfo(ti);
            time += System.currentTimeMillis() - tmp;
            Log.info("done\n");
            if (DEBUG) System.out.println("		done");

            // 5. insert and propagate thread IDs
            Log.info("propagating thread IDs...");
            if (DEBUG) System.out.print("  propagating thread IDs...");
            tmp = System.currentTimeMillis();
            propagateThreadIDs(ti, g);
            time += System.currentTimeMillis() - tmp;
            Log.info("done\n");
            if (DEBUG) System.out.println("		done");

            // 6. analyze join points
            Log.info("computing join points...");
            if (DEBUG) System.out.print("  computing join points...");
            tmp = System.currentTimeMillis();
            JoinAnalysis ja = new JoinAnalysis(g, ti, duplicator.getAllocs());
            ja.computeJoins();
            time += System.currentTimeMillis() - tmp;
            Log.info("done\n");
            if (DEBUG) System.out.println("		done");

            // 7. insert JOIN-Edges to store the result of the join analysis
            Log.info("insert join edges...");
            if (DEBUG) System.out.print("  adding join edges...");
            tmp = System.currentTimeMillis();
            createJoinEdges(g, ti);
            time += System.currentTimeMillis() - tmp;
            Log.info("done\n");
            if (DEBUG) System.out.println("			done");

            // 8. remove redundant interference edgies
            Log.info("start MHP analysis...");
            if (DEBUG) System.out.print("  running MHP analysis...");
            tmp = System.currentTimeMillis();
            mhp = PreciseMHPAnalysis.analyze(g);
            time += System.currentTimeMillis() - tmp;
            if (DEBUG) System.out.println("		done");
            if (DEBUG) System.out.println("  removing spurious concurrency edges...");
            cleanCSDG(g, mhp);
            Log.info("done\n");

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
            if (DEBUG) System.out.print("  adding synch edges...");
            SynchAnalysis sa = new SynchAnalysis();
            if (DEBUG) System.out.println("			done");
            sa.analyze(g);

            if (DEBUG) System.out.println("--> finished postprocessing");

//        	ctr = 0;
//        	for (SDGEdge e : g.edgeSet()) {
//        		if (e.getKind() == SDGEdge.Kind.INTERFERENCE) {
//        			ctr++;
//        		}
//        	}
        	System.out.println("MHP time needed: " + time);
        	System.out.println("Thread Regions: " + mhp.getThreadRegions().size());
        } catch (CancelException e) {
        	// a cancel exception is not an error in the computation.
        	throw e;
        } catch (Exception e) {
            Log.info("Creating cSDG failed and skipped:");
            e.printStackTrace();
        }

        return mhp;
    }

    // this should not be necessary by now. We will keep it for a while to assure that
    // it really works.
    private static void fixControlFlow(SDG sdg) {
        // connect root vertex with the CFG
        SDGNode root = sdg.getRoot();
        Set<SDGNode> set = sdg.vertexSet();

        for (SDGNode n : set) {
            if (n != root && n.getProc() == root.getProc() && sdg.getIncomingEdgesOfKind(n, SDGEdge.Kind.CONTROL_FLOW).isEmpty()){
                sdg.addEdge(new SDGEdge(root, n, SDGEdge.Kind.CONTROL_FLOW));
                // print to stderr to embarrass the sdg creator
                System.err.println("fix cfg: " + root.getLabel() + " to " + n.getLabel());
            }
        }

        // inline parameter nodes (implicitly adds return edges)
        GraphModifier.inlineParameterVertices(sdg);
//        GraphModifier.addReturnEdgesTo(sdg);

        List<SDGNodeTuple> callSites = sdg.getAllCallSites();
        for (SDGNodeTuple cs : callSites) {
        	List<SDGEdge> calls = sdg.getOutgoingEdgesOfKind(cs.getFirstNode(), SDGEdge.Kind.CALL);
        	List<SDGEdge> returns = sdg.getIncomingEdgesOfKind(cs.getSecondNode(), SDGEdge.Kind.RETURN);
        	if (calls.size() != returns.size()) {
        		System.out.println(calls);
        		System.out.println(returns);
        		throw new RuntimeException();
        	}
        }
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

        for (SDGNode run : s.keySet()) {
            LinkedList<Integer> IDs = s.get(run);
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

            // outerWorklist only contains procedure entries, thus allowing to visit
            // all nodes in one procedure before leaving towards another procedure
            while (!worklist.isEmpty()) {
                // a procedure entry
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

                    } else if (e.getKind() == SDGEdge.Kind.RETURN
                                || e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                        continue;

                    } else {
                        if (marked.add(e.getTarget())) {
                            worklist.add(e.getTarget());
                        }
                    }
                }
            }
        }

        for (SDGNode n : ids.keySet()) {
        	TIntHashSet set = ids.get(n);
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
        if (!error.isEmpty()) System.out.println("dangling nodes? "+error);
    }

    @SuppressWarnings("unused")
	private void cleanCSDG(SDG graph, MHPAnalysis mhp) {
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
            graph.removeEdge(e);
        }

        if (DEBUG) System.out.println("	"+x+" of "+all+" edges removed");
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

    public static MHPAnalysis runMHP(SDG g, IProgressMonitor cancel) throws CancelException {
        CSDGPreprocessor p = new CSDGPreprocessor(g);
        MHPAnalysis mhp = p.runMHP(cancel);

        return mhp;
    }

    public static SDG createCSDG(SDG g, IProgressMonitor cancel) throws CancelException {
        CSDGPreprocessor p = new CSDGPreprocessor(g);
        SDG csdg = p.createCSDG(cancel);

        return csdg;
    }

    public static SDG createCSDG(String file) throws IOException {
        SDG g = SDG.readFrom(file);
        CSDGPreprocessor p = new CSDGPreprocessor(g);
        SDG csdg = null;

        try {
        	csdg = p.createCSDG(new NullProgressMonitor());
        } catch (CancelException e) {
        	throw new IllegalStateException(e);
        }

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

        try {
        	csdg = p.createCSDG(new NullProgressMonitor());
        } catch (CancelException e) {
        	throw new IllegalStateException(e);
        }

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
