/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.old;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.TruncatedNonSameLevelChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGForward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.ContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.StaticContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;



/**
 * This class realizes Krinke's optimized Algorithm for threaded interprocedural slicing.
 * Uses thread regions.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class Slicer implements edu.kit.joana.ifc.sdg.graph.slicer.Slicer {
	public static long elems = 0L;
	private static boolean TIME_TRAVELS = true;

    /** the corresponding interprocedural control flow graph */
    protected CFG icfg;
    /** The graph to be sliced. */
    protected SDG sdg;
    /** The folded ICFG. */
    protected FoldedCFG foldedIcfg;
    protected ContextManager conMan;

    /** A reachability checker for control flow graphs. */
    protected ReachabilityChecker reachable;
    /** the call site of the threads of the program to slice */
    protected SDGNode threadCall;
    /** the call site for the main() thread **/
    protected SDGNode mainCall;
    /** the initial states of the threads */
    protected States initStates;

    /** The intrathreadual slicer. */
    protected ContextSlicer slicer;
    protected DummyContextSlicer dummySlicer;

    protected Context2PhaseSlicer c2pSlicer;
    /** A summary slicer. */
    protected SummarySlicer summarySlicer;
    /** A truncated non-same-level chopper. */
    protected TruncatedNonSameLevelChopper truncated;

    /** A map for caching state tuple changes for slicing criteria contexts. */
    protected InterferenceMap iMap;

    protected MHPAnalysis mhp;
    protected Map<SDGNode, Collection<SDGNode>> reachMap;

    /** DEBUG information*/
    public static long eingefuegt = 0l;
    public static int aufruf = 0;
    public static int chopaufruf = 0;
    public static int summaryaufruf = 0;
    public long eingefuegt() {
        return eingefuegt;
    }
    public int optimierung = 0;
    public int opt() {
        return optimierung;
    }
    public int opt1Sliced, opt1NotSliced = 0;
    public String opt1() {
        return "(slicer: "+opt1Sliced + ", map: "+opt1NotSliced+")";
    }
    public int noReach, reach, reducedReach = 0;
    public String opt2() {
        return "(no reach: "+noReach+", CFG: "+reach + ", reduced CFG: "+reducedReach+")";
    }

    /**
     * Creates a new instance of this slicer.
     *
     * @param graph A threaded interprocedural program dependencies graph that shall be sliced.
     *              It has to contain control flow edges.
     */
    public Slicer(SDG graph) {
        setGraph(graph);
    }

    /**
     * Initializes the fields of the slicer.
     *
     * @param graph A threaded interprocedural program dependencies graph that shall be sliced.
     *              It has to contain control flow edges.
     */
    public void setGraph(SDG graph) {
        // init context-using 2-phase slicer
        sdg = graph;
        conMan = StaticContextManager.create(sdg);

        // build the threaded ICFG
        icfg = ICFGBuilder.extractICFG(sdg);

        // fold ICFG with Krinke's two-pass folding algorithm
        foldedIcfg = GraphFolder.twoPassFolding(icfg);

        c2pSlicer = new Context2PhaseSlicer(sdg, conMan);

        // a simple 2-phase slicer
        // the summary slicer shall not traverse interference edges
        summarySlicer = new SummarySlicer(sdg);

        // a truncated non-same-level chopper
        truncated = new TruncatedNonSameLevelChopper(sdg);

        mhp = PreciseMHPAnalysis.analyze(sdg);

        // init the intrathredual slicer
        slicer = new ContextSlicer(sdg, conMan, mhp);
        dummySlicer = new DummyContextSlicer(sdg, conMan, mhp);

        // a reachability checker for ICFGs
        reachable = new ReachabilityChecker(foldedIcfg);

        // the initial states tuple
        initStates = initStates();

        // init interference map
        iMap = new InterferenceMap(initStates);

        reachMap = interThreadReach(icfg);
    }

    public SDG getGraph() {
        return sdg;
    }

    public Collection<SDGNode> slice(SDGNode criterion) {
    	return slice(Collections.singleton(criterion));
    }

    /**
     * The slicing algorithm.
     *
     * @param criterion  Contains a node and a thread ID for which the program
     *                   shall be sliced.
     */
    public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
        // the slice
        HashSet<SDGNode> slice = new HashSet<SDGNode>();
        // all visited worklist elements
        HashSet<WorklistElement> marked = new HashSet<WorklistElement>();
        // all appeared state tuples for every appeared context
        HashMap<Context, List<States>> markedStates = new HashMap<Context, List<States>>();
        // all already sequentially sliced contexts
        HashSet<Context> markedSequentialSlice = new HashSet<Context>();

        // two worklists for the algorithm
        LinkedList<WorklistElement> worklist_1 = initialWorklist(criteria, markedStates);
        LinkedList<WorklistElement> worklist_2 = new LinkedList<WorklistElement>();

        for(WorklistElement o : worklist_1){
            marked.add(o);
            mark(o, markedStates);
            elems++;
        }

        /*** slicing.. ***/
        // sequential slice for the contexts of the slicing criteria
        Collection<SDGNode> interferingNodes = summarySlicer.slice(criteria, slice);

        if (interferingNodes.isEmpty()) {
            return slice;

        } else {
            Collection<SDGNode> chop = truncated.chop(interferingNodes, criteria);

            while(!worklist_1.isEmpty()){
                WorklistElement next = worklist_1.poll();

                // compute explicitly context-sensitive slice on chop result
                Collection<WorklistElement> with_interference = slicer.subGraphSlice(next, chop);

                // processing interference edges
                processInterthreadualEdges(with_interference, worklist_2, marked, markedStates);
            }

            // iterated sequential slicing after switching threads
            while(!worklist_2.isEmpty()){
                WorklistElement next = worklist_2.poll();
                // slice for context in element 'next'
                // if it was not already sliced
                // retrieve resulting interfering contexts from interference map
                LinkedList<WorklistElement> with_interference = iMap.get(next);

                if (with_interference == null) {
                    States dummyStates = initStates.clone();
                    WorklistElement dummyCriterium = new WorklistElement(next.getContext(), dummyStates);
                    Collection<WorklistElement> dummyInterference = dummySlicer.slice(dummyCriterium, slice);
                    iMap.put(dummyCriterium, dummyInterference);
                    with_interference = iMap.get(next);

                } else {
                    if (markedSequentialSlice.add(next.getContext())) {//opt1Sliced++;
                        Set<Context> criterion = Collections.singleton(next.getContext());

                        Collection<SDGNode> s = c2pSlicer.contextSlice(criterion);
                        slice.addAll(s);

                    }
                }

                // processing interference edges
                processInterthreadualEdges(with_interference, worklist_2, marked, markedStates);
            }

            return slice;
        }
    }

    /**
     * Handles the found interference edges.
     * Every valid interferenc is traversed and the resulting worklist elements are
     * inserted in a given worklist.
     *
     * @param with_interference  A list of worklist elements with incoming interference edges.
     * @param worklist           A worklist to insert the new worklist elements into.
     * @param marked             Contains all previously occurred worklist elements.
     * @param markedStates       Contains for every occurred context their occurred state tuples.
     */
    protected void processInterthreadualEdges(
            Collection<WorklistElement> with_interference, LinkedList<WorklistElement> worklist,
            HashSet<WorklistElement> marked, HashMap<Context, List<States>> markedStates) {

        // iterate over the worklist elements
        for(WorklistElement w : with_interference){
            SDGNode n = w.getNode();

            // handle all incoming interference edges
            for(SDGEdge e : sdg.incomingEdgesOf(n)){
                if(isInterfering(e)){
                    // the edge's source
                    SDGNode source = e.getSource();

                    // the threads the source belongs to
                    int[] reached_threads  = source.getThreadNumbers();

                    // handling virtual code doubling
                    for (int thread : reached_threads) {

                        // only process real thread changes
                        if (thread != w.getThread()) {
                            // get all valid context for 'source'
                            Collection<Context> reached = reachingContexts(source, thread, w);

                            // create new worklist elements
                            LinkedList<WorklistElement> newElems = createWorklistElements(reached, w, marked);

                            // update worklist
                            updateWorklist(worklist, newElems, markedStates);
                        }
                    }
                }
            }
        }
    }

    /**
     * Computes all valid contexts according to an interference edge traversion.
     * It creates all possible contexts and performs a reaching analysis with
     * regard to a given state tuple.
     *
     * @param source        The node the interference edge is traversed to.
     * @param thread        The node's thread..
     * @param w             The worklist element representing the point where the old thread is left.
     *                      It contains the state tuple for the reaching analysis.
     */
    protected Collection<Context> reachingContexts(SDGNode source, int thread, WorklistElement w) {
        // retrieve all possible contexts for source in its thread
        Collection<Context> contextList = contextsForNode(source, thread);
        Collection<Context> reached = new LinkedList<Context>();
        ThreadRegion region = mhp.getThreadRegion(source, thread);
        Context target = w.getStates().state(region.getID());
        boolean init = target.equals(initStates.state(thread));

        if (init || mhp.isDynamic(thread) || !TIME_TRAVELS) {
            // if the thread was not visited yet, all contexts are valid
            reached = contextList;

        } else {
            if (thread != target.getThread()) {
                // if the contexts belong to different threads, use the
                // reaching relation for thread regions
                // this relation is context-insensitive
                if (reachesRegions(source, thread, target.getNode(), target.getThread())) {
                    reached = contextList;
                }

            } else {
                // for every context of source compute a reaching analysis
                for (Context s : contextList) {
                    // if reachable, add context to reached list
                    if (reaches(s, target)) {
                        reached.add(s);
                    }
                }
            }
        }

        return reached;
    }

    /** Creates new worklist elements for a set of contexts.
     * @param reached         A set of contexts for one node.
     * @param old            The old worklist element.
     * @param source region  The thread region of the contexts' node.
     * @param marked         The already marked worklist elements.
     * @return               A list of new worklist elements.
     */
    protected LinkedList<WorklistElement> createWorklistElements(Collection<Context> reached,
            WorklistElement old, HashSet<WorklistElement> marked) {

        LinkedList<WorklistElement> newElems = new LinkedList<WorklistElement>();

        for(Context context : reached){
            // build new worklist elements:
            States newStates = old.getStates().clone();

            for (int i = 0; i < initStates.size(); i++) {
                if (!mhp.isParallel(context.getNode(), context.getThread(), i)) {
                    newStates.set(i, context);
                }
            }

            // new worklist element
            WorklistElement newW = new WorklistElement(context, newStates);

            // add it to the result list
            if(marked.add(newW)){
                newElems.add(newW);
            }
        }

        return newElems;
    }

    /** Updates a worklist with a list of worklist elements.
     * Checks for restrictive state tuples.
     *
     * @param worklist      A worklist.
     * @param newElems      A list of worklist elements.
     * @param markedStates  The marked state tuples for every interference edge traversal.
     */
    protected void updateWorklist(LinkedList<WorklistElement> worklist, LinkedList<WorklistElement> newElems,
            HashMap<Context, List<States>> markedStates) {

        while (!newElems.isEmpty()) {
            WorklistElement el = newElems.poll();

            // checks if el is restrictive state tuple according to the marked element and
            // the other elements in the list
            if (!(isRedundant(el, newElems) || isRedundant(el, markedStates))) {
                mark(el, markedStates);
                worklist.add(el);
                elems++;
            }
        }
    }

    /** Returns an initial state tuple.
     * All threads are at their initial states.
     *
     * @return  An initial state tuple.
     */
    protected States initStates() {
    	LinkedList<Context> exitContexts = new LinkedList<Context>();
        LinkedList<Context> states = new LinkedList<Context>();

        for (int i = 0; i < sdg.getThreadsInfo().getNumberOfThreads(); i++) {
            Context con = conMan.getContextsOf(mhp.getThreadExit(i), i).iterator().next(); // es kann nur einen geben
            exitContexts.addLast(con);
        }

        for (ThreadRegion r : mhp.getThreadRegions()) {
        	int thread = r.getThread();
            states.addLast(exitContexts.get(thread)); // regionen sind anhand ihrer ID aufsteigend angeordnet
        }

        return new States(states);
    }

    /**
     * Creates the initial worklist for the slicing algorithm.
     * It consists of a LinkedList containing one ThreadedWorklistElement for
     * every context which can reach the given starting criterion.
     *
     * @param data.node  The starting criterion.
     * @return  A LinkedList, maybe empty.
     */
    protected LinkedList<WorklistElement> initialWorklist(Collection<SDGNode> criteria,
    		HashMap<Context, List<States>> markedStates) {

        LinkedList<WorklistElement> s = new LinkedList<WorklistElement>();

        for (SDGNode node : criteria) {
            int[] threads = node.getThreadNumbers();

            for (int thread : threads) {
                Collection<Context> contexts = contextsForNode(node, thread);
                LinkedList<WorklistElement> inits = initialWorklistElements(contexts, node ,thread);

                while (!inits.isEmpty()) {
                    WorklistElement el = inits.poll();
                    mark(el, markedStates);
                    s.addFirst(el);
                }
            }
        }

        return s;
    }

    /** Computes a list of initial worklist elements for a given list of contexts.
     * @param contexts  The contexts.
     * @param node      The node of the contexts.
     * @param thread    The thread of the contexts.
     * @return          The worklist elements list.
     */
    protected LinkedList<WorklistElement> initialWorklistElements(Collection<Context> contexts, SDGNode node, int thread) {
        // construct all initial ThreadedWorklistElement objects
        LinkedList<WorklistElement> inits = new LinkedList<WorklistElement>();

        for (Context con : contexts) {
            States newStates = initStates.clone();

            for (int i = 0; i < initStates.size(); i++) {
                if (!mhp.isParallel(con.getNode(), con.getThread(), i)) {
                    newStates.set(i, con);
                }
            }

            WorklistElement w = new WorklistElement(con, newStates);
            inits.add(w);
        }

        return inits;
    }

    /** Marks a given worklist element.
     * @param w       The element.
     * @param marked  The marking structure.
     */
    protected void mark(WorklistElement w, HashMap<Context, List<States>> marked) {
        // get the place for the context
        List<States> s = marked.get(w.getContext());

        if (s == null) {
            s = new LinkedList<States>();
        }

        // add the state tuple
        s.add(w.getStates());
        marked.put(w.getContext(), s);
    }

    /** Checks if the given worklist element is restrictive according to a list of states.
     * @param check   The element to check.
     * @param marked  Contains the state tuple list for the context of check.
     */
    protected boolean isRedundant(WorklistElement check, HashMap<Context, List<States>> marked) {
        List<States> oldStates = marked.get(check.getContext());

        if (oldStates == null) return false;

        // iterate over all states
        for (States old : oldStates) {
            // check for restrictiveness
            if (isRestrictive(check.getStates(), old)){
                return true;
            }
        }

        return false;
    }

    /** Checks if the given worklist element is restrictive according to a list of worklist elements.
     * @param check   The element to check.
     * @param marked  A list of worklist elements.
     */
    protected boolean isRedundant(WorklistElement check, List<WorklistElement> marked) {
        if (marked == null) return false;

        // iterate over all WorklistElements
        for (WorklistElement w : marked) {
            if (w.getContext().equals(check.getContext())) {
                // get the according visited contexts
                // if 'check' is in 'cons', check's context is restrictive to w's context
                // now we can test if check's states are restrictive to w's states
                if (isRestrictive(check.getStates(), w.getStates())){
                    return true;
                }
            }
        }

        return false;
    }

    /** Checks if a state 'restrictive' is restrictive to a state 'to'
     *
     */
    protected boolean isRestrictive(States restrictive, States to) {
        // iterate over all states
        for (int i = 0; i < to.size(); i++) {

            // one of both is the initial state
            if (restrictive.state(i).equals(initStates.state(i)) || to.state(i).equals(initStates.state(i))) {
                if (restrictive.state(i).equals(initStates.state(i))  && !to.state(i).equals(initStates.state(i))) {
                    // not restrictive
                    return false;

                } else {
                    // restrictive state
                    continue;
                }
            }

            // if the states are from different threads
            //    check if the first thread region reaches the second
            // else
            //    perform a reachability analysis between the states
            if (restrictive.state(i).getThread() != to.state(i).getThread()) {
                SDGNode toNode = to.state(i).getNode();
                List<SDGNode> nodes = foldedIcfg.getFoldedNodesOf(toNode);
                if (!nodes.isEmpty()) {
                    toNode = nodes.get(0);
                }

                SDGNode fromNode = restrictive.state(i).getNode();
                List<SDGNode> nodes2 = foldedIcfg.getFoldedNodesOf(fromNode);
                if (!nodes2.isEmpty()) {
                    fromNode = nodes2.get(0);
                }

                if (!reachesRegions(fromNode, restrictive.state(i).getThread(), toNode, to.state(i).getThread())) {
                    return false;
                }

            } else {
                // context reachability
                if (!reaches(restrictive.state(i), to.state(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * @return true if edge e is inter-threadual.
     */
    protected boolean isInterfering(SDGEdge e) {
        return e.getKind() == SDGEdge.Kind.INTERFERENCE
                || e.getKind() == SDGEdge.Kind.FORK
                || e.getKind() == SDGEdge.Kind.FORK_IN
                || e.getKind() == SDGEdge.Kind.FORK_OUT;
    }

    /** Computes the contexts for a given node.
     *
     * @param source  The node in question.
     * @param thread  Its thread.
     * @return  A list of Contexts or an empty list.
     */
    protected Collection<Context> contextsForNode(SDGNode source, int thread) {
        return conMan.getContextsOf(source, thread);
    }

    /** Maps a SDG-Context to a CFG-Context.
     * This mapping is nessecary as SDG and CFG are folded differently.
     *
     * @param origin  A SDG-Context.
     * @param foldedCall  The call graph.
     * @param foldedIcfg  The CFG
     * @return  A CFG-Context.
     */
    protected DynamicContext map(Context origin) {
        // list for building the context that maps to Context 'con'
        LinkedList<SDGNode> res = new LinkedList<SDGNode>();

        // for every vertex in the call stack of 'con' determine the fold vertex in graph 'to'
        for (SDGNode node : origin.getCallStack()) {

            // add found vertex to list 'res'
            if (node.getKind() == SDGNode.Kind.FOLDED) {

                // If the folded node is induced by a cycle of return edges,
                // and the topmost call site of 'context' is the folded call cycle
                // according to the return-cycle, create a context without this
                // topmost call site and add it to the initial worklist.
                if (node.getLabel() == GraphFolder.FOLDED_RETURN) {
                    // creates a context without the the folded call cycle
                    // according to the return-cycle
                    SDGNode call = returnCall(node);

                    if (res.size() == 0 || call != res.getLast()) {
                        res.addLast(call);
                    }
                }

                // maps node to the belonging node of the CFG
                SDGNode x = map(node);

                // prohibit redundant piling of Contexts
                if (res.size() == 0 || x != res.getLast()) {
                    res.addLast(x);
                }

            } else {
                res.addLast(foldedIcfg.map(node));
            }
        }

        SDGNode node = foldedIcfg.map(origin.getNode());
        if (res.size() > 0 && node == res.getLast()) {
            res.removeLast();
        }

        return new DynamicContext(res, node, origin.getThread());
    }

    /** Maps a SDG-node to a CFG-node.
     *
     * @param node  A SDG-node.
     * @param foldedCall  The call graph.
     * @param foldedIcfg  The CFG
     * @return  A CFG-node.
     */
    protected SDGNode map(SDGNode node) {
        // 1. get one of the unmapped nodes
        SDGNode unmapped = conMan.unmap(node);

        // 2. get mapping in the other graph
        return foldedIcfg.map(unmapped);
    }

    /** Computes the folded call cycle belonging to a folded return cycle.
     *
     * @param returnFold  The folded return cycle.
     * @return  The belonging call cycle.
     */
    protected SDGNode returnCall(SDGNode returnFold) {
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        SDGNode callFold = null;

        worklist.add(returnFold);

        // find the corresponding folded call cycle
        loop:
            while (!worklist.isEmpty()) {
                SDGNode next = worklist.poll();
                for (SDGEdge cf : foldedIcfg.getIncomingEdgesOfKind(next, SDGEdge.Kind.CONTROL_FLOW)) {
                    if (cf.getSource().getKind() == SDGNode.Kind.FOLDED
                            && cf.getSource().getLabel() == GraphFolder.FOLDED_CALL) {

                        callFold = cf.getSource();
                        break loop;
                    }
                    worklist.addFirst(cf.getSource());
                }
            }

        return callFold;
    }

    protected boolean reachesRegions(SDGNode from, int fromThread, SDGNode to, int toThread) {
    	// 1. from reaches the fork of to's thread?
        if (sdg.getThreadsInfo().getThreadFork(toThread) != null) {
	        if (reachMap.get(sdg.getThreadsInfo().getThreadFork(toThread)).contains(from)) {
	        	return true;
	        }
        }

        // 2. to is reached by the join of from's thread?
        if (sdg.getThreadsInfo().getThreadJoin(fromThread) != null) {
        	if (reachMap.get(sdg.getThreadsInfo().getThreadJoin(fromThread)).contains(to)) {
        		return true;
        	}
        }

        return false;
    }

    protected boolean reaches(Context start, Context target) {
	    // map the target to the folded ICFG
	    DynamicContext mappedTarget = map(target);
	    DynamicContext mappedStart = map(start);
    	return reachable.reaches(mappedStart, mappedTarget);
    }

    private Map<SDGNode, Collection<SDGNode>> interThreadReach(CFG cfg) {
    	HashMap<SDGNode, Collection<SDGNode>> map = new HashMap<SDGNode, Collection<SDGNode>>();
    	CFGForward cfgf = new CFGForward(cfg);
    	CFGBackward cfgb = new CFGBackward(cfg);
    	ThreadsInformation ti = cfg.getThreadsInfo();

    	Collection<SDGNode> forks = ti.getAllForks();
    	for (SDGNode fork : forks) {
    		Collection<SDGNode> before = cfgb.slice(fork);
    		map.put(fork, before);
    	}

    	Collection<SDGNode> joins = ti.getAllJoins();
    	for (SDGNode join : joins) {
    		Collection<SDGNode> behind = cfgf.slice(join);
    		map.put(join, behind);
    	}

    	return map;
    }
}
