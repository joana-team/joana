/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.interference;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;


/**
 *
 * -- Created on October 18, 2006
 *
 * @author  Dennis Giffhorn
 */
public class ThreadStartDuplicator {
    public static final int CHRISTIAN = 1;
    public static final int JUERGEN = 2;

    public static boolean DEBUG = false;

    private SDG sdg;
    /** A CFG. */
    private CFG icfg;

    private List<SDGNode> runEntries;
    private HashMap<SDGNode, List<SDGNode>> alloc_run; // maps thread allocations to thread entries
    private HashMap<SDGNode, List<SDGNode>> callStart_alloc; // maps thread allocations to calls of Thread::start
    private HashMap<SDGNode, HashSet<SDGNode>> callStart_run; // maps calls of Thread::start to thread entries
    private HashMap<SDGNode, HashSet<SDGNode>> run_callStart; // maps thread entries to calls of Thread::start

    private String createObject;
    private String runName;
    private String threadStart;

    /** Creates a new instance of ThreadAllocation
     * @param g  A SDG.
     */
    public ThreadStartDuplicator(SDG g, int type) {
    	sdg = g;

        if (type == CHRISTIAN) {
            christian();
        } else if (type == JUERGEN) {
            juergen();
        } else {
            throw new RuntimeException("not supported");
        }

        // build the threaded ICFG
        icfg = ICFGBuilder.extractICFG(sdg);
    }

    public Collection<SDGNode> getAllocs() {
    	return alloc_run.keySet();
    }

    private void christian() {
        createObject = "new class ";
        runName = ".run()V";
        threadStart = "java.lang.Thread.start()V";
    }

    private void juergen() {
        createObject = "new ";
        runName = ".run()";
        threadStart = "java.lang.Thread.start()";
    }

    /** Executes the thread allocation analysis.
     */
    public void dupe() {
        // determine all existing thread run methods
        // the List will contain their entry nodes
        runEntries = allRunMethods();
        if (DEBUG) System.out.println("run-method entries                : "+runEntries);

        // determine thread allocation sites
        HashMap<SDGNode, List<SDGNode>> run_alloc = threadAllocationSites(runEntries);

        // reverse mapping
        alloc_run = mapAllocToRun(run_alloc);
        if (DEBUG) System.out.println("run-entries -> thread allocations : "+run_alloc);
        if (DEBUG) System.out.println("thread allocations -> run-entries : "+alloc_run);

        // compute Thread::start invocations for each thread allocation site
        computeForkCalls();

        // map calls of Thread::start to thread entries
        callStart_run = mapCallsToRun();
        run_callStart = mapRunToCalls();
        if (DEBUG) System.out.println("Thread::start calls -> run-entries: "+callStart_run);
        if (DEBUG) System.out.println("run-entries -> Thread::start calls: "+run_callStart);

        // clone java.lang.Thread::start()
        cloneThreadStarts();
    }

    /** Computes the entries of all run methods in the program.
     * @return A list with the found entry nodes.
     */
    private List<SDGNode> allRunMethods() {
        List<SDGNode> result = new LinkedList<SDGNode>();

        // traverse all fork edges to find the entries
        for (SDGEdge fork : icfg.edgeSet()) {
            if (fork.getKind() != SDGEdge.Kind.FORK) continue;

            if (!result.contains(fork.getTarget())) {
                result.add(fork.getTarget());
            }
        }

        return result;
    }

    /** Computes all thread allocation nodes.
     * It uses a list with run-method entry nodes to determine the allocation sites.
     * @param arm  The list with run method entry nodes.
     * @return     A map run-method -> allocation sites
     */
    private HashMap<SDGNode, List<SDGNode>> threadAllocationSites(List<SDGNode> arm) {
        HashMap<SDGNode, List<SDGNode>> result = new HashMap<SDGNode, List<SDGNode>>();

        // determine all classes that implement class Thread
        // the indices of threadClass fit to the indices of arm
        List<String> threadClasses = allThreadClasses(arm);

        // iterate over all nodes
        for (SDGNode n : icfg.vertexSet()) {
            // only look for allocation nodes
            if(!n.getLabel().contains(createObject)) continue;

            // now check if the nodes allocates a subclass-object of class Thread
            for (int i = 0; i < threadClasses.size(); i++) {
                String name = threadClasses.get(i);

                if(n.getLabel().endsWith(createObject+name)) {
                    // i is identical index for threadClasses and arm
                    List<SDGNode> runs = result.get(arm.get(i));

                    // update the result map
                    if (runs == null) {
                        runs = new LinkedList<SDGNode>();
                    }

                    runs.add(n);
                    result.put(arm.get(i), runs);
                }
            }
        }

        return result;
    }

    /** Computes the name of all classes that implement class Thread.
     * @param   The entry nodes of all run methods.
     * @return  A list with class names.
     */
    private List<String> allThreadClasses(List<SDGNode> arm) {
        List<String> result = new LinkedList<String>();

        // iterate over the entry nodes
        for (SDGNode n : arm) {
            String str = n.getLabel();

            // the class name is part of the label
            int i = str.indexOf(runName);
            result.add(n.getLabel().substring(0, i));
        }

        return result;
    }

    /** Construct a mapping allocation site -> run-methods from a map <br>
     * run-method -> allocation sites.
     * @param run_alloc  A map run-method -> allocation sites
     * @return           A corresponding map allocation site -> run-methods.
     */
    private HashMap<SDGNode, List<SDGNode>> mapAllocToRun(HashMap<SDGNode, List<SDGNode>> run_alloc) {

        HashMap<SDGNode, List<SDGNode>> alloc_run = new HashMap<SDGNode, List<SDGNode>>();

        for (SDGNode key : run_alloc.keySet()) {
            List<SDGNode> alloc = run_alloc.get(key);

            for (SDGNode all : alloc) {
                List<SDGNode> run = alloc_run.get(all);

                if (run == null) {
                    run = new LinkedList<SDGNode>();
                }
                run.add(key);

                alloc_run.put(all, run);
            }
        }

        return alloc_run;
    }

    // map calls of Thread::start to thread entries
    private HashMap<SDGNode, HashSet<SDGNode>> mapCallsToRun() {
        HashMap<SDGNode, HashSet<SDGNode>> s = new HashMap<SDGNode, HashSet<SDGNode>>();

        for (SDGNode call : callStart_alloc.keySet()) {
            HashSet<SDGNode> r = new HashSet<SDGNode>();

            for (SDGNode alloc : callStart_alloc.get(call)) {
                for (SDGNode run : alloc_run.get(alloc)) {
                    r.add(run);
                }
            }

            s.put(call, r);
        }
        return s;
    }

    private HashMap<SDGNode, HashSet<SDGNode>> mapRunToCalls() {
        HashMap<SDGNode, HashSet<SDGNode>> s = new HashMap<SDGNode, HashSet<SDGNode>>();

        for (SDGNode call : callStart_run.keySet()) {
            HashSet<SDGNode> runs = callStart_run.get(call);

            for (SDGNode run : runs) {
                HashSet<SDGNode> calls = s.get(run);

                if (calls == null) {
                    calls = new HashSet<SDGNode>();
                }

                calls.add(call);
                s.put(run, calls);
            }
        }

        return s;
    }

    private void computeForkCalls() {
        // search calls of start and join
        LinkedList<SDGNode> startCalls = new LinkedList<SDGNode>();

        for (SDGNode n : sdg.vertexSet()) {
            if (n.getKind() == SDGNode.Kind.ENTRY && n.getLabel().equals(threadStart)) {
                for (SDGEdge e : sdg.incomingEdgesOf(n)) {
                    if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.FORK) {
                       startCalls.add(e.getSource());
                    }
                }
            }
        }

        // search this-pointer parameters of these calls
        HashMap<SDGNode, SDGNode> thisStart = new HashMap<SDGNode, SDGNode>();

        for (SDGNode n : startCalls) {
            for (SDGEdge e : sdg.outgoingEdgesOf(n)) {
                if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR && e.getTarget().getLabel().equals("this")) {
                    thisStart.put(e.getTarget(), n);
                }
            }
        }

        // a backward data slice searches for the origins of the this-parameters
        callStart_alloc = new HashMap<SDGNode, List<SDGNode>>();

        for (SDGNode n : thisStart.keySet()) {
            LinkedList<SDGNode> slice = dataSlice(n);
            callStart_alloc.put(thisStart.get(n), slice);
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

            if (alloc_run.containsKey(next)) {
                result.add(next);
            }

            for (SDGEdge e : sdg.incomingEdgesOf(next)) {
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

            if (alloc_run.containsKey(next)) {
                result.add(next);
            }

            for (SDGEdge e : sdg.incomingEdgesOf(next)) {
                if (!(e.getKind() == SDGEdge.Kind.DATA_DEP
                		|| e.getKind() == SDGEdge.Kind.DATA_HEAP
                		|| e.getKind() == SDGEdge.Kind.DATA_ALIAS
                        || e.getKind() == SDGEdge.Kind.PARAMETER_IN)) continue;

                if (marked.add(e.getSource())) {
                    w2.add(e.getSource());
                }
            }
        }

        return result;
    }

    private void cloneThreadStarts() {
        /* collect the nodes and edges of Thread::start */
//        SDGNode n = ti.getThreadFork(1);
    	if (runEntries.size() < 1) return;

    	SDGNode run = runEntries.get(0);
    	SDGNode n = null;
    	for (SDGEdge e : icfg.getIncomingEdgesOfKind(run, SDGEdge.Kind.FORK)) {
    		n = e.getSource();
    		break;
    	}
        if (n == null) return;

        int proc = n.getProc();
        int lastID = sdg.lastId() +1;
        int lastProc = sdg.lastProc() +1;

        HashSet<SDGNode> startNodes = new HashSet<SDGNode>();
        HashSet<SDGEdge> startEdges = new HashSet<SDGEdge>();

        for (SDGNode m : sdg.vertexSet()) {
            if (m.getProc() == proc) {
                startNodes.add(m);
            }
        }

        for (SDGNode m : startNodes) {
            startEdges.addAll(sdg.incomingEdgesOf(m));
            startEdges.addAll(sdg.outgoingEdgesOf(m));
        }

        /* clone Thread::start */
        // insert the new nodes - we need one instance of Thread::start for every call site
        Collection<SDGNode> calls = callStart_run.keySet();
//        Collection<SDGNode> calls = ti.getAllCalls();
        HashMap<SDGNode, Set<SDGNode>> call_start = new HashMap<SDGNode, Set<SDGNode>>();
        Iterator<SDGNode> iter = calls.iterator();
        SDGNode firstCall = iter.next();
        call_start.put(firstCall, startNodes);

        while (iter.hasNext()) {
            SDGNode call = iter.next();
            HashSet<SDGNode> s = new HashSet<SDGNode>();
            HashMap<SDGNode, SDGNode> original_clone = new HashMap<SDGNode, SDGNode>();

            for (SDGNode m : startNodes) {
                SDGNode mClone = m.clone(lastID, lastProc);
                lastID++;
                s.add(mClone);
                original_clone.put(m, mClone);
                sdg.addVertex(mClone);
            }
            call_start.put(call, s);
            lastProc++;

            // add edges
            Set<SDGNode> callSite = callSite(call, sdg);
            Set<SDGNode> runs = calledRuns(call, sdg);


            for (SDGEdge e : startEdges) {
                if (e.getKind().isIntraproceduralEdge()) {
                    SDGNode sourceClone = original_clone.get(e.getSource());
                    SDGNode targetClone = original_clone.get(e.getTarget());
                    sdg.addEdge(e.getKind().newEdge(sourceClone, targetClone));

                } else {
                    boolean add = true;

                    // interprocedural edge - check if it connects Thread::start to the right caller or run method
                    SDGNode sourceClone = original_clone.get(e.getSource());
                    if (sourceClone == null) {
                        sourceClone = e.getSource();

                        if ((e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN)
                                && !callSite.contains(e.getSource())) {

                           add = false;

                        } else if (e.getKind() == SDGEdge.Kind.FORK_OUT && !runs.contains(e.getSource())) {
                            add = false;
                        }
                    }

                    SDGNode targetClone = original_clone.get(e.getTarget());
                    if (targetClone == null) {
                        targetClone = e.getTarget();

                        if ((e.getKind() == SDGEdge.Kind.PARAMETER_OUT || e.getKind() == SDGEdge.Kind.RETURN)
                                && !callSite.contains(e.getTarget())) {

                            add = false;

                        } else if ((e.getKind() == SDGEdge.Kind.FORK_IN || e.getKind() == SDGEdge.Kind.FORK)
                                && !runs.contains(e.getTarget())) {

                            add = false;
                        }
                    }

                    if (add) sdg.addEdge(e.getKind().newEdge(sourceClone, targetClone));
                }
            }
        }

        // remove the spurious interprocedural edges from the original method
        Set<SDGNode> callSite = callSite(firstCall, sdg);
        Set<SDGNode> runs = calledRuns(firstCall, sdg);
        int p = startNodes.iterator().next().getProc();

        for (SDGEdge e : startEdges) {
            if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN) {
                if (e.getSource().getProc() != p && !callSite.contains(e.getSource())) {
                    sdg.removeEdge(e);
                }

            } else if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT || e.getKind() == SDGEdge.Kind.RETURN) {
                if (e.getTarget().getProc() != p && !callSite.contains(e.getTarget())) {
                    sdg.removeEdge(e);
                }

            } else if (e.getKind() == SDGEdge.Kind.FORK || e.getKind() == SDGEdge.Kind.FORK_IN) {
                if (!runs.contains(e.getTarget())) {
                    sdg.removeEdge(e);
                }

            } else if (e.getKind() == SDGEdge.Kind.FORK_OUT) {
                if (!runs.contains(e.getSource())) {
                    sdg.removeEdge(e);
                }
            }
        }
    }

    private Set<SDGNode> callSite(SDGNode call, SDG sdg) {
        HashSet<SDGNode> s = new HashSet<SDGNode>();
        LinkedList<SDGNode> wl = new LinkedList<SDGNode>();

        wl.add(call);
        s.add(call);

        while (!wl.isEmpty()) {
            SDGNode next = wl.poll();
            for (SDGEdge e : sdg.outgoingEdgesOf(next)) {
                if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR && s.add(e.getTarget())) {
                    wl.add(e.getTarget());
                }
            }
        }

        return s;
    }

    private Set<SDGNode> calledRuns(SDGNode call, SDG sdg) {
        HashSet<SDGNode> s = new HashSet<SDGNode>();
        Collection<SDGNode> es = callStart_run.get(call);//ti.getEntriesFor(call);
        LinkedList<SDGNode> wl = new LinkedList<SDGNode>();

        wl.addAll(es);
        s.addAll(es);

        while (!wl.isEmpty()) {
            SDGNode next = wl.poll();
            for (SDGEdge e : sdg.outgoingEdgesOf(next)) {
                if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR && s.add(e.getTarget())) {
                    wl.add(e.getTarget());
                }
            }
        }

        return s;
    }
}
