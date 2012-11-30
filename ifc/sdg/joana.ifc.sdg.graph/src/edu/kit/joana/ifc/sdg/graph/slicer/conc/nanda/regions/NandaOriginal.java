/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.regions;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;


/** An implementation of Nanda's slicer for multi-threaded Java programs.
 * It uses thread regions to model concurrency.
 *
 * -- Created on September 29, 2005
 *
 * @author  Dennis Giffhorn
 */
public class NandaOriginal extends Nanda {

    /** Creates a new instance of this algorithm.
     * @param graph  A SDG.
     */
    public NandaOriginal(SDG graph, NandaMode mode) {
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
        VisitedMap restrictive_1 = new VisitedMap();
        VisitedMap restrictive_2 = new VisitedMap();
        HashSet<WorklistElement> marked = new HashSet<WorklistElement>();

        // init the 3 worklists for this algorithm
        LinkedList<WorklistElement> worklist_1 = new LinkedList<WorklistElement>();
        LinkedList<WorklistElement> worklist_2 = new LinkedList<WorklistElement>();
        LinkedList<WorklistElement> worklist_0 = initWorklist_0(crit, restrictive_1);

        slice.addAll(crit);

        // iterate over a modified 2-phase-slicer until worklist_0 is empty
        while (!worklist_0.isEmpty()) {
            // init the next iteration
            worklist_1.add(worklist_0.poll());

            // === phase 1 ===
            // only ascend to calling procedures
            while (!worklist_1.isEmpty()) {
                WorklistElement next = worklist_1.poll();
                VirtualNode virtual = next.getNode();
                int thread = virtual.getNumber();

                /* placeholder for custom code */
                foo(next);

                for (SDGEdge edge : mode.getEdges(virtual.getNode())) {
                	if (omit(edge)) continue;

                	Nanda.Treatment treatment = mode.phase1Treatment(edge);
                	SDGNode adjacent = mode.adjacentNode(edge);

                	switch (treatment) {
            		case OMIT: break;

            		case THREAD:
            			// eliminate time travels
                        for (int reachedThread : adjacent.getThreadNumbers()) {
            				if (reachedThread == thread && !mhp.isDynamic(thread)) continue;

                            ThreadRegion reachedRegion = mhp.getThreadRegion(adjacent, reachedThread);
                            State tuple = next.getStateOf(reachedRegion.getID());
                            Iterator<TopologicalNumber> validTNRs = realisableInterference(adjacent, reachedThread, tuple, reachedRegion.getID());

                            // add to worklist 0
                            if (validTNRs.hasNext()) {
            					long tmp = worklist_0.size();
                                insert(adjacent, reachedThread, validTNRs, next.getStates(), worklist_0, restrictive_1, slice);
                                elems += (worklist_0.size() - tmp);
                            }
                        }
                        break;

            		case ASCEND:
            			// add to worklist 1
                        if (adjacent.isInThread(thread)) {
                        	Iterator<TopologicalNumber> validTNRs = mode.reachingContexts(adjacent, thread, next.getStateOf(thread));

                            if (validTNRs.hasNext()) {
                                insert(adjacent, thread, validTNRs, next.getStates(), worklist_1, restrictive_1, slice);
                            }
                        }
                        break;

            		case DESCEND:
                        // add to worklist 2
            			Iterator<TopologicalNumber> validTNRs = mode.reachingContexts(adjacent, thread, next.getStateOf(thread));

                        if (validTNRs.hasNext()) {
                			insert2(adjacent, thread, validTNRs, next.getStates(), worklist_2, restrictive_1, restrictive_2, slice);
                        }
                        break;

            		case INTRA:
            			Iterator<TopologicalNumber> nrs = mode.reachingContexts(adjacent, thread, next.getStateOf(thread));
                    	insert(adjacent, thread, nrs, next.getStates(), worklist_1, restrictive_1, slice);
                        break;

            		case CLASS_INITIALIZER:
                        // handle class initialiser
                        handleClassInitialiser(adjacent, 0, next.getStates(), worklist_1, marked, slice);
                        break;

                    default: break; // do nothing
                	}
                }
            }

            // === phase 2 ===
            while (!worklist_2.isEmpty()) {
                WorklistElement next = worklist_2.poll();
                VirtualNode virtual = next.getNode();
                int thread = virtual.getNumber();

                /* placeholder for custom code */
                foo(next);

                for (SDGEdge edge : mode.getEdges(virtual.getNode())) {
            		if (omit(edge)) continue;

                	Nanda.Treatment treatment = mode.phase2Treatment(edge);
                	SDGNode adjacent = mode.adjacentNode(edge);

                	switch (treatment) {
                		case OMIT: break;

                		case THREAD:
                			// eliminate time travels
                			for (int reachedThread : adjacent.getThreadNumbers()) {
                				if (reachedThread == thread && !mhp.isDynamic(thread)) continue;

                                ThreadRegion reachedRegion = mhp.getThreadRegion(adjacent, reachedThread);
                                State tuple = next.getStateOf(reachedRegion.getID());
                                Iterator<TopologicalNumber> validTNRs = realisableInterference(adjacent, reachedThread, tuple, reachedRegion.getID());

                                if (validTNRs.hasNext()) {
                					long tmp = worklist_0.size();
                                    insert(adjacent, reachedThread, validTNRs, next.getStates(), worklist_0, restrictive_1, slice);
                                    elems += (worklist_0.size() - tmp);
                                }
                            }
                            break;

                		case DESCEND:
                            // add to worklist 2
                			Iterator<TopologicalNumber> validTNRs = mode.reachingContexts(adjacent, thread, next.getStateOf(thread));

                			if (validTNRs.hasNext()) {
                				insert2(adjacent, thread, validTNRs, next.getStates(), worklist_2, restrictive_1, restrictive_2, slice);
                			}
                            break;

                		case INTRA:
                			Iterator<TopologicalNumber> nrs = mode.reachingContexts(adjacent, thread, next.getStateOf(thread));
                			insert2(adjacent, thread, nrs, next.getStates(), worklist_2, restrictive_1, restrictive_2, slice);
                			break;

                		case CLASS_INITIALIZER:
                			// add to worklist 2
                			Iterator<TopologicalNumber> validTnrs = mode.reachingContexts(adjacent, thread, next.getStateOf(thread));

                			if (validTnrs.hasNext()) {
                				insert2(adjacent, thread, validTnrs, next.getStates(), worklist_2, restrictive_1, restrictive_2, slice);
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
