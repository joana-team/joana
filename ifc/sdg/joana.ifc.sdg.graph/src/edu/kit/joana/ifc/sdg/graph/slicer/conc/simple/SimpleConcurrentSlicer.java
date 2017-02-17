/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.graph.slicer.conc.simple;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;


/**
 * A simple slicer for threaded Java programs.
 * It basically uses Krinke's algorithm, but doesn't save
 * contexts in the thread states, but simple nodes.
 * Therefore it is somewhat imprecise, but more precise than
 * the iterated two-phase-slicer and considerably faster than
 * Krinke's algorithm.
 *
 * -- Created on March 20, 2006
 *
 * @author  Dennis Giffhorn
 */
public class SimpleConcurrentSlicer implements Slicer {
	public static long elems = 0L;

    /** The dependence graph. */
    private SDG ipdg;
    /** A modified two-phase-slicer. */
    private Modded2PhaseSlicer twoPhaseSlicer;
    /** A reachability analysis for nodes. */
    private ReachAnalysis reachAnalysis;

    private HashMap<VirtualNode, List<States>> marked;

    public SimpleConcurrentSlicer() { }

    /**
     * Creates a new instance of SimpleConcurrentSlicer
     *
     * @param g  The system dependence graph to slice.
     */
    public SimpleConcurrentSlicer(SDG g) {
    	setGraph(g);
    }

    public void setGraph(SDG g) {
        ipdg = g;
        CFG icfg = ICFGBuilder.extractICFG(ipdg);
        twoPhaseSlicer = new Modded2PhaseSlicer(ipdg);
        reachAnalysis = new ReachAnalysis(icfg);
        marked = new HashMap<VirtualNode, List<States>>();
    }

    public Collection<SDGNode> slice(SDGNode criterion) {
    	return slice(Collections.singleton(criterion));
    }

    /**
     * Computes the slice for a given set of criteria.
     *
     * @param criteria  The slicing criteria.
     * @return          The result as a set of nodes in ascending order.
     */
    public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
        // contains the slicing result
        HashSet<SDGNode> slice = new HashSet<SDGNode>();

        // contains all previously occurred thread entry points
        marked.clear();

        // the worklist
        LinkedList<AnnotatedNode> worklist = initWorklist(criteria);

        //eingefuegt = worklist.size();
        // iterate over worklist
        while (!worklist.isEmpty()) {elems++;
            final AnnotatedNode next = worklist.poll();

            // compute a two-phase-slice, that returns a set of possible interference edge traverisons
            Collection<Interfering> interfering = twoPhaseSlicer.slice(next, slice);

            // identify the valid interference edge traversions and get the reached nodesx
            LinkedList<AnnotatedNode> valid = validInterferenceTraversals(next.getStates(), interfering);

            // add all those reached nodes that didn't occur yet to the worklist
            for (AnnotatedNode a : valid) {
                if (!restrictive(a)) {
                    markState(a);
//                  if (marked.add(a)) {
                    worklist.add(a);
                }
            }
        }

        return slice;
    }

    /**
     * Initialises the worklist.
     * It takes the slicing criteria and annotates them with the initial thread states.
     *
     * @param criteria  The slicing criteria.
     * @return          The initial worklist.
     */
    private LinkedList<AnnotatedNode> initWorklist(Collection<SDGNode> criteria) {
        // the resulting list
        LinkedList<AnnotatedNode> init = new LinkedList<AnnotatedNode>();

        // the amount of thread instances
        int ta = ipdg.getNumberOfThreads();

        for (SDGNode n : criteria) {
            // the threads n belongs to
            int[] threads = n.getThreadNumbers();

            // iterate over those threads
            for (int t : threads) {
                // create a new States object
                States s = new States(ta);
                // create a new AnnotatedNode object
                AnnotatedNode a = new AnnotatedNode(n, s, t);

                // set every state to the default value TOP
                for (int i = 0; i < ta; i++) {
                    s.set(i, null);
                }

                // mark state
//                marked.add(a);
                markState(a);
                init.add(a);
            }
        }

        return init;
    }

    /**
     * Computes which possible interference edge traverions are valid.
     * For every valid traversion a new AnnotatedNode is created from the reached ndoe.
     *
     * @param interfering  The possible interference edge traversions.
     * @return             A set of all nodes reached via valid interference edges.
     */
    private LinkedList<AnnotatedNode> validInterferenceTraversals(States oldStates, Collection<Interfering> interfering) {
        // a list for all nodes reached by valid interference edge traversals
        LinkedList<AnnotatedNode> l = new LinkedList<AnnotatedNode>();

        for (Interfering inter : interfering) {
            // the source of the interference edge
            SDGNode source = inter.getSource();

            // the thread of 'source'
            int sourceThread = inter.getSourceThread();

            // if the current state of 'sourceThread' is the initial state, the traversal is valid
            // else we need a reaching analysis
            if (oldStates.get(sourceThread) == null || ipdg.getThreadsInfo().isDynamic(sourceThread)) {
                States newStates = oldStates.clone();
//                newStates.set(sourceThread, source);
                newStates.set(inter.getSinkThread(), inter.getSink());
                final AnnotatedNode a = new AnnotatedNode(source, newStates, sourceThread);

                l.add(a);

            } else {
                // if start reaches target, the traversal is valid
                if (reachAnalysis.reaching(source, oldStates.get(sourceThread), sourceThread)) {
                    States newStates = oldStates.clone();
//                    newStates.set(sourceThread, source);
                    newStates.set(inter.getSinkThread(), inter.getSink());
                    final AnnotatedNode a = new AnnotatedNode(source, newStates, sourceThread);
                    l.add(a);
                }
            }
        }

        return l;
    }

    private boolean restrictive(AnnotatedNode toCheck) {
        List<States> marks = marked.get(toCheck.getVirtual());
        if (marks == null) {
            return false;
        }

        States states = toCheck.getStates();

        for (States prev : marks) {
            boolean redundant = true;

            for (int i = 0; i < states.size(); i++) {
                if (!reaching(states.get(i), prev.get(i), i)) {
                    redundant = false;
                    break;
                }
            }

            if (redundant) {
                return true;
            }
        }

        return false;
    }

    private boolean reaching(SDGNode start, SDGNode target, int thread) {
        if (target == null) {
            return true;

        } else  if (start == null) {
            return false;

        } else {
            return reachAnalysis.reaching(start, target, thread);
        }
    }

    private void markState(AnnotatedNode node) {
        List<States> l = marked.get(node.getVirtual());

        if (l == null) {
            l = new LinkedList<States>();
            marked.put(node.getVirtual(), l);
        }

        l.add(node.getStates());
    }
}
