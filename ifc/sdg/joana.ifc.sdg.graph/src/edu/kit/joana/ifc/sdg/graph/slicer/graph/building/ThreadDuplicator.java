/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.building;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/** A class for duplicating code that is shared by different threads.
 *
 * Warning: This class hasn't been used for years - if you intend to use it,
 * check first if it compatible with the current SDG structure.
 *
 * Warning: This class uses the spaghetti code pattern.
 *
 * -- Created on April 7, 2006
 *
 * @author  Dennis Giffhorn
 * @deprecated
 */
@Deprecated
public final class ThreadDuplicator {
    public static HashMap<SDGNode, SDGNode> idMap = new HashMap<SDGNode, SDGNode>();

    /** ThreadDuplicator is a utility class.
     */
    private ThreadDuplicator() { }

    /** This method duplicates procedures in a given graph that are shared by different threads.
     * The result is returned as a new graph.
     * Every node and edge in the result graph are new objects - there are no references to fields of the parameter graph.
     *
     * @param graph  The input graph.
     * @return       A new graph whose threads do not share code.
     */
    public static SDG duplicateSharedProcedures(SDG graph) {
        // contains the duplicated graph
        SDG data = new SDG();
        data.setName(graph.getName());

        // a set for all thread entry nodes
        Set<SDGNode> threadEntries = new TreeSet<SDGNode>(SDGNode.getIDComparator());
        // a list for all procedure entry nodes of the resulting graph
        LinkedList<SDGNode> entries = new LinkedList<SDGNode>();
        // a list for all inter-threadual edges
        LinkedList<SDGEdge> interthreadualEdges = new LinkedList<SDGEdge>();
        // maps node in 'graph' -> nodes in 'data' for all nodes that have inter-threadual edges
        HashMap<SDGNode, List<SDGNode>> interMap = new HashMap<SDGNode, List<SDGNode>>();
        // a counter for the new nodes IDs
        int ctr = 1;
        System.out.print("retrieve all thread entries...");
        // begin with the root node of 'graph'
        threadEntries.add(graph.getRoot());

        // add all thread entry nodes
        for (SDGEdge e : graph.edgeSet()) {
            if (e.getKind() == SDGEdge.Kind.FORK) {
                threadEntries.add(e.getTarget());
            }
        }
        System.out.println(" done");
        // now iterate over the thread entries and determine all nodes that are reachable intra-threadually
        System.out.print("duplicate code...");
        for (SDGNode entry : threadEntries) {
            // two worklists
            LinkedList<SDGNode> outerWorklist = new LinkedList<SDGNode>();
            LinkedList<SDGNode> innerWorklist = new LinkedList<SDGNode>();
            // already visited nodes
            HashSet<SDGNode> marked = new HashSet<SDGNode>();
            // maps node in 'graph' -> nodes in 'data' for the current thread
            HashMap<SDGNode, SDGNode> map = new HashMap<SDGNode, SDGNode>();
            // a list of all traversed edges of this thread
            LinkedList<SDGEdge> edges = new LinkedList<SDGEdge>();
            // the thread instance IDs of the current thread entry
            //int[] threads = g.getThreadsOf(entry);
            int[] threads = entry.getThreadNumbers();

            // init the worklist
            outerWorklist.add(entry);

            // outerWorklist only contains procedure entries, thus allowing to visit
            // all nodes in one procedure before leaving towards another procedure
            while (!outerWorklist.isEmpty()) {
                // a procedure entry
                SDGNode outerNext = outerWorklist.poll();
                // a clone
                SDGNode outerClone = outerNext.clone();

                // set the values of the clone
                outerClone.setThreadNumbers(threads);
                outerClone.setId(ctr);
                ctr++;
                data.addVertex(outerClone);
                map.put(outerNext, outerClone);
                idMap.put(outerClone, outerNext);

                // init the inner worklist for an intraprocedural traversion
                innerWorklist.add(outerNext);
                marked.add(outerNext);

                // save the entry clone for a later adjustment of the procedure IDs
                entries.addLast(outerClone);

                while (!innerWorklist.isEmpty()) {
                    // this is an original node, but was already cloned
                    SDGNode innerNext = innerWorklist.poll();

                    // traverse all intra-procedural edges
                    for (SDGEdge e : graph.outgoingEdgesOf(innerNext)) {
                        if (e.getKind() == SDGEdge.Kind.JOIN
                                || e.getKind() == SDGEdge.Kind.FORK
                                || e.getKind() == SDGEdge.Kind.FORK_IN
                                || e.getKind() == SDGEdge.Kind.FORK_OUT
                                || e.getKind() == SDGEdge.Kind.INTERFERENCE
                                || e.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {

                            // for inter-threadual edges save all clones of
                            // 'innerNext' in the interMap map
                            List<SDGNode> mapsTo = interMap.get(innerNext);

                            if (mapsTo == null) {
                                mapsTo = new LinkedList<SDGNode>();
                            }

                            mapsTo.add(map.get(innerNext));
                            interMap.put(innerNext, mapsTo);

                            // save the edge
                            interthreadualEdges.add(e);


                        } else if (e.getKind() == SDGEdge.Kind.RETURN
                                || e.getKind() == SDGEdge.Kind.PARAMETER_OUT
                                || e.getKind() == SDGEdge.Kind.PARAMETER_IN) {

                            // do not leave the procedure, but save the edge
                            edges.add(e);

                        }  else if (e.getKind() == SDGEdge.Kind.CALL){
                            // save the edge and add the target entry node to the outer worklist
                            if (marked.add(e.getTarget())) {
                                outerWorklist.add(e.getTarget());
                            }

                            edges.add(e);

                        } else {

                            if (marked.add(e.getTarget())) {
                                // clone the target node and add it to the worklist
                                SDGNode clone = e.getTarget().clone();
                                innerWorklist.add(e.getTarget());

                                // set the values of the clone
                                clone.setThreadNumbers(threads);
                                clone.setId(ctr);
                                ctr++;
                                data.addVertex(clone);

                                // remember the mapping
                                map.put(e.getTarget(), clone);
                                idMap.put(clone, e.getTarget());
//                            System.out.println(e.getTarget()+" >> "+clone);
                            }

                            // save the edge
                            edges.add(e);
                        }

                    }

                    // by now, only outgoing inter-threadual edges were found
                    // now search for incoming ones
                    for (SDGEdge e : graph.incomingEdgesOf(innerNext)) {
                        if (e.getKind() == SDGEdge.Kind.JOIN
                                || e.getKind() == SDGEdge.Kind.FORK
                                || e.getKind() == SDGEdge.Kind.FORK_IN
                                || e.getKind() == SDGEdge.Kind.FORK_OUT
                                || e.getKind() == SDGEdge.Kind.INTERFERENCE
                                || e.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {

                            // save all clones of 'innerNext' in the interMap map
                            List<SDGNode> mapsTo = interMap.get(innerNext);

                            if (mapsTo == null) {
                                mapsTo = new LinkedList<SDGNode>();
                            }

                            mapsTo.add(map.get(innerNext));
                            interMap.put(innerNext, mapsTo);

                            // save the edge
                            interthreadualEdges.add(e);
                        }
                    }
                }
            }

            // now create the intra-threadual edges for the resulting graph
            // by using the map
            for (SDGEdge edge : edges) {
                SDGNode sink = map.get(edge.getTarget());
                SDGNode source = map.get(edge.getSource());

                if (sink == null || source == null) continue;

                SDGEdge edgeClone =  edge.getKind().newEdge(source, sink);

                data.addEdge(edgeClone);
            }
        }
        data.setRoot(graph.getRoot());
        System.out.println(" done");

        System.out.println("adding interference edges...");
        System.out.println(" * processing "+interthreadualEdges.size()+" edges");
        // at last, create the inter-threadual edges for the resulting graph
        // connecting all clones of the original source and sink, respectively
        int x = 1;
        int m = 0;
        for (SDGEdge inter : interthreadualEdges) {
            if ((x % 100) == 0) {
                System.out.print(".");
            }
            if ((x % 1000) == 0) {
                System.out.print(m);
            }
            if ((x % 10000) == 0) {
                System.out.println();
            }
            List<SDGNode> sources = interMap.get(inter.getSource());
            List<SDGNode> sinks = interMap.get(inter.getTarget());

            for (SDGNode so : sources) {
                for (SDGNode si : sinks) {
                    SDGEdge edge = inter.getKind().newEdge(so, si);

                    data.addEdge(edge);
                    m++;
                }
            }
            x++;
        }
        System.out.println("\ndone");

        System.out.println("adjusting procedure IDs...");
        // set the procedure IDs in the resulting graph...
        adjustProcedureIDs(data, entries);
        System.out.println("done");

        // ... here we go!
        return data;
    }

    /** Creates new procedure IDs for a given graph.
     *
     * @param g        The SDG.
     * @param entries  All entry nodes of 'g'.
     */
    private static void adjustProcedureIDs(SDG g, LinkedList<SDGNode> entries) {
        int proc = 0;

        for (SDGNode e : entries) {
            renumber(g, e, proc);
            proc++;
        }
    }

    /** Sets the procedure ID value of all nodes of a given procedure to a given value.
     *
     * @param g       A SDG.
     * @param entry   A procedure entry node.
     * @param procID  The new procedure ID.
     */
    private static void renumber(SDG g, SDGNode entry, int procID) {
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        HashSet<SDGNode> marked = new HashSet<SDGNode>();

        worklist.add(entry);
        marked.add(entry);

        // traverse transitively all intraprocedural edges beginning at 'entry'
        // and apply the new procedure ID to all reached nodes.
        while (!worklist.isEmpty()) {
            SDGNode next = worklist.poll();

            next.setProc(procID);

            for (SDGEdge e : g.outgoingEdgesOf(next)) {
                if (e.getKind() == SDGEdge.Kind.CALL
                        || e.getKind() == SDGEdge.Kind.RETURN
                        || e.getKind() == SDGEdge.Kind.PARAMETER_IN
                        || e.getKind() == SDGEdge.Kind.PARAMETER_OUT
                        || e.getKind() == SDGEdge.Kind.FORK
                        || e.getKind() == SDGEdge.Kind.JOIN
                        || e.getKind() == SDGEdge.Kind.FORK_IN
                        || e.getKind() == SDGEdge.Kind.FORK_OUT
                        || e.getKind() == SDGEdge.Kind.INTERFERENCE
                        || e.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {

                    continue;
                }

                if (marked.add(e.getTarget())) {
                    worklist.add(e.getTarget());
                }
            }
        }
    }
}
