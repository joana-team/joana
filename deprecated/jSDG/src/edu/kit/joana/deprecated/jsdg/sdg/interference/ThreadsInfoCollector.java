/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.interference;

import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.PDGs.PDGIterator;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;


/**
 * A class for informations about threads in a CFG.
 * For every thread it contains informations about, this informations consist of
 * - the entry node
 * - the exit node ('null' for the main thread)
 * - the fork node ('null' for the main thread)
 * - the thread allocation node ('null' for the main thread)
 * - the thread invocation node ('null' for the main thread)
 * - the join node (maybe 'null')
 * .
 * The class computes that information itself.
 *
 * -- Created on August 24, 2005
 *
 * @author  Dennis Giffhorn
 */
public final class ThreadsInfoCollector {
    private static boolean DEBUG = false;

    private ThreadsInfoCollector() { }

    /**
     * Computes information about the threads in a CFG.
     */
    public static ThreadsInformation createThreadsInformation(ThreadAllocation ta, CFG cfg) {
        LinkedList<ThreadInstance> result = new LinkedList<ThreadInstance>();

        // create a ThreadInstance for the main thread
        ThreadInstance main = new ThreadInstance(0, cfg.getRoot(), null, null);
        main.dynamic = false;
        result.add(main);

        LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
        wl.add(main.entry);
        while(!wl.isEmpty()) {
            SDGNode n = wl.poll();
            boolean exit = true;

            for (SDGEdge e : cfg.outgoingEdgesOf(n)) {
                if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
                    exit = false;
                    wl.add(e.getTarget());
                }
            }

            if (exit) main.exit = n;
        }


        // a thread is identified by its calling context
        Set<DynamicContext> threads = ta.getThreads();
        int id = 1;

        // determine the thread instances
        for (DynamicContext thread : threads) {
            if (DEBUG) System.out.println("entry: "+thread.getNode());
            if (DEBUG) System.out.println("    fork: "+thread.getCallStack().peek());
            if (DEBUG) System.out.println("    context: "+thread.getCallStack());

            ThreadInstance ti =
            	new ThreadInstance(id, thread.getNode(), thread.getCallStack().peek(), thread.getCallStack());

            // distinguish between dynamic and not dynamic threads
            if(ta.getThreadAmount().get(thread) == -1) {
                ti.dynamic = true;

            } else {
            	ti.dynamic = false;
            }

            result.add(ti);
            id++;
        }


        // for every ThreadInstance, compute exit
        for (ThreadInstance ti: result) {
            // exit node - does not work for main thread, but we don't need the exit node for main thread
            for (SDGEdge e : cfg.outgoingEdgesOf(ti.entry)) {
                if (e.getTarget().getKind() == SDGNode.Kind.EXIT) {
                    ti.exit = e.getTarget();
                    break;
                }
            }
        }

        if (DEBUG) {
        	for (ThreadInstance ti : result) {
        		System.out.println(ti+"\n");
        	}
        }
        return new ThreadsInformation(result);
    }

    /** main() for debugging
     *
     * @param args
     * @throws Exception
     */
    public static void main (String[] args) throws Exception {
    	PDGIterator iter = PDGs.getPDGs();
    	while (iter.hasNext()) {
    		SDG sdg = iter.next();
    		CFG cfg = ICFGBuilder.extractICFG(sdg);
            FoldedCFG folded = GraphFolder.foldIntraproceduralSCC(cfg);
            ThreadAllocation alloc = new ThreadAllocation(sdg, cfg, folded);
    		System.out.println("*******************\n"+sdg.getName());
    		ThreadsInformation ti = createThreadsInformation(alloc, cfg);
    		System.out.println(ti);
    		System.out.println();
    	}
    }
}
