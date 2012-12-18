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
package edu.kit.joana.deprecated.jsdg.sdg.interference;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;


/**
 *
 * @author giffhorn
 */
public class JoinAnalysis {
    public static boolean DEBUG = false;

    private Collection<SDGNode> allocs; // the thread allocations
    private HashMap<SDGNode, Set<SDGNode>> callJoin_alloc; // maps calls of Thread::join to thread allocations
    private HashMap<SDGNode, Set<SDGNode>> alloc_callJoin; // maps thread allocations to calls of Thread::join
    private HashMap<SDGNode, Set<SDGNode>> fork_alloc; // maps forks to thread allocations
    private HashMap<SDGNode, Set<SDGNode>> alloc_fork; // maps thread allocations to forks
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
        if (DEBUG) System.out.println("Resulting threads information:\n"+ti);
    }

    private void computeMaps() {
        joinAllocations();
        forkAllocations();
    }

    private void joinAllocations() {
        // search calls of Thread::join
        LinkedList<SDGNode> joinCalls = new LinkedList<SDGNode>();

        for (SDGNode n : ipdg.vertexSet()) {
            if (n.getKind() == SDGNode.Kind.ENTRY && n.getLabel().equals("java.lang.Thread.join()")) {
                for (SDGEdge e : ipdg.incomingEdgesOf(n)) {
                    if (e.getKind() == SDGEdge.Kind.CALL) {
                        joinCalls.add(e.getSource());
                    }
                }
            }
        }

        // search this-pointer parameters of these calls
        HashMap<SDGNode, SDGNode> thisJoin = new HashMap<SDGNode, SDGNode>();

        for (SDGNode n : joinCalls) {
            for (SDGEdge e : ipdg.outgoingEdgesOf(n)) {
                if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR && e.getTarget().getLabel().equals("this")) {
                    thisJoin.put(e.getTarget(), n);
                }
            }
        }

        for (SDGNode n : thisJoin.keySet()) {
            LinkedList<SDGNode> slice = dataSlice(n);

            // update alloc_callJoin
            for (SDGNode alloc : slice) {
                Set<SDGNode> calls = alloc_callJoin.get(alloc);
                if (calls == null) {
                    calls = new HashSet<SDGNode>();
                    alloc_callJoin.put(alloc, calls);
                }
                calls.add(thisJoin.get(n));
            }

            // update callJoin_alloc
            Set<SDGNode> allocs = callJoin_alloc.get(thisJoin.get(n));
            if (allocs == null) {
                allocs = new HashSet<SDGNode>();
                callJoin_alloc.put(thisJoin.get(n), allocs);
            }
            allocs.addAll(slice);

            if (DEBUG) System.out.println("allocations "+slice+" joined at "+thisJoin.get(n));
            if (DEBUG) System.out.println(thisJoin.get(n)+" joins thread allocations: "+allocs);
        }
    }

    private void forkAllocations() {
        // search the this-pointer parameter of each fork
        HashMap<SDGNode, SDGNode> thisFork = new HashMap<SDGNode, SDGNode>();

        for (SDGNode fork : ti.getAllForks()) {
//        	if (fork.getId() < 0 ) continue; // recursive thread, has no univocal join
            for (SDGEdge e : ipdg.outgoingEdgesOf(fork)) {
                if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR && e.getTarget().getLabel().equals("this")) {
                    thisFork.put(e.getTarget(), fork);
                }
            }
        }

        // use data edu.kit.joana.deprecated.jsdg.slicing for finding thread allocations
        for (SDGNode n : thisFork.keySet()) {
            LinkedList<SDGNode> slice = dataSlice(n);

            // update alloc_fork
            for (SDGNode alloc : slice) {
                Set<SDGNode> calls = alloc_fork.get(alloc);
                if (calls == null) {
                    calls = new HashSet<SDGNode>();
                    alloc_fork.put(alloc, calls);
                }
                calls.add(thisFork.get(n));
            }

            // update fork_alloc
            Set<SDGNode> allocs = fork_alloc.get(thisFork.get(n));
            if (allocs == null) {
                allocs = new HashSet<SDGNode>();
                fork_alloc.put(thisFork.get(n), allocs);
            }
            allocs.addAll(slice);

            if (DEBUG) System.out.println("allocations "+slice+" forked at "+thisFork.get(n));
            if (DEBUG) System.out.println(thisFork.get(n)+" forks thread allocations: "+allocs);
        }
    }

    private LinkedList<SDGNode> dataSlice(SDGNode thiS) {
        LinkedList<SDGNode> w1 = new LinkedList<SDGNode>();
        LinkedList<SDGNode> w2 = new LinkedList<SDGNode>();
        HashSet<SDGNode> marked = new HashSet<SDGNode>();
        LinkedList<SDGNode> result = new LinkedList<SDGNode>();

        w1.clear();
        w2.clear();
        marked.clear();
        w1.add(thiS);
        marked.add(thiS);

        while (!w1.isEmpty()) {
            SDGNode next = w1.poll();

            if (allocs.contains(next)) {
                result.add(next);
            }

            for (SDGEdge e : ipdg.incomingEdgesOf(next)) {
                if (!(e.getKind() == SDGEdge.Kind.DATA_DEP
                		|| e.getKind() == SDGEdge.Kind.DATA_HEAP
                		|| e.getKind() == SDGEdge.Kind.DATA_ALIAS
                        || e.getKind() == SDGEdge.Kind.PARAMETER_IN
                        || e.getKind() == SDGEdge.Kind.PARAMETER_OUT)) continue;

                if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                    if (marked.add(e.getSource())) {
                        w2.add(e.getSource());
                    }

                } else {
                    if (marked.add(e.getSource())) {
                        w1.add(e.getSource());
                    }
                }
            }
        }

        while (!w2.isEmpty()) {
            SDGNode next = w2.poll();

            if (allocs.contains(next)) {
                result.add(next);
            }

            for (SDGEdge e : ipdg.incomingEdgesOf(next)) {
                if (!(e.getKind() == SDGEdge.Kind.DATA_DEP
                		|| e.getKind() == SDGEdge.Kind.DATA_HEAP
                		|| e.getKind() == SDGEdge.Kind.DATA_ALIAS
                        || e.getKind() == SDGEdge.Kind.PARAMETER_OUT)) continue;

                if (marked.add(e.getSource())) {
                    w2.add(e.getSource());
                }
            }
        }

        return result;
    }

    private void detectMustJoining() {
        for (SDGNode alloc : allocs) {
        	// 1. the thread has to be unique
        	Set<SDGNode> forks = alloc_fork.get(alloc);
        	if (forks == null || forks.size() != 1) continue;

        	// 2. the thread object has to be unique, too
        	if (fork_alloc.get(forks.iterator().next()).size() != 1) continue;

        	// 3. the thread must not be dynamic
        	ThreadInstance thread = ti.getThread(alloc_fork.get(alloc).iterator().next());
        	if (thread.isDynamic()) continue;

            // 4. the joining has to be unique
        	Set<SDGNode> joins = alloc_callJoin.get(alloc);
            if (joins == null || joins.size() != 1) continue;

            // 5. the thread object has to be unique, too
            if (callJoin_alloc.get(joins.iterator().next()).size() != 1) continue;

            // we have found a must-joining!
            thread.setJoin(alloc_callJoin.get(alloc).iterator().next());
        }
    }
}
