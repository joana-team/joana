/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.graph.slicer.conc.simple;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;


/**
 * A thread sensitive two-phase slicer that returns all possible interference
 * edge traversions it encountered.
 *
 * -- Created on March 20, 2006
 *
 * @author  Dennis Giffhorn
 */
public class Modded2PhaseSlicer {
    /** The dependence graph. */
    private SDG ipdg;
    private HashMap<VirtualNode, Collection<Interfering>> cache;
    private HashMap<VirtualNode, Collection<SDGNode>> cache2;

    /**
     * Creates a new instance of ContextbasedSlicer
     *
     * @param g  A dependence graph.
     */
    public Modded2PhaseSlicer(SDG g) {
        ipdg = g;
        cache = new HashMap<VirtualNode, Collection<Interfering>>();
        cache2 = new HashMap<VirtualNode, Collection<SDGNode>>();
    }

    /**
     * Computes a sequential slice, adds the result to a given result set and returns
     * a set of possible interference edge traversals.
     * Uses the standard two-phase slicing algorithm.
     *
     * @param criterion  The slicing criterion.
     * @param slice      The result set for the slice.
     * @return           A set of possible interference edge traversals
     */
    public Collection<Interfering> slice(AnnotatedNode criterion, Set<SDGNode> slice) {
        Collection<Interfering> cached = cache.get(criterion.getVirtual());

        if (cached == null) {
            // two worklists for the two-phase algorithm
            LinkedList<SDGNode> worklist_1 = new LinkedList<SDGNode>();
            LinkedList<SDGNode> worklist_2 = new LinkedList<SDGNode>();
            // a set of already visited nodes
            HashSet<SDGNode> marked = new HashSet<SDGNode>();
            // the current thread
            int thread = criterion.getThread();
//            final States oldStates = criterion.getStates();

            // a list for the possible interference and fork-site edge traversions
            HashSet<Interfering> interfering = new HashSet<Interfering>();

            worklist_1.add(criterion.getNode());
            marked.add(criterion.getNode());
//            slice.add(criterion.getNode());

            // === phase 1 ===
            while (!worklist_1.isEmpty()) {
                SDGNode next = worklist_1.poll();

//                slice.add(next);

                for (SDGEdge edge : ipdg.incomingEdgesOf(next)) {
                	if (!edge.getKind().isSDGEdge()) continue;

                    if (edge.getKind().isThreadEdge()) {
                        // don't traverse interference edges but save them in the 'interfering' list
                        interfering.addAll(interfering(edge.getSource(), next, thread));

                    } else if (edge.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                        // traverse the edge and add the reached node to worklist 2
                        if (marked.add(edge.getSource())) {
                            worklist_2.add(edge.getSource());
                        }

                    } else if (edge.getKind() == SDGEdge.Kind.CALL || edge.getKind() == SDGEdge.Kind.PARAMETER_IN) {
                        SDGNode source = edge.getSource();

                        // traverse the edge and add the reached node to worklist 1
                        if (source.isInThread(thread) && marked.add(source)) {
                            worklist_1.add(source);
                        }

                    } else if (edge.getKind().isSDGEdge()) {
                        // traverse the edge and add the reached node to worklist 1
                        if (marked.add(edge.getSource())) {
                            worklist_1.add(edge.getSource());
                        }
                    }
                }
            }

            // === phase 2 ===
            while (!worklist_2.isEmpty()) {
                SDGNode next = worklist_2.poll();

//                slice.add(next);

                for (SDGEdge edge : ipdg.incomingEdgesOf(next)) {
                	if (!edge.getKind().isSDGEdge()) continue;

                    if (edge.getKind().isThreadEdge()) {
                        interfering.addAll(interfering(edge.getSource(), next, thread));

                    } else if (edge.getKind() != SDGEdge.Kind.CALL
                            && edge.getKind() != SDGEdge.Kind.PARAMETER_IN) {

                        // an intraprocedural or a parameter-out edge
                        // traverse the edge and add the reached node to worklist 2
                        if (marked.add(edge.getSource())) {
                            worklist_2.add(edge.getSource());
                        }
                    }
                }
            }

            cache.put(criterion.getVirtual(), interfering);
            cache2.put(criterion.getVirtual(), marked);
            slice.addAll(marked);
            return interfering;

        } else {
            slice.addAll(cache2.get(criterion.getVirtual()));
            return cached;
        }
    }

    /**
     * Creates a set of Interfering objects for given source and sink nodes.
     * For every thread the source belongs to, one Interfering object will be created.
     *
     * @param source  The source node.
     * @param sink    The sink node.
     * @return        The set of Interfering objects.
     */
    private LinkedList<Interfering> interfering(SDGNode source, SDGNode sink, int thread) {
        LinkedList<Interfering> l = new LinkedList<Interfering>();

        int[] threads = source.getThreadNumbers();

        for (int t : threads) {
            if (t == thread) continue;

            Interfering inter = new Interfering(source, t, sink, thread);

            l.add(inter);
        }

        return l;
    }
}

