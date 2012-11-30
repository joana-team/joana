/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.graph.slicer.conc.simple;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;


/**
 * An analysis whether a node can reach another one in a given control flow graph.
 * The nodes are annotated with the thread they belong to to gain a thread sensitive
 * result.
 *
 * -- Created on March 21, 2006
 *
 * @see VirtualNode
 * @author  Dennis Giffhorn
 */
public class ReachAnalysis {
    private static class Cache {
        private HashMap<SDGNode, HashMap<SDGNode, TIntObjectHashMap<Boolean>>> cache
                = new  HashMap<SDGNode, HashMap<SDGNode, TIntObjectHashMap<Boolean>>>();

        private void put(SDGNode source, SDGNode target, int thread, boolean flag) {
            HashMap<SDGNode, TIntObjectHashMap<Boolean>> m1 = cache.get(source);
            if (m1 == null) {
                m1 = new HashMap<SDGNode, TIntObjectHashMap<Boolean>>();
                cache.put(source, m1);
            }

            TIntObjectHashMap<Boolean> m2 = m1.get(target);
            if (m2 == null) {
                m2 = new TIntObjectHashMap<Boolean>();
                m1.put(target, m2);
            }

            m2.put(thread, flag);
        }

        private Boolean get(SDGNode source, SDGNode target, int thread) {
            HashMap<SDGNode, TIntObjectHashMap<Boolean>> m1 = cache.get(source);
            if (m1 == null) {
                return null;
            }

            TIntObjectHashMap<Boolean> m2 = m1.get(target);
            if (m2 == null) {
                return null;
            }

            return m2.get(thread);
        }
    }

    /** A control flow graph. */
    private CFG icfg;
    private Cache cache;

    /**
     * Creates a new instance of ReachAnalysis
     *
     * @param g  A control flow graph.
     */
    public ReachAnalysis(CFG g) {
        icfg = g;
        cache = new Cache();
    }

    /**
     * Determines whether a given start node can reach a given target node using
     * a two-phase graph traversal.
     *
     * @param start   The start node.
     * @param target  The target node.
     */
    public boolean reaching(SDGNode start, SDGNode target, int thread) {
        if (start == target) return true;

        Boolean b = cache.get(start, target, thread);
        if (b != null) return b;

        // contains the so far visited nodes
        HashSet<SDGNode> marked = new HashSet<SDGNode>();
        LinkedList<SDGNode> worklist_1 = new LinkedList<SDGNode>();
        LinkedList<SDGNode> worklist_2 = new LinkedList<SDGNode>();

        // commit a forward search
        worklist_1.add(start);
        marked.add(start);

        // phase 1: only ascend into calling procedures
        while (!worklist_1.isEmpty()) {
            SDGNode next = worklist_1.poll();

            // iterate over all incoming edges of next
            for (SDGEdge e : icfg.outgoingEdgesOf(next)) {
                SDGNode sink = e.getTarget();

                if (e.getKind() == SDGEdge.Kind.RETURN ) {
                    // if source equals start, start can reach target
                    // else add it to the worklist 1
                    if (sink.isInThread(thread)) {
                        if (sink == target) {
                            cache.put(start, target, thread, true);
                            return true;

                        } else if (marked.add(sink)) {
                            worklist_1.add(sink);
                        }
                    }

                } else if (e.getKind() == SDGEdge.Kind.CALL) {
                    // if source equals start, start can reach target
                    // else add it to the worklist 2
                    if (sink == target) {
                        cache.put(start, target, thread, true);
                        return true;

                    } else if (marked.add(sink)) {
                        worklist_2.add(sink);
                    }

                } else if (e.getKind() != SDGEdge.Kind.FORK){
                    // intraprocedural edge

                    // if the source node equals the target node, start can reach target
                    // else create a new VirtualNode and add it to the worklist 1.
                    if (sink == target) {
                        cache.put(start, target, thread, true);
                        return true;

                    } else if (marked.add(sink))
                        worklist_1.add(sink);
                }
            }
        }

        // phase 2: only descend into called procedures
        while (!worklist_2.isEmpty()) {
            SDGNode next = worklist_2.poll();

            // iterate over all incoming edges of next
            for (SDGEdge e : icfg.outgoingEdgesOf(next)) {
                SDGNode sink = e.getTarget();

                if (e.getKind() != SDGEdge.Kind.RETURN
                        && e.getKind() != SDGEdge.Kind.FORK){

                    // if the source node equals the target node, start can reach target
                    // else create a new VirtualNode and add it to the worklist 1.
                    if (sink == target) {
                        cache.put(start, target, thread, true);
                        return true;

                    } else if (marked.add(sink))
                        worklist_2.add(sink);
                }
            }
        }

        cache.put(start, target, thread, false);

        return false;
    }
}
