/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.krinke;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.ContextManager;


/**
 * Offers two context-based intra-threadual slicing algorithms.
 *
 * -- Created on March 20, 2006
 *
 * @author  Dennis Giffhorn
 */
public class ContextSlicer {
    private SDG sdg;
    private ContextManager man;
    private Set<SDGEdge.Kind> threadEdges = SDGEdge.Kind.threadEdges();

    /** Containing marks for visited vertices. */
    protected HashMap<SDGNode, HashSet<WorklistElement>> map;

    /**
     * Instantiates a new ContextSlicer.
     * @param The SDG to slice.
     * @param The call graph of the program.
     * @param The thread regions of the program.
     */
    public ContextSlicer(SDG g, ContextManager man){
        sdg = g;
        this.man = man;
        map = new HashMap<SDGNode, HashSet<WorklistElement>>();
    }

    /**
     * Returns the map of visited worklist elements.
     */
    public Map<SDGNode, HashSet<WorklistElement>> getMap() {
        return map;
    }

    /**
     * Slices a threaded interprocedural graph using explicitly context sensitive slicing.
     * Interference edges and summary edges are not traversed.
     * Visited vertices with incoming interference edges are stored and returned in a LinkedList.
     *
     * @param criterium The slicing criterion
     * @return A list of WorklistElement objects representing those visited vertices
     *          that have an incoming interference edge.
     */
    public Collection<WorklistElement> slice(WorklistElement criterium, Set<SDGNode> slice) {
        HashSet<WorklistElement> has_interference = new HashSet<WorklistElement>();
        map.clear();

        // init worklists
        LinkedList<WorklistElement> worklist_1 = new LinkedList<WorklistElement>();
        LinkedList<WorklistElement> worklist_2 = new LinkedList<WorklistElement>();
        worklist_1.add(criterium);
        addMark(criterium.getNode(), criterium);

        while(!worklist_1.isEmpty()) {
            // next element, put it in the slice
        	WorklistElement next = worklist_1.poll();
            slice.add(next.getNode());

            // handle all incoming edges of 'next'
            for(SDGEdge e : sdg.incomingEdgesOf(next.getNode())) {//sdg.incomingEdgesOf(next.getNode())){
            	if (!e.getKind().isSDGEdge()) {
                    continue;
                }

                SDGNode n = e.getSource();

                // distinguish between different kinds of edges
                if ((e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN)
                		&& n.getKind() == SDGNode.Kind.FORMAL_OUT) {

                    // The class initialiser method is a special case due to the structure of the given SDG graphs.
                    // It can be recognised by having the only formal-out vertex with an outgoing param-in edge
                    // which is also the only 'entry point' during an intrathreadural backward slice.
                    Collection<Context> newContexts = man.getAllContextsOf(n);

                    // update the worklist
                    for (Context con : newContexts) {
                        addToWorklist(worklist_1, n, con, next.getStates(), next.getThread());
                    }

                } else if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN) {
                    // go to the calling procedure
                	if (n.isInThread(next.getThread()) && next.getContext().isInCallingProcedure(n)) {
                        SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                        Context[] newContexts = man.ascend(n, callSite, next.getContext());

                        for (Context con : newContexts) {
                        	if (con != null) {
                        		addToWorklist(worklist_1, n, con, next.getStates(), next.getThread());
                        	}
                        }
                    }

                } else if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    Context con = man.descend(n, callSite, next.getContext());
                    addToWorklist(worklist_2, n, con, next.getStates(), next.getThread());

                } else if (threadEdges.contains(e.getKind())) {
                    has_interference.add(next);

                } else {
                    // intra-procedural traversal
                    Context con = man.level(n, next.getContext());
                	addToWorklist(worklist_1, n, con, next.getStates(), next.getThread());
                }
            }
        }

        // slice
        while(!worklist_2.isEmpty()) {
            // next element, put it in the slice
            WorklistElement next = worklist_2.poll();
            slice.add(next.getNode());

            // handle all incoming edges of 'next'
            for(SDGEdge e : sdg.incomingEdgesOf(next.getNode())){
            	if (!e.getKind().isSDGEdge()) {
                    continue;
                }

                SDGNode n = e.getSource();

                // distinguish between different kinds of edges
                if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN) {
                    // skip

                } else if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    Context con = man.descend(n, callSite, next.getContext());
                    addToWorklist(worklist_2, n, con, next.getStates(), next.getThread());

                } else if (threadEdges.contains(e.getKind())) {
                    has_interference.add(next);

                } else {
                    // intra-procedural traversal
                    Context con = man.level(n, next.getContext());
                    addToWorklist(worklist_2, n, con, next.getStates(), next.getThread());
                }
            }
        }

        // return all found vertices with outgoing interference edges
        return has_interference;
    }

    /**
     * Slices a threaded interprocedural graph using explicitly context sensitive slicing.
     * Interference edges and summary edges are not traversed.
     * Traversion is further restricted to a given set of vertices.
     * Visited vertices with incoming interference edges are stored and returned in a LinkedList.
     *
     * @param criterium The slicing criterion
     * @param border This vertices may not be left during the traversion.
     * @return A list of WorklistElement objects representing those visited vertices
     * that have an incoming interference edge.
     */
    public Collection<WorklistElement> subGraphSlice(WorklistElement criterium, Collection<SDGNode> subGraph) {
        HashSet<WorklistElement> has_interference = new HashSet<WorklistElement>();
        map.clear();

        // init worklists
        LinkedList<WorklistElement> worklist_1 = new LinkedList<WorklistElement>();
        LinkedList<WorklistElement> worklist_2 = new LinkedList<WorklistElement>();
        worklist_1.add(criterium);
        addMark(criterium.getNode(), criterium);

        while(!worklist_1.isEmpty()) {
            // next element, put it in the slice
        	WorklistElement next = worklist_1.poll();

            // handle all incoming edges of 'next'
            for(SDGEdge e : sdg.incomingEdgesOf(next.getNode())) {//sdg.incomingEdgesOf(next.getNode())){
            	if (!e.getKind().isSDGEdge()) {
                    continue;
                }

                SDGNode n = e.getSource();

                // distinguish between different kinds of edges
                if (threadEdges.contains(e.getKind())) {
                    has_interference.add(next);

                } else if (!subGraph.contains(n)) {
                	// skip

                } else if ((e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN)
                		&& n.getKind() == SDGNode.Kind.FORMAL_OUT) {

                    // The class initialiser method is a special case due to the structure of the given SDG graphs.
                    // It can be recognised by having the only formal-out vertex with an outgoing param-in edge
                    // which is also the only 'entry point' during an intrathreadural backward slice.
                    Collection<Context> newContexts = man.getAllContextsOf(n);

                    // update the worklist
                    for (Context con : newContexts) {
                        addToWorklist(worklist_1, n, con, next.getStates(), next.getThread());
                    }

                } else if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN) {
                    // go to the calling procedure
                	if (n.isInThread(next.getThread()) && next.getContext().isInCallingProcedure(n)) {
                        SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                        Context[] newContexts = man.ascend(n, callSite, next.getContext());

                        for (Context con : newContexts) {
                        	if (con != null) {
                        		addToWorklist(worklist_1, n, con, next.getStates(), next.getThread());
                        	}
                        }
                    }

                } else if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    Context con = man.descend(n, callSite, next.getContext());
                    addToWorklist(worklist_2, n, con, next.getStates(), next.getThread());

                } else {
                    // intra-procedural traversal
                    Context con = man.level(n, next.getContext());
                	addToWorklist(worklist_1, n, con, next.getStates(), next.getThread());
                }
            }
        }

        // slice
        while(!worklist_2.isEmpty()) {
            // next element, put it in the slice
            WorklistElement next = worklist_2.poll();

            // handle all incoming edges of 'next'
            for(SDGEdge e : sdg.incomingEdgesOf(next.getNode())){
            	if (!e.getKind().isSDGEdge()) {
                    continue;
                }

                SDGNode n = e.getSource();

                // distinguish between different kinds of edges
                if (threadEdges.contains(e.getKind())) {
                    has_interference.add(next);

                } else if (!subGraph.contains(n)) {
                	// skip

                } else if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN) {
                    // skip

                } else if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    Context con = man.descend(n, callSite, next.getContext());
                    addToWorklist(worklist_2, n, con, next.getStates(), next.getThread());

                } else {
                    // intra-procedural traversal
                    Context con = man.level(n, next.getContext());
                    addToWorklist(worklist_2, n, con, next.getStates(), next.getThread());
                }
            }
        }

        // return all found vertices with outgoing interference edges
        return has_interference;
    }

    protected void addToWorklist(LinkedList<WorklistElement> worklist, SDGNode newNode,
    		Context newCon, States oldStates, int oldThread) {
        // update the states
        States newStates = oldStates.clone();
        newStates.set(oldThread, newCon);
        WorklistElement twe = new WorklistElement(newCon, newStates);

        // Has vertex 'node' already been visited ?
        if (!isProperlyMarked(newNode, twe)) {
            worklist.add(twe);
            addMark(newNode, twe);
        }
    }


    /* map modifying methods */

    /**
     * Adds a worklist element for a given vertex to the marking map.
     *
     * @param node The vertex that has to be marked.
     * @param con The worklist element.
     */
    protected void addMark(SDGNode node, WorklistElement con) {
        HashSet<WorklistElement> marks = this.map.get(node);

        // if there isn't any mark yet, a list for the marks has to be created first
        if (marks == null) {
            marks = new HashSet<WorklistElement>();
            this.map.put(node, marks);

        }

        marks.add(con);
    }

    /**
     * Checks whether a given worklist element was already visited.
     *
     * @param node The vertex of the element.
     * @param context The worklist element.
     */
    protected boolean isProperlyMarked(SDGNode node, WorklistElement context) {
        HashSet<WorklistElement> marks = this.map.get(node);
        if (marks == null) {
            return false;
        }

        return marks.contains(context);
    }
}

