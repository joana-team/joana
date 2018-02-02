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
import java.util.function.Function;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.ContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.StaticContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.StaticContextManager.StaticContext;


/** Offers two context-based sequential slicing algorithms.
 *
 * -- Created on March 20, 2006
 *
 * @author  Dennis Giffhorn
 */
public abstract class IPDGSlicer<C extends Context> implements Slicer {
    protected Set<SDGEdge.Kind> omittedEdges = SDGEdge.Kind.threadEdges();
    protected SDG sdg;
    protected boolean staticCM;
    protected ContextManager<C> conMan;
    protected final Function<SDG, ContextManager<C>> newManager;
    
    protected static final Function<SDG, ContextManager<DynamicContext>> newDynamicManager = (sdg -> new DynamicContextManager(sdg));
    protected static final Function<SDG, ContextManager<StaticContext>>  newStaticManager  = (sdg -> StaticContextManager.create(sdg));


    protected IPDGSlicer(SDG g, Function<SDG, ContextManager<C>> newManager) {
        this.newManager = newManager;
        omittedEdges = SDGEdge.Kind.threadEdges(); // we have to traverse summary edges, because it is the only way to
                                                   // deal with method stubs

        if (g != null) {
        	setGraph(g);
        }
    }

    public IPDGSlicer(SDG graph, Set<SDGEdge.Kind> omit, Function<SDG, ContextManager<C>> newManager) {
        this.newManager = newManager;
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

        conMan = newManager.apply(graph);
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
    	HashSet<C> visited = new HashSet<>();
        LinkedList<C> worklist = new LinkedList<>();

        // init worklist
        for (SDGNode c : criterion) {
        	worklist.addAll(conMan.getAllContextsOf(c));
        }

        // slice
        while(!worklist.isEmpty()) {
            // next element, put it in the slice
            C next = worklist.poll();
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
                    Collection<C> newContexts = conMan.getContextsOf(n, 0);

                    // update the worklist
                    for (C con : newContexts) {
                    	if (visited.add(con)) {
                    		worklist.add(con);
                    	}
                    }

                } else if (isAscendingEdge(e.getKind())) {
                    // go to the calling procedure
                	if (n.isInThread(next.getThread()) && next.isInCallingProcedure(n)) {
                        SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                        C[] newContexts = conMan.ascend(n, callSite, next);

                        for (C con : newContexts) {
                        	if (con != null && visited.add(con)) {
                        		worklist.add(con);
                        	}
                        }
                    }

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    C con = conMan.descend(n, callSite, next);

                    if (visited.add(con)) {
                		worklist.add(con);
                	}

                } else {
                    // intraprocedural traversal
                    C con = conMan.level(n, next);

                    if (visited.add(con)) {
                		worklist.add(con);
                	}
                }
            }
        }

        // return the slice
        return slice;
    }


    public Collection<C> contextSlice(SDGNode criterion) {
    	return contextSlice(conMan.getAllContextsOf(criterion));
    }

    public Collection<C> contextSlice(C criterion) {
    	return contextSlice(Collections.singleton(criterion));
    }

    /** Slices an IPDG context-sensitively by using call string information.
     * Thread edges and summary edges are not traversed.
     *
     * @param criterion  The slicing criterion
     * @return The slice, as a collection of SDGNodes.
     */
    public Collection<C> contextSlice(Collection<? extends C> criterion){
    	HashSet<C> slice = new HashSet<>();
        LinkedList<C> worklist = new LinkedList<>();

        // init worklist
        for (C c : criterion) {
        	if (slice.add(c)) {
        		worklist.add(c);
        	}
        }

        // slice
        while(!worklist.isEmpty()) {
            // next element, put it in the slice
            C next = worklist.poll();

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
                    Collection<C> newContexts = conMan.getContextsOf(n, 0);

                    // update the worklist
                    for (C con : newContexts) {
                    	if (slice.add(con)) {
                    		worklist.add(con);
                    	}
                    }

                } else if (isAscendingEdge(e.getKind())) {
                    // go to the calling procedure
                	if (n.isInThread(next.getThread()) && next.isInCallingProcedure(n)) {
                        SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                        C[] newContexts = conMan.ascend(n, callSite, next);

                        for (C con : newContexts) {
                        	if (con != null && slice.add(con)) {
                        		worklist.add(con);
                        	}
                        }
                    }

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    C con = conMan.descend(n, callSite, next);

                    if (slice.add(con)) {
                		worklist.add(con);
                	}

                } else {
                    // intraprocedural traversal
                    C con = conMan.level(n, next);

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

