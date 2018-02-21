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
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.StaticContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.StaticContextManager.StaticContext;


/** Offers two context-based sequential slicing algorithms.
 *
 * -- Created on March 20, 2006
 *
 * @author  Dennis Giffhorn
 */
public abstract class ContextSlicer<C extends Context<C>> implements Slicer {
    protected Set<SDGEdge.Kind> omittedEdges = SDGEdge.Kind.threadEdges();
    protected SDG sdg;
    protected ContextManager<C> conMan;
    protected final Function<SDG, ContextManager<C>> newManager;
    
    protected static final Function<SDG, ContextManager<DynamicContext>> newDynamicManager = (sdg -> new DynamicContextManager(sdg));
    protected static final Function<SDG, ContextManager<StaticContext>>  newStaticManager  = (sdg -> StaticContextManager.create(sdg));

    protected ContextSlicer(SDG graph, Function<SDG, ContextManager<C>> newManager) {
        this.newManager = newManager;
        omittedEdges = SDGEdge.Kind.threadEdges(); // we have to traverse summary edges, because it is the only way to
                                                   // deal with method stubs

        if (graph != null) {
        	setGraph(graph);
        }
    }

    protected ContextSlicer(SDG graph, Set<SDGEdge.Kind> omit, Function<SDG, ContextManager<C>> newManager) {
    	omittedEdges = omit;
    	this.newManager = newManager;

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

        conMan = newManager.apply(sdg);
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
        LinkedList<C> worklist_1 = new LinkedList<>();
        LinkedList<C> worklist_2 = new LinkedList<>();

        // init worklist
        for (SDGNode c : criterion) {
        	worklist_1.addAll(conMan.getAllContextsOf(c));
        }

        // slice
        while(!worklist_1.isEmpty()) {
            // next element, put it in the slice
            C next = worklist_1.poll();
            slice.add(next.getNode());

            // handle all incoming edges of 'next'
            for(SDGEdge e : getEdges(next.getNode())) {
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
                    		worklist_1.add(con);
                       	}
                    }

                } else if (isAscendingEdge(e.getKind())) {
                    // go to the calling procedure
                	if (n.isInThread(next.getThread()) && next.isInCallingProcedure(n)) {
                        SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                        C[] newContexts = conMan.ascend(n, callSite, next);

                        for (C con : newContexts) {
                        	if (con != null && visited.add(con)) {
                        		worklist_1.add(con);
                        	}
                        }
                    }

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    C con = conMan.descend(n, callSite, next);

                    if (visited.add(con)) {
                		worklist_2.add(con);
                	}

                } else {
                    // intra-procedural traversal
                    C con = conMan.level(n, next);

                    if (visited.add(con)) {
                		worklist_1.add(con);
                	}
                }
            }
        }

        // slice
        while(!worklist_2.isEmpty()) {
            // next element, put it in the slice
            C next = worklist_2.poll();
            slice.add(next.getNode());

            // handle all incoming edges of 'next'
            for(SDGEdge e : getEdges(next.getNode())){
            	if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind())) {
                    continue;
                }

                SDGNode n = getAdjacentNode(e);

                // distinguish between different kinds of edges
                if (isAscendingEdge(e.getKind())) {
                    // skip

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    C con = conMan.descend(n, callSite, next);

                    if (visited.add(con)) {
                		worklist_2.add(con);
                	}

                } else {
                    // intra-procedural traversal
                    C con = conMan.level(n, next);

                    if (visited.add(con)) {
                		worklist_2.add(con);
                	}
                }
            }
        }

        // return the slice
        return slice;
    }


    public Collection<? extends C> contextSliceNodes(Collection<SDGNode> criterion) {
    	HashSet<C> s = new HashSet<>();
    	for (SDGNode n : criterion) {
    		s.addAll(conMan.getAllContextsOf(n));
    	}
    	return contextSlice(s);
    }

    public Collection<? extends C> contextSlice(SDGNode criterion) {
    	return contextSlice(conMan.getAllContextsOf(criterion));
    }

    public Collection<? extends C> contextSlice(C criterion) {
    	return contextSlice(Collections.singleton(criterion));
    }

    /** Slices an IPDG context-sensitively by using call string information.
     * Thread edges and summary edges are not traversed.
     *
     * @param criterion  The slicing criterion
     * @return The slice, as a collection of Contexts.
     */
    public Collection<? extends C> contextSlice(Collection<? extends C> criterion){
    	HashSet<C> slice = new HashSet<>();
        LinkedList<C> worklist_1 = new LinkedList<>();
        LinkedList<C> worklist_2 = new LinkedList<>();

        // init worklist
        for (C c : criterion) {
        	if (slice.add(c)) {
                worklist_1.add(c);
        	}
        }

        // slice
        while(!worklist_1.isEmpty()) {
            // next element, put it in the slice
            C next = worklist_1.poll();

            // handle all incoming edges of 'next'
            for(SDGEdge e : getEdges(next.getNode())) {
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
                    		worklist_1.add(con);
                       	}
                    }

                } else if (isAscendingEdge(e.getKind())) {
                    // go to the calling procedure
                	if (n.isInThread(next.getThread()) && next.isInCallingProcedure(n)) {
                        SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                        C[] newContexts = conMan.ascend(n, callSite, next);

                        for (C con : newContexts) {
                        	if (con != null && slice.add(con)) {
                        		worklist_1.add(con);
                        	}
                        }
                    }

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    C con = conMan.descend(n, callSite, next);

                    if (slice.add(con)) {
                		worklist_2.add(con);
                	}

                } else {
                    // intra-procedural traversal
                    C con = conMan.level(n, next);

                    if (slice.add(con)) {
                		worklist_1.add(con);
                	}
                }
            }
        }

        // slice
        while(!worklist_2.isEmpty()) {
            // next element, put it in the slice
            C next = worklist_2.poll();

            // handle all incoming edges of 'next'
            for(SDGEdge e : getEdges(next.getNode())){
            	if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind())) {
                    continue;
                }

                SDGNode n = getAdjacentNode(e);

                // distinguish between different kinds of edges
                if (isAscendingEdge(e.getKind())) {
                    // skip

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    C con = conMan.descend(n, callSite, next);

                    if (slice.add(con)) {
                		worklist_2.add(con);
                	}

                } else {
                    // intra-procedural traversal
                    C con = conMan.level(n, next);

                    if (slice.add(con)) {
                		worklist_2.add(con);
                	}
                }
            }
        }

        // return the slice
        return slice;
    }


    /* subgraph slicing for node-subgraph */

    public Collection<SDGNode> subgraphSlice(SDGNode criterion, Collection<SDGNode> subgraph) {
    	return subgraphSlice(Collections.singleton(criterion), subgraph);
    }

    /** Slices an IPDG context-sensitively by using call string information.
     * Thread edges and summary edges are not traversed.
     *
     * @param criterion  The slicing criterion
     * @return The slice, as a collection of SDGNodes.
     */
    public Collection<SDGNode> subgraphSlice(Collection<SDGNode> criterion, Collection<SDGNode> subgraph){
    	HashSet<SDGNode> slice = new HashSet<SDGNode>();
    	HashSet<C> visited = new HashSet<>();
        LinkedList<C> worklist_1 = new LinkedList<>();
        LinkedList<C> worklist_2 = new LinkedList<>();

        // init worklist
        for (SDGNode c : criterion) {
        	if (subgraph.contains(c)) {
        		worklist_1.addAll(conMan.getAllContextsOf(c));
        	}
        }

        // slice
        while(!worklist_1.isEmpty()) {
            // next element, put it in the slice
            C next = worklist_1.poll();
            slice.add(next.getNode());

            // handle all incoming edges of 'next'
            for(SDGEdge e : getEdges(next.getNode())) {
            	if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind())) {
                    continue;
                }

                SDGNode n = getAdjacentNode(e);

                if (!subgraph.contains(n)) continue;

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
                    		worklist_1.add(con);
                       	}
                    }

                } else if (isAscendingEdge(e.getKind())) {
                    // go to the calling procedure
                	if (n.isInThread(next.getThread()) && next.isInCallingProcedure(n)) {
                        SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                        C[] newContexts = conMan.ascend(n, callSite, next);

                        for (C con : newContexts) {
                        	if (con != null && visited.add(con)) {
                        		worklist_1.add(con);
                        	}
                        }
                    }

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    C con = conMan.descend(n, callSite, next);

                    if (visited.add(con)) {
                		worklist_2.add(con);
                	}

                } else {
                    // intra-procedural traversal
                    C con = conMan.level(n, next);

                    if (visited.add(con)) {
                		worklist_1.add(con);
                	}
                }
            }
        }

        // slice
        while(!worklist_2.isEmpty()) {
            // next element, put it in the slice
            C next = worklist_2.poll();
            slice.add(next.getNode());

            // handle all incoming edges of 'next'
            for(SDGEdge e : getEdges(next.getNode())){
            	if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind())) {
                    continue;
                }

                SDGNode n = getAdjacentNode(e);

                if (!subgraph.contains(n)) continue;

                // distinguish between different kinds of edges
                if (isAscendingEdge(e.getKind())) {
                    // skip

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    C con = conMan.descend(n, callSite, next);

                    if (visited.add(con)) {
                		worklist_2.add(con);
                	}

                } else {
                    // intra-procedural traversal
                    C con = conMan.level(n, next);

                    if (visited.add(con)) {
                		worklist_2.add(con);
                	}
                }
            }
        }

        // return the slice
        return slice;
    }


    public Collection<C> subgraphContextSliceNodes(Collection<SDGNode> criterion, Collection<SDGNode> subgraph) {
    	HashSet<C> s = new HashSet<>();
    	for (SDGNode n : criterion) {
    		s.addAll(conMan.getAllContextsOf(n));
    	}
    	return subgraphContextSlice(s, subgraph);
    }

    public Collection<C> subgraphContextSlice(SDGNode criterion, Collection<SDGNode> subgraph) {
    	return subgraphContextSlice(conMan.getAllContextsOf(criterion), subgraph);
    }

    public Collection<C> subgraphContexSlice(C criterion, Collection<SDGNode> subgraph) {
    	return subgraphContextSlice(Collections.singleton(criterion), subgraph);
    }

    /** Slices an IPDG context-sensitively by using call string information.
     * Thread edges and summary edges are not traversed.
     *
     * @param criterion  The slicing criterion
     * @return The slice, as a collection of Contexts.
     */
    public Collection<C> subgraphContextSlice(Collection<? extends C> criterion, Collection<SDGNode> subgraph){
    	HashSet<C> slice = new HashSet<>();
        LinkedList<C> worklist_1 = new LinkedList<>();
        LinkedList<C> worklist_2 = new LinkedList<>();

        // init worklist
        for (C c : criterion) {
        	if (subgraph.contains(c.getNode()) && slice.add(c)) {
                worklist_1.add(c);
        	}
        }

        // slice
        while(!worklist_1.isEmpty()) {
            // next element, put it in the slice
            C next = worklist_1.poll();

            // handle all incoming edges of 'next'
            for(SDGEdge e : getEdges(next.getNode())) {
            	if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind())) {
                    continue;
                }

                SDGNode n = getAdjacentNode(e);

                if (!subgraph.contains(n)) continue;

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
                    		worklist_1.add(con);
                       	}
                    }

                } else if (isAscendingEdge(e.getKind())) {
                    // go to the calling procedure
                	if (n.isInThread(next.getThread()) && next.isInCallingProcedure(n)) {
                        SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                        C[] newContexts = conMan.ascend(n, callSite, next);

                        for (C con : newContexts) {
                        	if (con != null && slice.add(con)) {
                        		worklist_1.add(con);
                        	}
                        }
                    }

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    C con = conMan.descend(n, callSite, next);

                    if (slice.add(con)) {
                		worklist_2.add(con);
                	}

                } else {
                    // intra-procedural traversal
                    C con = conMan.level(n, next);

                    if (slice.add(con)) {
                		worklist_1.add(con);
                	}
                }
            }
        }

        // slice
        while(!worklist_2.isEmpty()) {
            // next element, put it in the slice
            C next = worklist_2.poll();

            // handle all incoming edges of 'next'
            for(SDGEdge e : getEdges(next.getNode())){
            	if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind())) {
                    continue;
                }

                SDGNode n = getAdjacentNode(e);

                if (!subgraph.contains(n)) continue;

                // distinguish between different kinds of edges
                if (isAscendingEdge(e.getKind())) {
                    // skip

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    C con = conMan.descend(n, callSite, next);

                    if (slice.add(con)) {
                		worklist_2.add(con);
                	}

                } else {
                    // intra-procedural traversal
                    C con = conMan.level(n, next);

                    if (slice.add(con)) {
                		worklist_2.add(con);
                	}
                }
            }
        }

        // return the slice
        return slice;
    }


    /* subgraph slicing for context-subgraph */

    public Collection<SDGNode> contextSubgraphSlice(SDGNode criterion, Collection<C> subgraph) {
    	return contextSubgraphSlice(Collections.singleton(criterion), subgraph);
    }

    /** Slices an IPDG context-sensitively by using call string information.
     * Thread edges and summary edges are not traversed.
     *
     * @param criterion  The slicing criterion
     * @return The slice, as a collection of SDGNodes.
     */
    public Collection<SDGNode> contextSubgraphSlice(Collection<SDGNode> criterion, Collection<? extends C> subgraph){
    	HashSet<SDGNode> slice = new HashSet<SDGNode>();
    	HashSet<C> visited = new HashSet<>();
        LinkedList<C> worklist_1 = new LinkedList<>();
        LinkedList<C> worklist_2 = new LinkedList<>();

        // init worklist
        for (SDGNode c : criterion) {
        	for (C con : conMan.getAllContextsOf(c)) {
	        	if (subgraph.contains(con)) {
	        		worklist_1.add(con);
	        	}
        	}
        }

        // slice
        while(!worklist_1.isEmpty()) {
            // next element, put it in the slice
            C next = worklist_1.poll();
            slice.add(next.getNode());

            // handle all incoming edges of 'next'
            for(SDGEdge e : getEdges(next.getNode())) {
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
                    	if (subgraph.contains(con) && visited.add(con)) {
                    		worklist_1.add(con);
                       	}
                    }

                } else if (isAscendingEdge(e.getKind())) {
                    // go to the calling procedure
                	if (n.isInThread(next.getThread()) && next.isInCallingProcedure(n)) {
                        SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                        C[] newContexts = conMan.ascend(n, callSite, next);

                        for (C con : newContexts) {
                        	if (con != null && subgraph.contains(con) && visited.add(con)) {
                        		worklist_1.add(con);
                        	}
                        }
                    }

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    C con = conMan.descend(n, callSite, next);

                    if (subgraph.contains(con) && visited.add(con)) {
                		worklist_2.add(con);
                	}

                } else {
                    // intra-procedural traversal
                    C con = conMan.level(n, next);

                    if (subgraph.contains(con) && visited.add(con)) {
                		worklist_1.add(con);
                	}
                }
            }
        }

        // slice
        while(!worklist_2.isEmpty()) {
            // next element, put it in the slice
            C next = worklist_2.poll();
            slice.add(next.getNode());

            // handle all incoming edges of 'next'
            for(SDGEdge e : getEdges(next.getNode())){
            	if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind())) {
                    continue;
                }

                SDGNode n = getAdjacentNode(e);

                // distinguish between different kinds of edges
                if (isAscendingEdge(e.getKind())) {
                    // skip

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    C con = conMan.descend(n, callSite, next);

                    if (subgraph.contains(con) && visited.add(con)) {
                		worklist_2.add(con);
                	}

                } else {
                    // intra-procedural traversal
                    C con = conMan.level(n, next);

                    if (subgraph.contains(con) && visited.add(con)) {
                		worklist_2.add(con);
                	}
                }
            }
        }

        // return the slice
        return slice;
    }


    public Collection<C> contextSubgraphContextSliceNodes(Collection<SDGNode> criterion, Collection<C> subgraph) {
    	HashSet<C> s = new HashSet<>();
    	for (SDGNode n : criterion) {
    		s.addAll(conMan.getAllContextsOf(n));
    	}
    	return contextSubgraphContextSlice(s, subgraph);
    }

    public Collection<C> contextSubgraphContextSlice(SDGNode criterion, Collection<C> subgraph) {
    	return contextSubgraphContextSlice(conMan.getAllContextsOf(criterion), subgraph);
    }

    public Collection<C> contextSubgraphContextSlice(C criterion, Collection<C> subgraph) {
    	return contextSubgraphContextSlice(Collections.singleton(criterion), subgraph);
    }

    /** Slices an IPDG context-sensitively by using call string information.
     * Thread edges and summary edges are not traversed.
     *
     * @param criterion  The slicing criterion
     * @return The slice, as a collection of Contexts.
     */
    public Collection<C> contextSubgraphContextSlice(Collection<? extends C> criterion, Collection<C> subgraph){
    	HashSet<C> slice = new HashSet<>();
        LinkedList<C> worklist_1 = new LinkedList<>();
        LinkedList<C> worklist_2 = new LinkedList<>();

        // init worklist
        for (C c : criterion) {
        	if (subgraph.contains(c) && slice.add(c)) {
                worklist_1.add(c);
        	}
        }

        // slice
        while(!worklist_1.isEmpty()) {
            // next element, put it in the slice
            C next = worklist_1.poll();

            // handle all incoming edges of 'next'
            for(SDGEdge e : getEdges(next.getNode())) {
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
                    	if (subgraph.contains(con) && slice.add(con)) {
                    		worklist_1.add(con);
                       	}
                    }

                } else if (isAscendingEdge(e.getKind())) {
                    // go to the calling procedure
                	if (n.isInThread(next.getThread()) && next.isInCallingProcedure(n)) {
                        SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                        C[] newContexts = conMan.ascend(n, callSite, next);

                        for (C con : newContexts) {
                        	if (con != null && subgraph.contains(con) && slice.add(con)) {
                        		worklist_1.add(con);
                        	}
                        }
                    }

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    C con = conMan.descend(n, callSite, next);

                    if (subgraph.contains(con) && slice.add(con)) {
                		worklist_2.add(con);
                	}

                } else {
                    // intra-procedural traversal
                    C con = conMan.level(n, next);

                    if (subgraph.contains(con) && slice.add(con)) {
                		worklist_1.add(con);
                	}
                }
            }
        }

        // slice
        while(!worklist_2.isEmpty()) {
            // next element, put it in the slice
            C next = worklist_2.poll();

            // handle all incoming edges of 'next'
            for(SDGEdge e : getEdges(next.getNode())){
            	if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind())) {
                    continue;
                }

                SDGNode n = getAdjacentNode(e);

                // distinguish between different kinds of edges
                if (isAscendingEdge(e.getKind())) {
                    // skip

                } else if (isDescendingEdge(e.getKind())) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    C con = conMan.descend(n, callSite, next);

                    if (subgraph.contains(con) && slice.add(con)) {
                		worklist_2.add(con);
                	}

                } else {
                    // intra-procedural traversal
                    C con = conMan.level(n, next);

                    if (subgraph.contains(con) && slice.add(con)) {
                		worklist_2.add(con);
                	}
                }
            }
        }

        // return the slice
        return slice;
    }
}

