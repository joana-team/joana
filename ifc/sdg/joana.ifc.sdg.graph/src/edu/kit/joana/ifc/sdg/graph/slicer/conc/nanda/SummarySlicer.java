/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * A modified two-phase slicer that additionally returns the set <br>
 * of visited nodes with incoming interference edges.
 *
 * -- Created on October 30, 2006
 *
 * @author  Dennis Giffhorn
 */
public abstract class SummarySlicer {
    /** The graph to slice. */
    protected SDG g;
    protected Set<SDGEdge.Kind> omittedEdges = SDGEdge.Kind.threadEdges();

    /** Creates a new instance of SummarySlicer.
     * @param graph  The graph to slice.
     */
    public SummarySlicer(SDG graph) {
        g = graph;
    }

    /** Sets a new graph to slice.
     * @param graph  The graph to slice.
     */
    public void setGraph(SDG graph) {
        g = graph;
    }

    /** Performs a two-phase slice on the attribute graph<br>.
     * and collects the visited nodes with incoming interference edges
     * @param criteria  The slicing criteria.
     * @param slice  The result set for the computed slice.
     * @return The set of visited nodes with incoming interference edges.
     */
    public abstract Collection<SDGNode> slice(Collection<SDGNode> criteria, Set<SDGNode> slice);

    static class Backward extends SummarySlicer {

        /** Creates a new instance of SummarySlicer.
         * @param graph  The graph to slice.
         */
        public Backward(SDG graph) {
            super(graph);
        }

        /** Sets a new graph to slice.
         * @param graph  The graph to slice.
         */
        public void setGraph(SDG graph) {
            this.g = graph;
        }

        /** Performs a two-phase slice on the attribute graph<br>.
         * and collects the visited nodes with incoming interference edges
         * @param criteria  The slicing criteria.
         * @param slice  The result set for the computed slice.
         * @return The set of visited nodes with incoming interference edges.
         */
        public Collection<SDGNode> slice(Collection<SDGNode> criteria, Set<SDGNode> slice) {
            // two worklists for
            LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
            LinkedList<SDGNode> nextWorklist = new LinkedList<SDGNode>();

            // set for the visited nodes with incoming interference edges
            Set<SDGNode> interfering = new HashSet<SDGNode>();
            // set for visited nodes
            Set<SDGNode> marked = new HashSet<SDGNode>();

            worklist.addAll(criteria);

            for (SDGNode v : criteria) {
                slice.add(v);
            }

            // phase 1
            while (!worklist.isEmpty()) {
                SDGNode w = worklist.poll();

                for (SDGEdge e : g.incomingEdgesOf(w)) {
                    // omitted edges are interference, fork, and fork-in edges
                    if (omittedEdges.contains(e.getKind())) {
                        // add node to interfering-set
                        interfering.add(w);
                        continue;
                    }

                    // only traverse dependence-edges
                    if (!e.getKind().isSDGEdge()) {
                        continue;
                    }

                    SDGNode v = e.getSource();

                    // if node v was not visited yet,
                    if (marked.add(v)) {
                        // if edge is a param-out edge, add to next worklist,
                        // else add to this worklist
                        if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                            nextWorklist.add(v);

                        } else {
                            worklist.add(v);
                        }

                        // add note to the result
                        slice.add(v);
                    }
                }
            }

            // phase 2
            while (!nextWorklist.isEmpty()) {
                SDGNode w = nextWorklist.poll();

                for (SDGEdge e : g.incomingEdgesOf(w)) {
                    // omitted edges are interference, fork, and fork-in edges
                    if (omittedEdges.contains(e.getKind())) {
                        // add node to interfering-set
                        interfering.add(w);
                        continue;
                    }

                    // don't traverse param-in and call-edges
                    if (!e.getKind().isSDGEdge() ||
                            e.getKind() == SDGEdge.Kind.PARAMETER_IN ||
                            e.getKind() == SDGEdge.Kind.CALL) {

                        continue;
                    }

                    SDGNode v = e.getSource();

                    // if node v was not visited yet, add it to the worklist and to the slice
                    if (marked.add(v)) {
                        nextWorklist.add(v);
                        slice.add(v);
                    }
                }
            }

            return interfering;
        }
    }

    static class Forward extends SummarySlicer {

        /** Creates a new instance of SummarySlicer.
         * @param graph  The graph to slice.
         */
        public Forward(SDG graph) {
            super(graph);
        }

        /** Sets a new graph to slice.
         * @param graph  The graph to slice.
         */
        public void setGraph(SDG graph) {
            this.g = graph;
        }

        /** Performs a two-phase slice on the attribute graph<br>.
         * and collects the visited nodes with incoming interference edges
         * @param criteria  The slicing criteria.
         * @param slice  The result set for the computed slice.
         * @return The set of visited nodes with incoming interference edges.
         */
        public Collection<SDGNode> slice(Collection<SDGNode> criteria, Set<SDGNode> slice) {
            // two worklists for
            LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
            LinkedList<SDGNode> nextWorklist = new LinkedList<SDGNode>();

            // set for the visited nodes with incoming interference edges
            Set<SDGNode> interfering = new HashSet<SDGNode>();
            // set for visited nodes
            Set<SDGNode> marked = new HashSet<SDGNode>();

            worklist.addAll(criteria);

            for (SDGNode v : criteria) {
                slice.add(v);
            }

            // phase 1
            while (!worklist.isEmpty()) {
                SDGNode w = worklist.poll();

                for (SDGEdge e : g.outgoingEdgesOf(w)) {
                    // omitted edges are interference, fork, and fork-in edges
                    if (omittedEdges.contains(e.getKind())) {
                        // add node to interfering-set
                        interfering.add(w);
                        continue;
                    }

                    // only traverse dependence-edges
                    if (!e.getKind().isSDGEdge()) {
                        continue;
                    }

                    SDGNode v = e.getTarget();

                    // if node v was not visited yet,
                    if (marked.add(v)) {
                        // if edge is a param-out edge, add to next worklist,
                        // else add to this worklist
                        if (e.getKind() == SDGEdge.Kind.PARAMETER_IN || e.getKind() == SDGEdge.Kind.CALL) {
                            nextWorklist.add(v);

                        } else {
                            worklist.add(v);
                        }

                        // add note to the result
                        slice.add(v);
                    }
                }
            }

            // phase 2
            while (!nextWorklist.isEmpty()) {
                SDGNode w = nextWorklist.poll();

                for (SDGEdge e : g.outgoingEdgesOf(w)) {
                    // omitted edges are interference, fork, and fork-in edges
                    if (omittedEdges.contains(e.getKind())) {
                        // add node to interfering-set
                        interfering.add(w);
                        continue;
                    }

                    // don't traverse param-in and call-edges
                    if (!e.getKind().isSDGEdge() ||
                            e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {

                        continue;
                    }

                    SDGNode v = e.getTarget();

                    // if node v was not visited yet, add it to the worklist and to the slice
                    if (marked.add(v)) {
                        nextWorklist.add(v);
                        slice.add(v);
                    }
                }
            }

            return interfering;
        }
    }
}
