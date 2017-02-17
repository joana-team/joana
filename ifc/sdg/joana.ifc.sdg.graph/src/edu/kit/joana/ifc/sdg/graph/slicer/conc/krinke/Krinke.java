/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.krinke;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.ContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.StaticContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;



/** This class realises Krinke's unoptimised algorithm for threaded interprocedural slicing.
 * <strong>NOTE:</strong> This implementation does not work anymore due to changes in the
 * structure of our SDGs. It will not receive an update to fix that issue.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 * @deprecated
 */
@Deprecated
public class Krinke implements edu.kit.joana.ifc.sdg.graph.slicer.Slicer {
	public static long elems = 0L;

    /** the folded icfg for reachability checking purpose */
    private FoldedCFG foldedIcfg;
    /** The graph to be sliced. */
    private SDG ipdg;
    protected ContextManager man;

    /** realises the reachability checking algorithm */
    private ReachabilityChecker reachable;
    /** the initial states of the threads */
    private States states;

    /** The intrathreadual slicer. */
    public ContextSlicer slicer;

    public Krinke() { }

    /** Creates a new instance of Krinke.
     *
     * @param graph A threaded interprocedural program dependencies graph that shall be sliced.
     *              It has to contain control flow edges.
     */
    public Krinke(SDG graph) {
		setGraph(graph);
    }

    /** Initialises the fields of the Krinke slicer.
     */
    public void setGraph(SDG graph) {
        ipdg = graph;

        man = StaticContextManager.create(ipdg);

        // build the threaded ICFG
        CFG icfg = ICFGBuilder.extractICFG(ipdg);

        // fold ICFG with Krinke's two-pass folding algorithm
        foldedIcfg = GraphFolder.twoPassFolding(icfg);

        // determine the amount of threads in the TIPDG
        int threads = ipdg.getNumberOfThreads();

        // init the states tuple
        states = this.initialStateTuple(threads);

        // init the intrathredual slicer
        slicer = new ContextSlicer(ipdg, man);

        // init the valid-path checker
        this.reachable = new ReachabilityChecker(foldedIcfg);
    }

    public Collection<SDGNode> slice(SDGNode criterion) {
    	return slice(Collections.singleton(criterion));
    }


    /** The slicing algorithm.
     *
     * @param criterion Contains a node and a thread ID for which the program
     *                  shall be sliced.
     */
    public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
        HashSet<SDGNode> slice = new HashSet<SDGNode>();
        HashSet<WorklistElement> mark = new HashSet<WorklistElement>();

        // initialize worklist and mark structure
        LinkedList<WorklistElement> worklist = initialWorklist(criteria);

        for(WorklistElement o : worklist){elems++;
            mark.add(o);
        }

        // slicing..
        while(!worklist.isEmpty()){
            WorklistElement next = worklist.poll();
            SDGNode add = next.getContext().getNode();

            // add top node of sliced context
            slice.add(add);

            // slice for context in element 'next'
            Collection<WorklistElement> with_interference = threadSlice(next, slice);

            // processing interference edges
            for(WorklistElement w : with_interference){
                SDGNode n = w.getNode();

                for(SDGEdge e : ipdg.incomingEdgesOf(n)){
                    // handle all interference edges

                    if(e.getKind() == SDGEdge.Kind.INTERFERENCE
                            || e.getKind() == SDGEdge.Kind.FORK
                            || e.getKind() == SDGEdge.Kind.FORK_IN
                            || e.getKind() == SDGEdge.Kind.FORK_OUT) {

                        SDGNode source = e.getSource();
                        int[] reached_threads  = source.getThreadNumbers();

                        // handling virtual code doubling
                        for (int thread : reached_threads) {
                            // only process real thread changes
                            if (thread != w.getThread()) {
                                // get all context for 'source'
                                LinkedList<Context> contextList = new LinkedList<Context>();

                                contextList.addAll(man.getContextsOf(source, thread));

                                LinkedList<Context> reached = new LinkedList<Context>();

                                if (w.getStates().get(thread).isEmpty()) {
                                    reached = contextList;

                                } else {
                                    DynamicContext target = map(w.getStates().get(thread));

                                    for (Context s : contextList) {
                                        DynamicContext start = map(s);

                                        if (reachable.reaches(start, target)) {
                                            reached.add(s);
                                        }
                                    }
                                }

                                // update worklist
                                if (!reached.isEmpty()){
                                    for(Context context : reached){
                                        // build new worklist elements
                                        States new_states = w.getStates().clone();

                                        // thread states update
                                        new_states.set(w.getThread(), w.getContext());

                                        // new worklist element
                                        WorklistElement newW =
                                                new WorklistElement(context, new_states);

                                        if(!mark.contains(newW)){
                                            mark.add(newW);
                                            worklist.addFirst(newW);elems++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return slice;
    }

    protected Collection<WorklistElement> threadSlice(WorklistElement next, HashSet<SDGNode> slice) {
    	return slicer.slice(next, slice);
    }


    /* auxiliary methods */

    /** Maps a SDG-Context to a CFG-Context.
     * (CFG-Contexts are stronger folded.)
     *
     * @param origin  The SDG-Context.
     * @return  A CFG-Context.
     */
    private DynamicContext map(Context origin) {
        // list for building the context that maps to Context 'con'
        LinkedList<SDGNode> res = new LinkedList<SDGNode>();

        // for every vertex in the call stack of 'con' determine the fold vertex in graph 'to'
        for (SDGNode node : origin.getCallStack()) {
            res.addFirst(map(node));
        }

        res.addLast(foldedIcfg.map(origin.getNode()));

        // filter
        LinkedList<SDGNode> filtered = filter(res);
        SDGNode newNode = filtered.poll();

        // build Context and add pair to map
        return new DynamicContext(filtered, newNode, origin.getThread());
    }

    private LinkedList<SDGNode> filter(LinkedList<SDGNode> toFilter) {
        LinkedList<SDGNode> res = new LinkedList<SDGNode>();

        // for every vertex in the call stack of 'con' determine the fold vertex in graph 'to'
        for (SDGNode node : toFilter) {

            // add vertex to list 'res'
            if (node.getKind() == SDGNode.Kind.FOLDED) {

                // If the folded node is induced by a cycle of return edges,
                // and the topmost call site of 'context' is the folded call cycle
                // according to the return-cycle, create a context without this
                // topmost call site and add it to the initial worklist.
                if (node.getLabel() == GraphFolder.FOLDED_RETURN) {
                    // creates a context without the the folded call cycle
                    // according to the return-cycle
                    SDGNode call = returnCall(node);

                    if (!res.isEmpty() && call == res.getFirst()) {
                        res.removeFirst();
                    }
                }

                if (res.isEmpty() || node != res.getFirst()) {
                    res.addFirst(node);
                }

            } else {
                res.addFirst(node);
            }
        }

        return res;
    }

    /** Maps SDG-Nodes to CFG-Nodes.
     * Needed as folding is stronger in CFG.
     *
     * @param node  The SDG-node to map.
     * @return  The CFG-node.
     */
    private SDGNode map(SDGNode node) {
        // 1. get one of the unmapped nodes
        SDGNode unmapped = man.unmap(node);

        // 2. get mapping in the other graph
        return foldedIcfg.map(unmapped);
    }

    /** Computes the folded call cycle belonging to a folded return cycle.
     *
     * @param returnFold  The folded return cycle.
     * @return  The belonging call cycle.
     */
    private SDGNode returnCall(SDGNode returnFold) {
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


    /**
     * Computes the initial state tuple.
     * Its length is set to the amount of threads in the graph and all
     * elements are set to 'null'.
     *
     * @param width
     *               The amount of threads.
     * @return
     *               The initial state tuple.
     */
    private States initialStateTuple(int width) {
        States init = new States(width +1);

        for (int i = 0; i <= width; i++) {
            init.set(i, new DynamicContext(null, i));
        }

        return init;
    }

    /**
     * Creates the initial worklist for the slicing algorithm.
     * It consists of a LinkedList containing one ThreadedWorklistElement for
     * every context which can reach the given starting criterion.
     *
     * @param node  The starting criterion.
     * @return  A LinkedList, maybe empty.
     */
    protected LinkedList<WorklistElement> initialWorklist(Collection<SDGNode> criteria) {
        LinkedList<WorklistElement> s = new LinkedList<WorklistElement>();

        for (SDGNode node : criteria) {
            // test whether criterion is valid
            int[] threads = node.getThreadNumbers();

            for (int thread : threads) {
                List<Context> contexts = new LinkedList<Context>();
                contexts.addAll(man.getContextsOf(node, thread));

                // construct all initial ThreadedWorklistElement objects
                for (Context con : contexts) {
                    States states_copy = states.clone();

                    s.add(new WorklistElement(con, states_copy));elems++;
                }

                // for start node and first call node
                if (contexts.isEmpty()) {
                    States states_copy = states.clone();
                    Context con = new DynamicContext(node, 0);

                    s.add(new WorklistElement(con, states_copy));elems++;
                }
            }
        }

        return s;
    }
}
