/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.barrier.conc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.BarrierManager;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.BarrierSlicer;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;


/** An iterated-2-phase barrier slicer.
 * A barrier slicer receives a set of nodes - the barrier - which it will not trespass.
 *
 * @author Dennis Giffhorn, Christian Hammer
 */
public abstract class I2PBarrierSlicer implements BarrierSlicer {

	private final Logger debug = Log.getLogger(Log.L_SDG_GRAPH_DEBUG);
	
    interface EdgePredicate {
        public boolean phase1();
        public boolean follow(SDGEdge e);
        public boolean saveInOtherWorklist(SDGEdge e);
    }

    protected SDG g;
    private BarrierManager barrier;

    protected abstract Collection<SDGEdge> edgesToTraverse(SDGNode node);

    protected abstract SDGNode reachedNode(SDGEdge edge);

    protected abstract EdgePredicate phase1Predicate();

    protected abstract EdgePredicate phase2Predicate();

    /**
     * Creates a new instance of ContextSensitiveSlicer
     */
    protected I2PBarrierSlicer(SDG graph) {
    	barrier = new BarrierManager();
        setGraph(graph);
    }

    public void setGraph(SDG graph) {
        g = graph;
    }

	public void setBarrier(Collection<SDGNode> barrier) {
		this.barrier.setBarrier(g, barrier);
	}

	public void setBarrier(Collection<SDGNode> barrier, Collection<SDGEdge> blockedSummaryEdges) {
		this.barrier.setBarrier(barrier, blockedSummaryEdges);
	}

	public void setBarrier(BarrierManager barrier) {
		this.barrier = barrier;
	}

	public Collection<SDGNode> slice(SDGNode criterion) {
		return slice(Collections.singleton(criterion));
	}

    public Collection<SDGNode> slice(Collection<SDGNode> c) {
        HashMap<SDGNode, SDGNode> slice = new HashMap<SDGNode, SDGNode>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();

        EdgePredicate phase = phase1Predicate();
        EdgePredicate nextPhase = phase2Predicate();

        LinkedList<SDGNode> nextWorklist = new LinkedList<SDGNode>();

        worklist.addAll(c);
        for (SDGNode v : c) {
            slice.put(v, phase.phase1() ? v : null);
        }

        while (!worklist.isEmpty()) {
            while (!worklist.isEmpty()) {
                SDGNode w = worklist.poll();

                for (SDGEdge e : edgesToTraverse(w)) {
                    // don't traverse blocked summary edges
                    if (!e.getKind().isSDGEdge() || barrier.isBlocked(e))
                        continue;

                    SDGNode v = reachedNode(e);

                    // don't cross the barrier
                    if (barrier.isBlocked(v)) continue;

                    if (!slice.containsKey(v) ||
                            (slice.get(v) == null && (phase.phase1() || e.getKind().isThreadEdge()))) {
                        // if node was not yet added or node was added in phase2
                        if (phase.saveInOtherWorklist(e)) {
                            debug.outln(phase.phase1()+" OTHER\t" + e);

                            nextWorklist.add(v);
                            slice.put(v, phase.phase1() ? v : null);

                        } else if (phase.follow(e)) {
                        	debug.outln(phase.phase1()+" FOLLOW\t" + e);

                            worklist.add(v);
                            slice.put(v, phase.phase1() ? v : null);
                        }
                    }
                }
            }
            // swap worklists and predicates
            debug.outln("swap");

            LinkedList<SDGNode> tmp = worklist;
            worklist = nextWorklist;
            nextWorklist = tmp;
            EdgePredicate p = phase;
            phase = nextPhase;
            nextPhase = p;
        }

        return slice.keySet();
    }

    public Collection<SDGNode> subgraphSlice(Collection<SDGNode> c, Collection<SDGNode> sub) {
        HashMap<SDGNode, SDGNode> slice = new HashMap<SDGNode, SDGNode>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();

        EdgePredicate phase = phase1Predicate();
        EdgePredicate nextPhase = phase2Predicate();

        LinkedList<SDGNode> nextWorklist = new LinkedList<SDGNode>();

        for (SDGNode v : c) {
        	if (sub.contains(v)) {
        		worklist.add(v);
        		slice.put(v, phase.phase1() ? v : null);
        	}
        }

        while (!worklist.isEmpty()) {
            while (!worklist.isEmpty()) {
                SDGNode w = worklist.poll();

                for (SDGEdge e : edgesToTraverse(w)) {
                    // don't traverse blocked summary edges
                    if (!e.getKind().isSDGEdge() || barrier.isBlocked(e))
                        continue;

                    SDGNode v = reachedNode(e);

                    // don't cross the barrier and don't leav the sub-graph
                    if (barrier.isBlocked(v) || !sub.contains(v)) continue;

                    if (!slice.containsKey(v) ||
                            (slice.get(v) == null && (phase.phase1() || e.getKind().isThreadEdge()))) {
                        // if node was not yet added or node was added in phase2
                        if (phase.saveInOtherWorklist(e)) {
                        	debug.outln(phase.phase1()+" OTHER\t" + e);

                            nextWorklist.add(v);
                            slice.put(v, phase.phase1() ? v : null);

                        } else if (phase.follow(e)) {
                        	debug.outln(phase.phase1()+" FOLLOW\t" + e);

                            worklist.add(v);
                            slice.put(v, phase.phase1() ? v : null);
                        }
                    }
                }
            }
            // swap worklists and predicates
            debug.outln("swap");

            LinkedList<SDGNode> tmp = worklist;
            worklist = nextWorklist;
            nextWorklist = tmp;
            EdgePredicate p = phase;
            phase = nextPhase;
            nextPhase = p;
        }

        return slice.keySet();
    }
}
