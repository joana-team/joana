/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.mhpoptimization;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ifc.sdg.mhpoptimization.ThreadAllocationAnalysis.SpawnNumber;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;


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

    private ThreadsInfoCollector() { }


    private static SDGNode findExitNode(SDGNode node, CFG cfg) {
    	SDGNode entry = cfg.getEntry(node);
    	LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
    	Set<SDGNode> visited = new HashSet<SDGNode>();
        wl.add(entry);
        while(!wl.isEmpty()) {
            SDGNode n = wl.poll();
            visited.add(n);
            if (n.getKind() == SDGNode.Kind.EXIT) {
            	return n;
            } else {

            	for (SDGEdge e : cfg.outgoingEdgesOf(n)) {
            		if (!visited.contains(e.getTarget()) && e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
            			wl.add(e.getTarget());
            		}
            	}
            }
        }

        throw new IllegalStateException("Visited the following nodes, which not include an exit node: " + visited);
    }

    /**
	 * Finds the entry node of the main method of the given sdg.
	 * @param g sdg to find main entry node of
	 * @return entry node of the main method of the given sdg
	 */
	private static SDGNode findMainEntry(CFG g) {
    	for (SDGNode n : g.vertexSet()) {
    		if (g.inDegreeOf(n) == 0) {
    			//assert n.getBytecodeMethod().contains("main([Ljava/lang/String;)V");
    			return n;
    		}
    	}

    	throw new IllegalStateException();
    }

    /**
     * Computes information about the threads in a CFG.
     */
    public static ThreadsInformation createThreadsInformation(ThreadAllocationAnalysis ta, CFG cfg) {
        LinkedList<ThreadInstance> result = new LinkedList<ThreadInstance>();

        // create a ThreadInstance for the main thread
        ThreadInstance main = new ThreadInstance(0, findMainEntry(cfg), null, null);

        main.setDynamic(false);

        main.setExit(findExitNode(main.getEntry(), cfg));

        result.add(main);

        final Logger debug = Log.getLogger(Log.L_MHP_DEBUG);
        debug.outln("entry: " + main.getEntry());
        debug.outln("    fork: " + main.getFork());
        debug.outln("    context: " + main.getThreadContext());

        // a thread is identified by its calling context
        Set<DynamicContext> threads = ta.getThreads();
        int id = 1;

        // determine the thread instances
        for (DynamicContext thread : threads) {
        	debug.outln("entry: "+thread.getNode());
        	debug.outln("    fork: "+thread.getCallStack().peek());
        	debug.outln("    context: "+thread.getCallStack());

            ThreadInstance ti =
            	new ThreadInstance(id, thread.getNode(), thread.getCallStack().peek(), thread.getCallStack());
            ti.setExit(findExitNode(ti.getEntry(), cfg));

            // distinguish between dynamic and not dynamic threads
            if(ta.getThreadAmount().get(thread) == SpawnNumber.INDEFINITE) {
                ti.setDynamic(true);

            } else {
            	ti.setDynamic(false);
            }

            result.add(ti);
            id++;
        }

        if (debug.isEnabled()) {
        	for (ThreadInstance ti : result) {
        		debug.outln(ti + "\n");
        	}
        }
        return new ThreadsInformation(result);
    }
}
