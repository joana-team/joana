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
import java.util.Iterator;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/** An implementation of Nanda's slicer for multi-threaded Java programs.
 * Uses reachability analysis upon each edge traversal.
 *
 * -- Created on September 29, 2005
 *
 * @author  Dennis Giffhorn
 */
public class NandaReach extends Nanda {

    /** Creates a new instance of this algorithm.
     * @param graph  A SDG.
     */
    public NandaReach(SDG graph, NandaMode mode) {
        super(graph, mode);
    }

    /**
     * Executes Nanda's slicing algorithm for a given set of slicing criteria.
     * Returns the computed slice as a sorted set of nodes.
     *
     * @param criteria  The slicing criteria.
     * @return          The slice.
     */
    protected Collection<SDGNode> nandaSlice(Collection<SDGNode> crit) {
    	HashSet<SDGNode> slice = new HashSet<SDGNode>();
        restrictive_1 = new VisitedMap();
        restrictive_2 = new VisitedMap();

        // init the 3 worklists for this algorithm
        LinkedList<WorklistElement> worklist_1 = new LinkedList<WorklistElement>();
        LinkedList<WorklistElement> worklist_2 = new LinkedList<WorklistElement>();
        LinkedList<WorklistElement> worklist_0 = initWorklist_0(crit, slice);

        // iterate over a modified 2-phase-slicer until worklist_0 is empty
        while (!worklist_0.isEmpty()) {
            // init the next iteration
            worklist_1.add(worklist_0.poll());

            // === phase 1 ===
            // only ascend to calling procedures
            while (!worklist_1.isEmpty()) {
                WorklistElement next = worklist_1.poll();
                SDGNode node = next.getNode();
                int thread = next.getThread();

                /* placeholder for custom code */
                foo(next);

                for (SDGEdge edge : mode.getEdges(node)) {
                	if (omit(edge)) continue;

                	Nanda.Treatment treatment = mode.phase1Treatment(edge);
                	SDGNode adjacent = mode.adjacentNode(edge);

                	switch (treatment) {
                	case OMIT: break;

                	case THREAD:
                		// eliminate time travels
                		for (int reachedThread : adjacent.getThreadNumbers()) {
            				if (reachedThread == thread && !mhp.isDynamic(thread)) continue;

            				TopologicalNumber tuple = next.getStateOf(reachedThread);
                			Iterator<TopologicalNumber> validTNRs = realisableInterference(adjacent, reachedThread, tuple);

                			// add to worklist 0
                			if (validTNRs.hasNext()) {
                				insert(false, adjacent, reachedThread, validTNRs, next.getStates(), worklist_0, slice);
                			}
                		}
                		break;

                	case ASCEND:
                		// add to worklist 1
                		if (adjacent.isInThread(thread)) {
                			Iterator<TopologicalNumber> validTNRs = mode.reachingContexts(adjacent, thread, next.getStateOf(thread));

                			if (validTNRs.hasNext()) {
                				insert(false, adjacent, thread, validTNRs, next.getStates(), worklist_1, slice);
                			}
                		}
                		break;

                	case DESCEND:
                		// add to worklist 2
                		Iterator<TopologicalNumber> validTNRs = mode.reachingContexts(adjacent, thread, next.getStateOf(thread));

                		if (validTNRs.hasNext()) {
                			insert(true, adjacent, thread, validTNRs, next.getStates(), worklist_2, slice);
                		}
                		break;

                	case INTRA:
                		Iterator<TopologicalNumber> nrs = mode.reachingContexts(adjacent, thread, next.getStateOf(thread));
                		insert(false, adjacent, thread, nrs, next.getStates(), worklist_1, slice);
                		break;

                	case CLASS_INITIALIZER:
                		// class initializer - all contexts are valid.
                        Iterator<TopologicalNumber> tnrs = mode.getTopologicalNumbers(adjacent, 0);
                		insert(false, adjacent, thread, tnrs, next.getStates(), worklist_1, slice);
                		break;

                	default: break; // do nothing
                	}
                }
            }

            // === phase 2 ===
            while (!worklist_2.isEmpty()) {
            	WorklistElement next = worklist_2.poll();
            	SDGNode node = next.getNode();
            	int thread = next.getThread();

            	/* placeholder for custom code */
            	foo(next);

            	for (SDGEdge edge : mode.getEdges(node)) {
            		if (omit(edge)) continue;

            		Nanda.Treatment treatment = mode.phase2Treatment(edge);
            		SDGNode adjacent = mode.adjacentNode(edge);

            		switch (treatment) {
            		case OMIT: break;

            		case THREAD:
            			// eliminate time travels
            			for (int reachedThread : adjacent.getThreadNumbers()) {
            				if (reachedThread == thread && !mhp.isDynamic(thread)) continue;

            				TopologicalNumber tuple = next.getStateOf(reachedThread);
            				Iterator<TopologicalNumber> validTNRs = realisableInterference(adjacent, reachedThread, tuple);

            				if (validTNRs.hasNext()) {
            					insert(false, adjacent, reachedThread, validTNRs, next.getStates(), worklist_0, slice);
            				}
            			}
            			break;

            		case DESCEND:
            			// add to worklist 2
            			Iterator<TopologicalNumber> validTNRs = mode.reachingContexts(adjacent, thread, next.getStateOf(thread));

            			if (validTNRs.hasNext()) {
            				insert(true, adjacent, thread, validTNRs, next.getStates(), worklist_2, slice);
            			}
            			break;

            		case INTRA:
            			Iterator<TopologicalNumber> nrs = mode.reachingContexts(adjacent, thread, next.getStateOf(thread));
            			insert(true, adjacent, thread, nrs, next.getStates(), worklist_2, slice);
            			break;

            		case CLASS_INITIALIZER:
            			// add to worklist 2
            			Iterator<TopologicalNumber> validTnrs = mode.reachingContexts(adjacent, thread, next.getStateOf(thread));

            			if (validTnrs.hasNext()) {
            				insert(true, adjacent, thread, validTnrs, next.getStates(), worklist_2, slice);
            			}
            			break;

            		default: break; // do nothing
            		}
                }
            }
        }

        return slice;
    }
}
