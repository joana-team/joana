/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.mhpoptimization;

import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ifc.sdg.mhpoptimization.ThreadAllocationAnalysis.SpawnNumber;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.util.Pair;


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
    	final SDGNode exit = cfg.getExit(node);
    	if (exit == null) {
    		throw new IllegalStateException("No exit node for: " + node);
    	}
    	
    	return exit;
        
    }

    /**
	 * Finds the entry node of the main method of the given sdg.
	 * @param g sdg to find main entry node of
	 * @return entry node of the main method of the given sdg
	 */
	private static SDGNode findMainEntry(CFG g) {
		final SDGNode mainEntry = g.getRoot();

		if (mainEntry == null) throw new IllegalStateException();
		
		return mainEntry;
    }

    /**
     * Computes information about the threads in a CFG.
     */
    public static ThreadsInformation createThreadsInformation(ThreadAllocationAnalysis ta, CFG cfg) {
        LinkedList<ThreadInstance> result = new LinkedList<ThreadInstance>();

        
        // create a ThreadInstance for the main thread {
        final ThreadInstance main; {
            final SDGNode entry = findMainEntry(cfg);
            final SDGNode exit  = findExitNode(entry, cfg);
            main = new ThreadInstance(ThreadInstance.MAIN_THREAD_ID, entry, exit, null, new LinkedList<SDGNode>(), false);
        }

        result.add(main);

        final Logger debug = Log.getLogger(Log.L_MHP_DEBUG);
        debug.outln("entry: " + main.getEntry());
        debug.outln("    fork: " + main.getFork());
        debug.outln("    context: " + main.getThreadContext());

        // a thread is identified by its calling context
        Set<Pair<SDGNode, DynamicContext>> threads = ta.getThreads();
        int id = 1;

        // determine the thread instances
        for (Pair<SDGNode, DynamicContext> pair : threads) {
        	DynamicContext thread = pair.getSecond();
        	SDGNode forkNode = pair.getFirst();
        	debug.outln("entry: "+thread.getNode());
        	debug.outln("    fork: "+ forkNode);
        	debug.outln("    context: "+thread.getCallStack());

            // distinguish between dynamic and not dynamic threads
            final boolean dynamic = ta.getThreadAmount().get(thread) == SpawnNumber.INDEFINITE;
            
            final SDGNode entry = thread.getNode();
            final SDGNode exit  = findExitNode(entry, cfg);
            ThreadInstance ti =
            	new ThreadInstance(id, entry, exit, thread.getCallStack().peek(), thread.getCallStack(), dynamic);

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
