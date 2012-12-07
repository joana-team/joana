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
import java.util.Map;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.LevelSlicer;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;


/** An iterated-2-phase level slicer.
 * It counts the minimal number of edge traversals each visited node is away from the slicing criterion.
 *
 * @author Dennis Giffhorn, Christian Hammer
 */
public abstract class I2PLevelSlicer implements LevelSlicer {

	private final Logger debug = Log.getLogger(Log.L_SDG_GRAPH_DEBUG);
    protected SDG g;

    /**
     * Creates a new instance of ContextSensitiveSlicer
     */
    protected I2PLevelSlicer(SDG graph) {
        setGraph(graph);
    }

    public void setGraph(SDG graph) {
        g = graph;
    }

    interface EdgePredicate {
        public boolean phase1();
        public boolean follow(SDGEdge e);
        public boolean saveInOtherWorklist(SDGEdge e);
    }

    public Map<SDGNode, Integer> slice(SDGNode n) {
    	return slice(Collections.singleton(n));
    }

    public Map<SDGNode, Integer> slice(Collection<SDGNode> c) {
        HashMap<SDGNode, SDGNode> slice = new HashMap<SDGNode, SDGNode>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        Map<SDGNode, Integer> level = new HashMap<SDGNode, Integer>();

        EdgePredicate phase = phase1Predicate();
        EdgePredicate nextPhase = phase2Predicate();

        LinkedList<SDGNode> nextWorklist = new LinkedList<SDGNode>();

        worklist.addAll(c);
        for (SDGNode v : c) {
            slice.put(v, phase.phase1() ? v : null);
            level.put(v,0);
        }

        while (!worklist.isEmpty()) {
            while (!worklist.isEmpty()) {
                SDGNode w = worklist.poll();
                assert w.getKind() != SDGNode.Kind.FORMAL_OUT;

                for (SDGEdge e : edgesToTraverse(w)) {
                    if (!e.getKind().isSDGEdge())
                        continue;

                    SDGNode v = reachedNode(e);
                    if (!slice.containsKey(v) ||
                            (slice.get(v) == null && (phase.phase1() || threadEdge(e)))) {
                        // if node was not yet added or node was added in phase2
                        if (phase.saveInOtherWorklist(e)) {
                        	debug.outln(phase.phase1()+" OTHER\t" + e);

                            nextWorklist.add(v);
                            slice.put(v, phase.phase1() ? v : null);
                            level.put(v,(level.get(w)+1));

                        } else if (phase.follow(e)) {
                        	debug.outln(phase.phase1()+" FOLLOW\t" + e);

                            worklist.add(v);
                            slice.put(v, phase.phase1() ? v : null);
                            level.put(v,(level.get(w)+1));
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

        return level;
    }


    public Map<SDGNode, Integer> slice(SDGNode n, int maxSteps) {
    	return slice(Collections.singleton(n), maxSteps);
    }

    public Map<SDGNode, Integer> slice(Collection<SDGNode> c, int maxSteps) {
        HashMap<SDGNode, SDGNode> slice = new HashMap<SDGNode, SDGNode>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        Map<SDGNode, Integer> level = new HashMap<SDGNode, Integer>();

        EdgePredicate phase = phase1Predicate();
        EdgePredicate nextPhase = phase2Predicate();

        LinkedList<SDGNode> nextWorklist = new LinkedList<SDGNode>();

        worklist.addAll(c);
        for (SDGNode v : c) {
            slice.put(v, phase.phase1() ? v : null);
            level.put(v,0);
        }

        while (!worklist.isEmpty()) {
            while (!worklist.isEmpty()) {
                SDGNode w = worklist.poll();

                if (level.get(w) == maxSteps) continue;

                for (SDGEdge e : edgesToTraverse(w)) {
                    if (!e.getKind().isSDGEdge())
                        continue;

                    SDGNode v = reachedNode(e);
                    if (!slice.containsKey(v) ||
                            (slice.get(v) == null && (phase.phase1() || threadEdge(e)))) {
                        // if node was not yet added or node was added in phase2
                        if (phase.saveInOtherWorklist(e)) {
                        	debug.outln(phase.phase1()+" OTHER\t" + e);

                            nextWorklist.add(v);
                            slice.put(v, phase.phase1() ? v : null);
                            level.put(v,(level.get(w)+1));

                        } else if (phase.follow(e)) {
                        	debug.outln(phase.phase1()+" FOLLOW\t" + e);

                            worklist.add(v);
                            slice.put(v, phase.phase1() ? v : null);
                            level.put(v,(level.get(w)+1));
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

        return level;
    }

    private boolean threadEdge(SDGEdge e) {
        return e.getKind().isSDGEdge() && e.getKind().isThreadEdge();
    }

    protected abstract Collection<SDGEdge> edgesToTraverse(SDGNode node);

    protected abstract SDGNode reachedNode(SDGEdge edge);

    protected abstract EdgePredicate phase1Predicate();

    protected abstract EdgePredicate phase2Predicate();
}
