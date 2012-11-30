/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.interference;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.PDGs.PDGIterator;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;


/** Implements a thread allocation analysis.
 * It is based on the thread allocation analysis of Eric Ruf.
 *
 * -- Created on October 18, 2006
 *
 * @author  Dennis Giffhorn
 */
public class ThreadAllocation {
    public static boolean DEBUG = true;

    /** A CFG and its contexts. */
    private CFG cfg;
    private FoldedCFG folded;
    private DynamicContextManager conMan;

    private HashMap<SDGNode, Collection<DynamicContext>> run_thread;
    private HashSet<DynamicContext> threads; // maps thread allocations to thread entries
    private HashMap<DynamicContext, Integer> thread_amount; // maps thread allocations to number of invocations

    /** Creates a new instance of ThreadAllocation
     * @param g  A SDG.
     */
    public ThreadAllocation(SDG sdg, CFG cfg, FoldedCFG folded) {
        this.cfg = cfg;
        this.folded = folded;

        // create the context manager
        conMan = new DynamicContextManager(sdg);
    }

    public Set<DynamicContext> getThreads() {
    	return Collections.unmodifiableSet(threads);
    }

    public HashMap<DynamicContext, Integer> getThreadAmount() {
    	return thread_amount;
    }

    /** Executes the thread allocation analysis.
     */
    public void compute() {
        // determine all existing thread run methods
        // the List will contain their entry nodes
        List<SDGNode> runEntries = allRunMethods();
        if (DEBUG) System.out.println("run-method entries                : "+runEntries);

        // determine thread contexts
        run_thread = threadContexts(runEntries);
        threads = new HashSet<DynamicContext>();
        for (Collection<DynamicContext> l : run_thread.values()) {
        	threads.addAll(l);
        }

        if (DEBUG) System.out.println("run-entries -> thread contexts : "+run_thread);
        if (DEBUG) System.out.println("threads : "+threads);

        // compute the number of threads
        thread_amount = computeNumberOfThreads();
        if (DEBUG) System.out.println("thread -> instances   : "+thread_amount);

        // compute the thread invocation structure
        HashMap<DynamicContext, List<DynamicContext>> str = invocationStructure();
        if (DEBUG) System.out.println("thread invocation structure:\n "+str);
    }

    private HashMap<DynamicContext, List<DynamicContext>> invocationStructure() {
    	HashMap<DynamicContext, List<DynamicContext>> result = new HashMap<DynamicContext, List<DynamicContext>>();

    	for (DynamicContext c : threads) {
    		DynamicContext invokedBy = null;
    		int diff = c.size();

    		for (DynamicContext d : threads) {
    			if (c == d) continue;
    			if (d.isSuffixOf(c) && diff > (c.size() - d.size())) {
    				invokedBy = d;
    			}
    		}

    		if (invokedBy != null) {
    			List<DynamicContext> invoked = result.get(invokedBy);
    			if (invoked == null) {
    				invoked = new LinkedList<DynamicContext>();
    				result.put(invokedBy, invoked);
    			}
    			invoked.add(c);
    		}
    	}

    	return result;
    }

    /** Computes the entries of all run methods in the program.
     * @return A list with the found entry nodes.
     */
    private List<SDGNode> allRunMethods() {
        List<SDGNode> result = new LinkedList<SDGNode>();

        // traverse all fork edges to find the entries
        for (SDGEdge fork : cfg.edgeSet()) {
            if (fork.getKind() != SDGEdge.Kind.FORK) continue;

            if (!result.contains(fork.getTarget())) {
                result.add(fork.getTarget());
            }
        }

        return result;
    }

    private HashMap<SDGNode, Collection<DynamicContext>> threadContexts(List<SDGNode> runEntries) {
    	HashMap<SDGNode, Collection<DynamicContext>> tc = new HashMap<SDGNode, Collection<DynamicContext>>();

    	for (SDGNode run : runEntries) {
    		Collection<DynamicContext> cons = conMan.getExtendedContextsOf(run);
    		tc.put(run, cons);
    	}

    	return tc;
    }

    private HashMap<DynamicContext, Integer> computeNumberOfThreads () {
    	HashMap<DynamicContext, Integer> result = new HashMap<DynamicContext, Integer>();
    	List<DynamicContext> remainingThreads = new LinkedList<DynamicContext>();

    	// search for recursive calls in the contexts
    	for (DynamicContext thread : threads) {
    		boolean recursive = false;

    		for (SDGNode n : thread.getCallStack()) {
    			if (n.getId() < 0) {
    				result.put(thread, -1);
    				recursive = true;
    			}
    		}

    		if (!recursive) {
    			remainingThreads.add(thread);
    		}
    	}

    	// search for loops in the TCFG
    	for (DynamicContext thread : remainingThreads) {
    		if (isInALoop(thread)) {
    			result.put(thread, -1);

    		} else {
    			result.put(thread, 1);
    		}
    	}

    	// handle recursive thread generation
    	LinkedList<DynamicContext> recursiveThreads = new LinkedList<DynamicContext>();
    	LinkedList<DynamicContext> refinedThreads = new LinkedList<DynamicContext>();
    	for (DynamicContext thread : threads) {
			if (thread.top().getId() < 0) {
				// this thread recursively invokes itself (directly or indirectly)
				recursiveThreads.add(thread);

				DynamicContext rootOfTheRecursion = thread.copy();
				rootOfTheRecursion.pop();
				result.put(rootOfTheRecursion, -1);
				result.remove(thread);
				refinedThreads.add(rootOfTheRecursion);
			}
    	}
    	threads.removeAll(recursiveThreads);
    	threads.addAll(refinedThreads);

    	return result;
    }

    private boolean isInALoop(DynamicContext thread) {
    	LinkedList<Context> w = new LinkedList<Context>();
		HashSet<Context> visited = new HashSet<Context>();
		w.add(thread);
		visited.add(thread);

		while(!w.isEmpty()) {
			Context next = w.poll();
			SDGNode node = folded.map(next.getNode());

			if (node.getId() < 0) { // loop: return true
				return true;
			}

			for (SDGEdge e : folded.incomingEdgesOf(node)) {
				SDGNode source = e.getSource();

				if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
			        Context newContext = next.level(source);

					if (visited.add(newContext)) {
						w.add(newContext);
					}

				} else if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.FORK) {
					// the call site calling the procedure to descend into
			        SDGNode mapped = conMan.map(source);
			        Context newContext = null;

			        // if the corresponding call site is recursive,
			        // clone context and set 'source' as new node
			        // else ascend to the calling procedure
			        if (conMan.isFolded(source) && next.top() == mapped) {
			        	newContext = next.level(source);

			        } else {
			        	newContext = next.ascend(source, new SDGNodeTuple(source, null));
			        }

			        if (visited.add(newContext)) {
			        	w.add(newContext);
			        }
				}
			}
		}

		return false;
    }


    /** main() for debugging
     *
     * @param args
     * @throws Exception
     */
    public static void main (String[] args) throws Exception {
        args = new String[1];
//        args[0] = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/tests.Mantel00Page10.pdg";
//        args[0] = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/tests.ThreadSpawning.pdg";
//        args[0] = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/tests.RecursiveThread.pdg";

        PDGIterator iter = PDGs.getPDGs();
		while (iter.hasNext()) {
			SDG sdg = iter.next();
            CFG tcfg = ICFGBuilder.extractICFG(sdg);
            FoldedCFG folded = GraphFolder.foldIntraproceduralSCC(tcfg);
			System.out.println("*******************\n"+sdg.getName());
            ThreadAllocation t = new ThreadAllocation(sdg, tcfg, folded);
            t.compute();
//			System.out.println("-------------------");
//			for (SDGEdge e : t.tcfg.edgeSet()) {
//				if (e.getKind() == SDGEdge.Kind.FORK) {
//					System.out.println("thread entry "+e+":");
//					Set<LinkedList<SDGNode> > rawContexts = t.buildContextsOf(e.getSource());
//					for (LinkedList<SDGNode> raw : rawContexts) {
//						raw.addFirst(e.getSource());
//		    			DynamicContext con = new DynamicContext(raw, e.getTarget(), -1); // assign a dummy thread ID
//		    			System.out.println(con);
//		    		}
//				}
//			}
            System.out.println();
		}
    }
}
