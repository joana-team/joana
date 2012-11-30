/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.ContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.StaticContextManager;


/** Offers two context-based sequential slicing algorithms.
 *
 * -- Created on March 20, 2006
 *
 * @author  Dennis Giffhorn
 */
public abstract class IPDGSlicer implements Slicer {
    protected Set<SDGEdge.Kind> omittedEdges = SDGEdge.Kind.threadEdges();
    protected SDG sdg;
    protected boolean staticCM;
    protected ContextManager conMan;

    public IPDGSlicer(SDG g, boolean staticContexts) {
        staticCM = staticContexts;
        omittedEdges = SDGEdge.Kind.threadEdges(); // we have to traverse summary edges, because it is the only way to
                                                   // deal with method stubs

        if (g != null) {
        	setGraph(g);
        }
    }

    public IPDGSlicer(SDG graph, Set<SDGEdge.Kind> omit, boolean staticContexts) {
        staticCM = staticContexts;
    	omittedEdges = omit;

        if (graph != null) {
        	setGraph(graph);
        }
    }

    protected abstract Collection<SDGEdge> getEdges(SDGNode n);

    protected abstract SDGNode getAdjacentNode(SDGEdge e);

    protected abstract boolean isAscendingEdge(SDGEdge.Kind k);

    protected abstract boolean isDescendingEdge(SDGEdge.Kind k);

    public void setGraph(SDG graph) {
        sdg = graph;

        if (staticCM) {
        	conMan = StaticContextManager.create(sdg);

        } else {
        	conMan = new DynamicContextManager(sdg);
        }
    }

    public Collection<SDGNode> slice(SDGNode criterion) {
    	return slice(Collections.singleton(criterion));
    }

    /** Slices an IPDG context-sensitively by using call string information.
     * Thread edges and summary edges are not traversed.
     *
     * @param criterion  The slicing criterion
     * @return The slice, as a collection of SDGNodes.
     */
    public Collection<SDGNode> slice(Collection<SDGNode> criterion){
    	HashSet<SDGNode> slice = new HashSet<SDGNode>();
    	HashSet<Context> visited = new HashSet<Context>();
        LinkedList<Context> worklist = new LinkedList<Context>();

        // init worklist
        for (SDGNode c : criterion) {
        	worklist.addAll(conMan.getAllContextsOf(c));
        }

        // slice
        while(!worklist.isEmpty()) {
            // next element, put it in the slice
            Context next = worklist.poll();
            slice.add(next.getNode());

            // handle all incoming edges of 'next'
            for(SDGEdge e : getEdges(next.getNode())){
            	if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind())) {
                    continue;
                }

                SDGNode n = getAdjacentNode(e);

                // distinguish between different kinds of edges
                if ((e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN)
                		&& n.getKind() == SDGNode.Kind.FORMAL_OUT) {
                    // The class initialiser method is a special case due to the structure of the given SDG graphs.
                    // It can be recognised by having the only formal-out vertex with an outgoing param-in edge
                    // which is also the only 'entry point' during an intrathreadural backward slice.
                    Collection<Context> newContexts = conMan.getContextsOf(n, 0);

                    // update the worklist
                    for (Context con : newContexts) {
                    	if (visited.add(con)) {
                    		worklist.add(con);
                    	}
                    }

                } else if (isAscendingEdge(e.getKind())) {
                    // go to the calling procedure
                	if (n.isInThread(next.getThread()) && next.isInCallingProcedure(n)) {
                        SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                        Context[] newContexts = conMan.ascend(n, callSite, next);

                        for (Context con : newContexts) {
                        	if (con != null && visited.add(con)) {
                        		worklist.add(con);
                        	}
                        }
                    }

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    Context con = conMan.descend(n, callSite, next);

                    if (visited.add(con)) {
                		worklist.add(con);
                	}

                } else {
                    // intraprocedural traversal
                    Context con = conMan.level(n, next);

                    if (visited.add(con)) {
                		worklist.add(con);
                	}
                }
            }
        }

        // return the slice
        return slice;
    }


    public Collection<Context> contextSlice(SDGNode criterion) {
    	return contextSlice(conMan.getAllContextsOf(criterion));
    }

    public Collection<Context> contextSlice(Context criterion) {
    	return contextSlice(Collections.singleton(criterion));
    }

    /** Slices an IPDG context-sensitively by using call string information.
     * Thread edges and summary edges are not traversed.
     *
     * @param criterion  The slicing criterion
     * @return The slice, as a collection of SDGNodes.
     */
    public Collection<Context> contextSlice(Collection<Context> criterion){
    	HashSet<Context> slice = new HashSet<Context>();
        LinkedList<Context> worklist = new LinkedList<Context>();

        // init worklist
        for (Context c : criterion) {
        	if (slice.add(c)) {
        		worklist.add(c);
        	}
        }

        // slice
        while(!worklist.isEmpty()) {
            // next element, put it in the slice
            Context next = worklist.poll();

            // handle all incoming edges of 'next'
            for(SDGEdge e : getEdges(next.getNode())){
            	if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind())) {
                    continue;
                }

                SDGNode n = getAdjacentNode(e);

                // distinguish between different kinds of edges
                if ((e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN)
                		&& n.getKind() == SDGNode.Kind.FORMAL_OUT) {
                    // The class initialiser method is a special case due to the structure of the given SDG graphs.
                    // It can be recognised by having the only formal-out vertex with an outgoing param-in edge
                    // which is also the only 'entry point' during an intrathreadural backward slice.
                    Collection<Context> newContexts = conMan.getContextsOf(n, 0);

                    // update the worklist
                    for (Context con : newContexts) {
                    	if (slice.add(con)) {
                    		worklist.add(con);
                    	}
                    }

                } else if (isAscendingEdge(e.getKind())) {
                    // go to the calling procedure
                	if (n.isInThread(next.getThread()) && next.isInCallingProcedure(n)) {
                        SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                        Context[] newContexts = conMan.ascend(n, callSite, next);

                        for (Context con : newContexts) {
                        	if (con != null && slice.add(con)) {
                        		worklist.add(con);
                        	}
                        }
                    }

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    Context con = conMan.descend(n, callSite, next);

                    if (slice.add(con)) {
                		worklist.add(con);
                	}

                } else {
                    // intraprocedural traversal
                    Context con = conMan.level(n, next);

                    if (slice.add(con)) {
                		worklist.add(con);
                	}
                }
            }
        }

        // return the slice
        return slice;
    }
}

