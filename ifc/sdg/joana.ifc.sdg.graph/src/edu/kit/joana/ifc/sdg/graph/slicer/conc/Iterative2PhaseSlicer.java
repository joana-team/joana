/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * ContextSensitiveSlicer.java
 *
 * Created on August 1, 2005, 2:10 PM
 *
 */

package edu.kit.joana.ifc.sdg.graph.slicer.conc;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.ContextInsensitiveBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.ContextInsensitiveSlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.SDGSlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;


/**
 * This class represents the Interative 2-Phase Slicer described in Nanda's
 * PhD. thesis. It is a correction of Zhao's unsound 2-phase slicer
 * (@see sliceMDG)
 *
 * Warning: This algorithm is thread-insensitive. A thread-sensitive variant is
 * provided by NandaI2PSlicer.
 *
 * @author hammer, giffhorn
 */
public abstract class Iterative2PhaseSlicer implements Slicer {
    interface Phase {
        public boolean follow(SDGEdge e);
        public boolean saveInOtherWorklist(SDGEdge e);
    }

    public static long elems = 0L;
    protected static boolean DEBUG = false;

    protected SDG g;

    protected abstract Collection<SDGEdge> edgesToTraverse(SDGNode node);

    protected abstract SDGNode reachedNode(SDGEdge edge);

    protected abstract Phase phase1();

    protected abstract Phase phase2();

    /**
     * Creates a new instance of ContextSensitiveSlicer
     */
    protected Iterative2PhaseSlicer(SDG g) {
        this.g = g;
    }

    public void setGraph(SDG graph) {
        g = graph;
    }

    public Collection<SDGNode> slice(SDGNode c) {
    	return slice(Collections.singleton(c));
    }

    public Collection<SDGNode> slice(Collection<SDGNode> c) {
        HashMap<SDGNode, Phase> slice = new HashMap<SDGNode, Phase>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        Phase phase1 = phase1();
        Phase phase2 = phase2();

        worklist.addAll(c);elems += worklist.size();

        for (SDGNode v : c) {
            slice.put(v, phase1);
        }

        while (!worklist.isEmpty()) {
        	SDGNode next = worklist.poll();
        	Phase currentPhase = slice.get(next);

        	for (SDGEdge e : edgesToTraverse(next)) {
        		if (!e.getKind().isSDGEdge()) continue;

        		SDGNode adjacent = reachedNode(e);
        		Phase status = slice.get(adjacent);

        		if (status == null // hasn't been visited before
        				|| (status == phase2 && (currentPhase == phase1 || e.getKind().isThreadEdge()))) {

        			// if we are in phase 1 or e is not a descending edge, traverse e
        			if (currentPhase.follow(e)) {
        				if (e.getKind().isThreadEdge()) {
        					worklist.addFirst(adjacent); elems++;
        				} else {
        					worklist.add(adjacent);
        				}

        				// determine how to mark `adjacent'
        				if (currentPhase == phase1 && currentPhase.saveInOtherWorklist(e)) {
        					// standard two-phase slicing: mark adjacent with phase 2
        					slice.put(adjacent, phase2);

        				} else if (currentPhase == phase2 && e.getKind().isThreadEdge()) {
        					// we are in phase 2 and about to traverse an interference edge: mark adjacent with phase 1
        					slice.put(adjacent, phase1);

        				} else {
        					// mark adjacent with the current phase
        					slice.put(adjacent, currentPhase);
        				}
        			}
        		}
        	}
        }

        return slice.keySet();
    }

    /**
     * Computes a forward slice restricted to a given set of nodes.
     *
     * @param criteria  The slicing criterion.
     * @param back      The restriction.
     * @return          The slice.
     */
    public Collection<SDGNode> subgraphSlice(Collection<SDGNode> criteria, Collection<SDGNode> back) {
        HashMap<SDGNode, Phase> slice = new HashMap<SDGNode, Phase>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        Phase phase1 = phase1();
        Phase phase2 = phase2();


        for (SDGNode v : criteria) {
        	if (back.contains(v)) {
        		worklist.add(v);
        		slice.put(v, phase1);
        	}
        }

        while (!worklist.isEmpty()) {
        	SDGNode next = worklist.poll();
        	Phase currentPhase = slice.get(next);

        	for (SDGEdge e : edgesToTraverse(next)) {
        		if (!e.getKind().isSDGEdge()) continue;

        		SDGNode adjacent = reachedNode(e);
        		if (!back.contains(adjacent)) continue;

        		Phase status = slice.get(adjacent);

        		if (status == null // hasn't been visited before
        				|| (status == phase2 && (currentPhase == phase1 || e.getKind().isThreadEdge()))) {

        			// if we are in phase 1 or e is not a descending edge, traverse e
        			if (currentPhase.follow(e)) {
        				worklist.add(adjacent);

        				// determine how to mark `adjacent'
        				if (currentPhase == phase1 && currentPhase.saveInOtherWorklist(e)) {
        					// standard two-phase slicing: mark adjacent with phase 2
        					slice.put(adjacent, phase2);

        				} else if (currentPhase == phase2 && e.getKind().isThreadEdge()) {
        					// we are in phase 2 and about to traverse an interference edge: mark adjacent with phase 1
        					slice.put(adjacent, phase1);

        				} else {
        					// mark adjacent with the current phase
        					slice.put(adjacent, currentPhase);
        				}
        			}
        		}
        	}
        }

        return slice.keySet();
    }


    /**
     * Zhao algorithm: INCORRECT
     * @author hammer
     */
    private static Collection<SDGNode> sliceMDG(SDG g, Collection<SDGNode> c) {
        Set<SDGNode> slice = new HashSet<SDGNode>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        worklist.addAll(c);
        slice.addAll(c);
        while (!worklist.isEmpty()) {
            SDGNode w = worklist.getLast();
            for (SDGEdge e : g.incomingEdgesOf(w)) {
                if (!e.getKind().isSDGEdge())
                    continue;
                SDGNode v = e.getSource();
                if (!slice.contains(v)) {
                    if (e.getKind() != SDGEdge.Kind.PARAMETER_OUT
                            && e.getKind() != SDGEdge.Kind.FORK_OUT) {

                        worklist.add(v);
                        slice.add(v);
                    }
                }
            }
        }
        worklist.addAll(slice);
        ContextInsensitiveSlicer cis = new ContextInsensitiveBackward(g, SDGSlicer.omittedEdges);
        slice.addAll(cis.slice(worklist));
        return slice;
    }

    /* main- method */

    public static void main(String[] args) throws IOException {
        edu.kit.joana.ifc.sdg.graph.SDG g = edu.kit.joana.ifc.sdg.graph.SDG.readFrom("/home/st/hammer/scratch/pdg/tests.ProducerConsumer.pdg");
        SDGNode c = g.getNode(180);
        Iterative2PhaseSlicer slicer = new I2PBackward(g);
        Collection<SDGNode> slice = slicer.slice(Collections.singleton(c));
        Collection<SDGNode> slice1 = /*SDGSlicer.*/sliceMDG(g, Collections.singleton(c));
        System.out.println(slice.size() + " " + slice1.size());

        System.out.println(slice);
        slice.removeAll(slice1);
        System.out.println(slice);
    }
}
