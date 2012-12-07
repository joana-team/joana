/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;


/**
 * @author giffhorn
 *
 */
public class RemoveInterference {

    private final SDG g;
    private final Logger log = Log.getLogger(Log.L_SDG_INFO);
    private final Logger debug = Log.getLogger(Log.L_SDG_INTERFERENCE_DEBUG);

    private RemoveInterference(SDG g) {
        this.g = g;
    }

    private SDG removeInterference() {
        // insert and propagate thread IDs
        log.outln("propagating thread IDs...");
        debug.out("  propagating thread IDs...");
        propagateThreadIDs(g);
        debug.outln("		done");
        log.outln("done\n");

        // remove redundant interference edgies
        log.outln("start MHP analysis...");
        debug.out("  running MHP analysis...");
        MHPAnalysis mhp = PreciseMHPAnalysis.analyze(g);
        debug.outln("		done");
        debug.outln("  removing spurious concurrency edges...");
        cleanCSDG(g, mhp);
        log.outln("done\n");

        return g;
    }

    private void propagateThreadIDs(SDG graph) {
        // adjust the thread IDs in the SDG
        HashMap<SDGNode, LinkedList<Integer>> s = new HashMap<SDGNode, LinkedList<Integer>>();
        ThreadsInformation ti = graph.getThreadsInfo();

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
                    if (e.getKind() == SDGEdge.Kind.JOIN
                            || e.getKind() == SDGEdge.Kind.FORK
                            || e.getKind() == SDGEdge.Kind.FORK_IN
                            || e.getKind() == SDGEdge.Kind.FORK_OUT
                            || e.getKind() == SDGEdge.Kind.INTERFERENCE
                            || e.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {

                        continue;

                    } else if (e.getKind() == SDGEdge.Kind.RETURN
                                || e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {

                        continue;

                    } else {
                        if (marked.add(e.getTarget())) {
                            worklist.add(e.getTarget());;
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
        
        if (!error.isEmpty()) log.outln("dangling nodes? " + error);
    }

    private void cleanCSDG(SDG graph, MHPAnalysis mhp) {
        LinkedList<SDGEdge> remove = new LinkedList<SDGEdge>();
        int all = 0;
        int x = 0;
        for (SDGEdge e : graph.edgeSet()) {
            if (e.getKind() == SDGEdge.Kind.INTERFERENCE
                    || e.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {
            	all++;
                // nicht parallel
                if (!mhp.isParallel(e.getSource(), e.getTarget()) && !isParallel(e.getSource(), e.getTarget())) {
                    remove.add(e);
                    x++;
                }
            }
        }

        for (SDGEdge e : remove) {
            graph.removeEdge(e);
        }

        debug.outln("	" + x + " of " + all + " edges removed");
    }

    private boolean isParallel(SDGNode m, SDGNode n) {
    	for (int mt : m.getThreadNumbers()) {
    		for (int nt : n.getThreadNumbers()) {
    			if (areDuplicates(mt, nt)) {
    				return true;
    			}
    		}
    	}

    	return false;
    }

    private boolean areDuplicates(int t1, int t2) {
    	// handcoded, because PreciseMHPAnalysis is too smart to be tricked...
    	return t1 == 1 && (t2 == 2 || t2 == 3)
    			|| t1 == 2 && (t2 == 1 || t2 == 3)
    			|| t1 == 3 && (t2 == 1 || t2 == 2);
    }

    /* Factories */

    public static SDG createCSDG(String file) throws IOException {
        SDG g = SDG.readFrom(file);
        RemoveInterference p = new RemoveInterference(g);
        SDG csdg = p.removeInterference();
        return csdg;
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

}
