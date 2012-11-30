/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.barrier;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/** A 2-phase barrier slicer.
 * A barrier slicer receives a set of nodes - the barrier - which it will not trespass.
 *
 * -- Created on September 6, 2005
 *
 * @author  Kai Brueckner, Dennis Giffhorn
 */
public abstract class CSBarrierSlicer implements BarrierSlicer {
    interface EdgePredicate {
        public boolean phase1();
        public boolean follow(SDGEdge e);
        public boolean saveInOtherWorklist(SDGEdge e);
    }

    protected SDG g;
    private Collection<SDGEdge.Kind> omit;
    private BarrierManager barrier;

    /* abstract methods */
    protected abstract Collection<SDGEdge> edgesToTraverse(SDGNode node);

    protected abstract SDGNode reachedNode(SDGEdge edge);

    protected abstract EdgePredicate phase1Predicate();

    protected abstract EdgePredicate phase2Predicate();


    public CSBarrierSlicer(SDG graph) {
    	omit = Collections.emptySet();
    	barrier = new BarrierManager();
        setGraph(graph);
    }

    public void setGraph(SDG graph) {
        g = graph;
    }

    public void setOmittedEdges(Set<SDGEdge.Kind> omit){
        this.omit = omit;
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

    public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
    	Map<SDGNode, SDGNode> slice = new HashMap<SDGNode, SDGNode>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        LinkedList<SDGNode> nextWorklist = new LinkedList<SDGNode>();
        EdgePredicate p = phase1Predicate();

        worklist.addAll(criteria);

        for (SDGNode v : criteria) {
            slice.put(v, phase1Predicate().phase1() ? v : null);
        }

        while (!worklist.isEmpty()) {
            while (!worklist.isEmpty()) {
                SDGNode w = worklist.poll();

                for (SDGEdge e : edgesToTraverse(w)) {
                    // dont't leave the thread and don't traverse blocked summary edges
                    if (!e.getKind().isSDGEdge()
                    		|| e.getKind().isThreadEdge()
                    		|| omit.contains(e.getKind())
                            || barrier.isBlocked(e)) {

                        continue;
                    }

                    SDGNode v = reachedNode(e);

                    // don't trespass the barrier
                    if (barrier.isBlocked(v)) continue;

                    if (!slice.containsKey(v) || (p.phase1() && slice.get(v) == null)){

                        // if node was not yet added or node was added in phase2
                        if (p.saveInOtherWorklist(e)) {
                            slice.put(v, p.phase1() ? v : null);
                            nextWorklist.add(v);

                        } else if (p.follow(e)) {
                            slice.put(v, p.phase1() ? v : null);
                            worklist.add(v);
                        }
                    }
                }
            }

            // swap worklists and predicates
            worklist = nextWorklist;
            p =  phase2Predicate();
        }

        return slice.keySet();
    }

    public Collection<SDGNode> subgraphSlice(Collection<SDGNode> criteria, Collection<SDGNode> sub) {
    	Map<SDGNode, SDGNode> slice = new HashMap<SDGNode, SDGNode>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        LinkedList<SDGNode> nextWorklist = new LinkedList<SDGNode>();
        EdgePredicate p = phase1Predicate();

        for (SDGNode v : criteria) {
        	if (sub.contains(v)) {
        		worklist.add(v);
        		slice.put(v, phase1Predicate().phase1() ? v : null);
        	}
        }

        while (!worklist.isEmpty()) {
            while (!worklist.isEmpty()) {
                SDGNode w = worklist.poll();

                for (SDGEdge e : edgesToTraverse(w)) {
                    // dont't leave the thread and don't traverse blocked summary edges
                    if (!e.getKind().isSDGEdge()
                    		|| e.getKind().isThreadEdge()
                    		|| omit.contains(e.getKind())
                            || barrier.isBlocked(e)) {

                        continue;
                    }

                    SDGNode v = reachedNode(e);

                    // don't trespass the barrier and don't leave the sub-graph
                    if (barrier.isBlocked(v) || !sub.contains(v)) continue;

                    if (!slice.containsKey(v) || (p.phase1() && slice.get(v) == null)){

                        // if node was not yet added or node was added in phase2
                        if (p.saveInOtherWorklist(e)) {
                            slice.put(v, p.phase1() ? v : null);
                            nextWorklist.add(v);

                        } else if (p.follow(e)) {
                            slice.put(v, p.phase1() ? v : null);
                            worklist.add(v);
                        }
                    }
                }
            }

            // swap worklists and predicates
            worklist = nextWorklist;
            p =  phase2Predicate();
        }

        return slice.keySet();
    }
}
