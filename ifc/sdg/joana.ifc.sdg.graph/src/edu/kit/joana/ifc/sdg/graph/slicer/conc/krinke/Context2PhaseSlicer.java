/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.krinke;

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
public class Context2PhaseSlicer<C extends Context<C>> {
    private final Set<SDGEdge.Kind> threadEdges = SDGEdge.Kind.threadEdges();
    private final ContextManager<C> man;
    protected final SDG sdg;

    /**
     * Creates a new instance of Context2PhaseSlicer
     *
     * @param g The SDG to slice.
     */
    public Context2PhaseSlicer(SDG g, ContextManager<C> m) {
        sdg = g;
        man = m;
    }

    /** Computes the precise slice for the given context.
     * It works by using call strings in the first phase to ascend to
     * calling methods context-sensitively.
     *
     * @param criteria A set of contexts to slice.
     * @return The slice as a sorted set of nodes.
     */
    public Collection<SDGNode> slice(Collection<C> criteria, HashSet<SDGNode> slice) {
        // the sorted set for the result
        HashSet<SDGNode> interfering = new HashSet<SDGNode>();

        // sets for marking visited nodes and contexts
        HashSet<SDGNode> markedNodes = new HashSet<SDGNode>();
        HashSet<C> markedContexts = new HashSet<>();

        // two worklists
        LinkedList<C> worklist1 = new LinkedList<>();
        LinkedList<SDGNode> worklist2 = new LinkedList<SDGNode>();

        // initialize the first worklist
        worklist1.addAll(criteria);
        markedContexts.addAll(criteria);

        // phase 1
        while (!worklist1.isEmpty()) {
            // retrieve the next node and add it to the slice
            C next = worklist1.poll();
            slice.add(next.getNode());

            // now check all incoming edges
            for (SDGEdge e : sdg.incomingEdgesOf(next.getNode())) {
                if (!e.getKind().isSDGEdge()) continue;

                SDGNode reached = e.getSource();

                if (e.getKind() == SDGEdge.Kind.PARAMETER_IN || e.getKind() == SDGEdge.Kind.CALL) {
                    // our graph representation has a special case for class initializer methods
                    if (e.getKind() == SDGEdge.Kind.PARAMETER_IN && e.getSource().getKind() == SDGNode.Kind.FORMAL_OUT) {
                        // retrieve the contexts of the class initializer
                        Collection<? extends C> newContexts = man.getAllContextsOf(reached);

                        // add the contexts
                        for (C newContext : newContexts) {
                            if (!markedContexts.contains(newContext)) {
                                worklist1.add(newContext);
                                markedContexts.add(newContext);
                            }
                        }

                    } else {
                        // ascend to the calling method
                    	SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                        C[] cons = man.ascend(reached, callSite ,next);

                        // add the contexts to the worklist
                        for (C c : cons) {
                            if (c != null && !markedContexts.contains(c)) {
                                worklist1.add(c);
                                markedContexts.add(c);
                            }
                        }
                    }

                } else if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                    // add the node to the second worklist
                    worklist2.add(reached);
                    markedNodes.add(reached);

                } else if (threadEdges.contains(e.getKind())){
                    interfering.add(reached);

                } else if(e.getKind().isSDGEdge()) {
                    // only traverse dependence edges, but do not leave the thread
                    C newContext = man.level(reached, next);

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

            // traverse all incoming intraprocedural edges or
            // descend into called procedures
            for (SDGEdge e : sdg.incomingEdgesOf(next)) {
                if (!e.getKind().isSDGEdge()) continue;

                SDGNode reached = e.getSource();

                if (threadEdges.contains(e.getKind())) {
                    interfering.add(reached);

                } else if (e.getKind() != SDGEdge.Kind.CALL
                        && e.getKind() != SDGEdge.Kind.PARAMETER_IN) {

                    // add the found nodes
                    if (!markedNodes.contains(reached)) {
                        worklist2.add(reached);
                        markedNodes.add(reached);
                    }
                }
            }
        }

        return interfering;
    }
}
