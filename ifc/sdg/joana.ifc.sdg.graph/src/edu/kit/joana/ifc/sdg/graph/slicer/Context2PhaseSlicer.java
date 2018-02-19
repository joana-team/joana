/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.ContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.StaticContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.StaticContextManager.StaticContext;


/**
 * A two-phase slice that gets a context as slicing criterion and
 * computes the precise slice for that context.
 * It works by using call strings in the first phase to ascend to
 * calling methods context-sensitively.
 *
 * -- Created on January 31, 2006
 *
 * @author  Dennis Giffhorn
 */
public abstract class Context2PhaseSlicer {
    private Set<SDGEdge.Kind> omittedEdges = SDGEdge.Kind.threadEdges();
    private ContextManager<StaticContext> man;
    protected SDG sdg;

    /**
     * Creates a new instance of Context2PhaseSlicer
     *
     * @param g The SDG to slice.
     */
    public Context2PhaseSlicer(SDG g) {
        setGraph(g);
    }

    public Context2PhaseSlicer(SDG g, ContextManager<StaticContext> m) {
        sdg = g;
        man = m;
    }

    /**
     * Sets the graph to slice to the given graph.
     *
     * @param graph The new SDG to slice.
     */
    public void setGraph(SDG graph) {
        sdg = graph;
        man = StaticContextManager.create(graph);
    }

    public void setOmittedEdges(Set<SDGEdge.Kind> omit){
        this.omittedEdges = omit;
    }

    /** Computes the precise slice for the given context.
     * It works by using call strings in the first phase to ascend to
     * calling methods context-sensitively.
     *
     * @param criteria A set of contexts to slice.
     * @return The slice as a sorted set of nodes.
     */
    public Collection<SDGNode> contextSlice(Collection<StaticContext> criteria) {
        // the sorted set for the result
        HashSet<SDGNode> slice = new HashSet<SDGNode>();

        // sets for marking visited nodes and contexts
        HashSet<SDGNode> markedNodes = new HashSet<SDGNode>();
        HashSet<StaticContext> markedContexts = new HashSet<>();

        // two worklists
        LinkedList<StaticContext> worklist1 = new LinkedList<>();
        LinkedList<SDGNode> worklist2 = new LinkedList<SDGNode>();

        // initialize the first worklist
        worklist1.addAll(criteria);
        markedContexts.addAll(criteria);

        // phase 1
        while (!worklist1.isEmpty()) {
            // retrieve the next node and add it to the slice
        	StaticContext next = worklist1.poll();
            slice.add(next.getNode());

            // now check all incoming edges
            for (SDGEdge e : edgesToTraverse(next.getNode())) {
                if (omittedEdges.contains(e.getKind()) || !e.getKind().isSDGEdge()) continue;

                SDGNode reached = reachedNode(e);

                if (e.getKind() == SDGEdge.Kind.PARAMETER_IN && e.getSource().getKind() == SDGNode.Kind.FORMAL_OUT) {
                	// class initializer; demands special treatment
                    // retrieve the contexts of the class initializer
                    Collection<? extends StaticContext> newContexts = man.getAllContextsOf(reached);

                    // add the contexts
                    for (StaticContext newContext : newContexts) {
                        if (!markedContexts.contains(newContext)) {
                            worklist1.add(newContext);
                            markedContexts.add(newContext);
                        }
                    }

                } else if (ascend(e)) {
                    // ascend to the calling method
                	SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                	StaticContext[] cons = man.ascend(reached, callSite ,next);

                    // add the contexts to the worklist
                    for (StaticContext c : cons) {
                        if (c != null && !markedContexts.contains(c)) {
                            worklist1.add(c);
                            markedContexts.add(c);
                        }
                    }

                } else if (descend(e)) {
                    // add the node to the second worklist
                    worklist2.add(reached);
                    markedNodes.add(reached);

                } else {
                    // intra-procedural edges
                	StaticContext newContext = man.level(reached, next);

                    // add the found context
                    if (!markedContexts.contains(newContext)) {
                        worklist1.add(newContext);
                        markedContexts.add(newContext);
                    }
                }
            }
        }

        // phase 2
        while (!worklist2.isEmpty()) {
            // retrieve the next node and add it to the slice
            SDGNode next = worklist2.poll();
            slice.add(next);

            // traverse all intra-procedural edges or
            // descend into called procedures
            for (SDGEdge e : edgesToTraverse(next)) {
                if (omittedEdges.contains(e.getKind()) || !e.getKind().isSDGEdge()) continue;

                SDGNode reached = reachedNode(e);

                if (!ascend(e)) {
                    // traverse only intra-procedural or descending edges
                    if (markedNodes.add(reached)) {
                        worklist2.add(reached);
                    }
                }
            }
        }

        return slice;
    }

    protected abstract Collection<SDGEdge> edgesToTraverse(SDGNode node);

    protected abstract SDGNode reachedNode(SDGEdge edge);

    protected abstract boolean ascend(SDGEdge edge);

    protected abstract boolean descend(SDGEdge edge);

    // DEBUG
    public ContextManager<StaticContext> getMan() {
    	return man;
    }
}
