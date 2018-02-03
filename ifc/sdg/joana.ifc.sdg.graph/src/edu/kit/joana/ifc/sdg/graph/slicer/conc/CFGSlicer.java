/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;


/**
 * This slicer slices CFG's for concurrent programs context-sensitively.
 * Employable for simple reachability analyses.
 *
 * @author hammer, giffhorn
 */
public abstract class CFGSlicer {
    interface Phase {
        public boolean follow(SDGEdge e);
        public boolean saveInOtherWorklist(SDGEdge e);
    }

    protected CFG g;
    private Set<SDGEdge.Kind> omittedEdges = new HashSet<SDGEdge.Kind>();

    protected abstract Iterable<SDGEdge> edgesToTraverse(SDGNode node);

    protected abstract SDGNode reachedNode(SDGEdge edge);

    protected abstract Phase phase1();

    protected abstract Phase phase2();

    /**
     * Creates a new instance of ContextSensitiveSlicer
     */
    protected CFGSlicer(CFG g) {
        this.g = g;
    }

    protected CFGSlicer(SDG sdg) {
        this.g = ICFGBuilder.extractICFG(sdg);
    }

    public void setGraph(CFG graph) {
        g = graph;
    }

    public void setOmittedEdges(Set<SDGEdge.Kind> omit){
        this.omittedEdges = omit;
    }

    public Collection<SDGNode> slice(SDGNode c) {
    	return slice(Collections.singleton(c));
    }

    private static void slice_put(SDGNode n, Phase phase) {
    	n.customData = phase;
    }
    
    private static Phase slice_get(SDGNode n) {
    	return (Phase) n.customData;
    }
    
    public Collection<SDGNode> slice(Collection<SDGNode> c) {
        HashMap<SDGNode, Phase> slice = new HashMap<SDGNode, Phase>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        final Phase phase1 = phase1();
        final Phase phase2 = phase2();
        
        boolean assertionEnabled = false;
        assert (assertionEnabled = true);

        for (SDGNode n : g.vertexSet()) {
        	slice_put(n, null);
        }
        
        worklist.addAll(c);

        for (SDGNode v : c) {
            assert slice.put(v, phase1) == null;
            slice_put(v, phase1);
        }

        while (!worklist.isEmpty()) {
            SDGNode next = worklist.poll();
            Phase  currentPhase =  slice_get(next);
            assert currentPhase == slice.get(next);

            for (SDGEdge e : edgesToTraverse(next)) {
            	if (omittedEdges.contains(e.getKind())) continue;

            	// TODO: why isn't e.getKind().isControlFlowEdge() used?
            	// i.e.: why don't we follow SDGEdge.Kind.NO_FLOW or SDGEdge.Kind.JUMP_FLOW??
                if (e.getKind() != SDGEdge.Kind.CONTROL_FLOW
                		&& e.getKind() != SDGEdge.Kind.CALL
                		&& e.getKind() != SDGEdge.Kind.RETURN
                		&& e.getKind() != SDGEdge.Kind.FORK
                		&& e.getKind() != SDGEdge.Kind.JOIN)
                    continue;

        		SDGNode adjacent = reachedNode(e);
        		Phase  status =  slice_get(adjacent);
        		assert status == slice.get(adjacent);
        		if (status == null // hasn't been visited before
        				|| (status == phase2 && (currentPhase == phase1 || e.getKind().isThreadEdge()))) {

        			// if we are in phase 1 or e is not a descending edge, traverse e
        			if (currentPhase.follow(e)) {
        				worklist.add(adjacent);

        				// determine how to mark `adjacent'
        				if (currentPhase == phase1 && currentPhase.saveInOtherWorklist(e)) {
        					// standard two-phase slicing: mark adjacent with phase 2
        					if (assertionEnabled) {
        						slice.put(adjacent, phase2);
        					} {
        						slice_put(adjacent, phase2);
        					}

        				} else if (currentPhase == phase2 && e.getKind().isThreadEdge()) {
        					// we are in phase 2 and about to traverse an interference edge: mark adjacent with phase 1
        					if (assertionEnabled) {
        						slice.put(adjacent, phase1);
        					} {
        						slice_put(adjacent, phase1);
        					}
        				} else {
        					// mark adjacent with the current phase
        					if (assertionEnabled) {
        						slice.put(adjacent, currentPhase);
        					} {
        						slice_put(adjacent, currentPhase);
        					}
        					
        				}
        			}
        		}
            }
        }
		final Set<SDGNode> result =
				g.vertexSet()
				 .stream()
				 .filter(n -> n.customData != null)
				 .collect(Collectors.toSet());
		
		assert slice.keySet().equals(result);
        return result;
    }

    public Collection<SDGNode> subgraphSlice(Collection<SDGNode> c, Collection<SDGNode> sub) {
    	HashMap<SDGNode, Phase> slice = new HashMap<SDGNode, Phase>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        Phase phase1 = phase1();
        Phase phase2 = phase2();

        worklist.addAll(c);

        for (SDGNode v : c) {
            if (sub.contains(v)) {
            	slice.put(v, phase1);
            }
        }

        while (!worklist.isEmpty()) {
            SDGNode next = worklist.poll();
        	Phase currentPhase = slice.get(next);

            for (SDGEdge e : edgesToTraverse(next)) {
            	if (omittedEdges.contains(e.getKind())) continue;

                if (e.getKind() != SDGEdge.Kind.CONTROL_FLOW
                		&& e.getKind() != SDGEdge.Kind.CALL
                		&& e.getKind() != SDGEdge.Kind.RETURN
                		&& e.getKind() != SDGEdge.Kind.FORK
                		&& e.getKind() != SDGEdge.Kind.JOIN)
                    continue;

        		SDGNode adjacent = reachedNode(e);

        		if (!sub.contains(adjacent)) continue;

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
}
