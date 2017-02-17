/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.regions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;


/** An implementation of Nanda's slicer for multithreaded Java programs.
 * It uses an optimization to omit reachability analysis after traversing
 * intra-procedural edges
 *
 * Works only with SDGs created with jSDG version 1412.
 * Later versions produce inappropriate control flow graphs and cause NullPointerExceptions.
 * Note: Even with SDGs created with jSDG version 1412 you may have to manually remove
 * nodes not reachable via control flow by the procedure entry and nodes not reaching
 * the exit of its procedure via control flow.
 *
 * -- Created on September 29, 2005
 *
 * @author  Dennis Giffhorn
 */
public class Nanda implements Slicer {
	public enum Treatment {
		OMIT,
		THREAD,
		ASCEND,
		DESCEND,
		INTRA,
		CLASS_INITIALIZER;
	}

	public static long elems = 0L;
	protected static boolean TIME_TRAVELS = true;

    // === fields ===
//	private boolean debug;

    /** The dependence graph. */
    protected SDG graph;

    /** Forward or backward slice. */
    protected NandaMode mode;

    /** The thread regions the graph consists of. */
    protected MHPAnalysis mhp;

    /** The context graphs. */
    protected ContextGraphs contextGraphs;

    /** A summary slicer. */
    protected SummarySlicer summarySlicer;

    protected Iterator<TopologicalNumber> emptyIterator;

	private ThreadRegionsReach reach;

    /** Creates a new instance of this algorithm.
     * @param graph  A SDG.
     */
    public Nanda(SDG graph, NandaMode mode) {
        this.mode = mode;
        Collection<TopologicalNumber> tmp = Collections.emptySet();
        emptyIterator = tmp.iterator();

        if (graph != null) setGraph(graph);
    }

    public Nanda(Nanda n, NandaMode mode) {
        this.mode = mode;
        this.graph = n.graph;
        this.mhp = n.mhp;
        this.contextGraphs = n.contextGraphs;
        this.mode.init(contextGraphs, mhp, graph);
        summarySlicer = this.mode.initSummarySlicer(this.graph);
        Collection<TopologicalNumber> tmp = Collections.emptySet();
        emptyIterator = tmp.iterator();
    }

    /* predefined methods */

    /**
	 * @param edge The edge which shall be tested for omission.
	 */
    protected boolean omit(SDGEdge edge) {
    	return false;
    }

    /**
	 * @param next The worklist element which shall be processed.
	 */
    protected void foo(WorklistElement next) {

    }

    protected MHPAnalysis mhp() {
    	return PreciseMHPAnalysis.analyze(graph);
    }

    /* end of predefined methods */


    public void setGraph(SDG graph) {
        this.graph = graph;

        // build the threaded ICFG
        CFG cfg = ICFGBuilder.extractICFG(this.graph);

        // compute the ISCR graphs
        contextGraphs = ContextGraphs.build(cfg);

        // compute thread regions and parallelism relation
        mhp = mhp();

        // initialize the slicing mode
        mode.init(contextGraphs, mhp, graph);

        summarySlicer = mode.initSummarySlicer(graph);

        reach = ThreadRegionsReach.create(mhp.getThreadRegions(), graph);
    }

    public Collection<SDGNode> slice(SDGNode criterion) {
    	return slice(Collections.singleton(criterion));
    }

    /** Executes Nanda's slicing algorithm for a given set of slicing criteria.
     * Returns the computed slice as a sorted set of nodes.
     *
     * @param criteria  The slicing criteria.
     *
     * @return          The slice.
     */
    public Collection<SDGNode> slice(Collection<SDGNode> crit) {
        HashSet<SDGNode> slice = new HashSet<SDGNode>();
        Collection<SDGNode> interferingNodes = summarySlicer.slice(crit, slice);

        if (interferingNodes.isEmpty()) {
        	elems++;
            return slice;

        } else {
            return nandaSlice(crit);
        }
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
        while (!worklist_0.isEmpty()) {elems++;
            // init the next iteration
            worklist_1.add(worklist_0.poll());

            // === phase 1 ===
            // only ascend to calling procedures
            while (!worklist_1.isEmpty()) {
                WorklistElement next = worklist_1.poll();
                VirtualNode virtual = next.getNode();
                int thread = virtual.getNumber();
                TopologicalNumber tnr = next.getTopolNr();

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
//            					long tmp = worklist_0.size();
                                insert(adjacent, reachedThread, validTNRs, next.getStates(), worklist_0, restrictive_1, slice);
//                                elemsTR += (worklist_0.size() - tmp);
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
            			Iterator<TopologicalNumber> nrs = mode.intraproceduralNeighbours(adjacent, tnr);
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
                TopologicalNumber tnr = next.getTopolNr();

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
//                					long tmp = worklist_0.size();
                                    insert(adjacent, reachedThread, validTNRs, next.getStates(), worklist_0, restrictive_1, slice);
//                                    elemsTR += (worklist_0.size() - tmp);
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
                			Iterator<TopologicalNumber> nrs = mode.intraproceduralNeighbours(adjacent, tnr);
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

    /** Annotates the valid contexts of a reached node with updated state tuples
     * and inserts them into a worklist.
     * @param source  The reached node.
     * @param sourceRegion  The thread region of source.
     * @param mu  The valid contexts.
     * @param oldStates  The current state tuple.
     * @param worklist  The worklist where the annotated contexts shall be inserted.
     */
    protected void insert(SDGNode reached, int thread, Iterator<TopologicalNumber> mu,
            States oldStates, LinkedList<WorklistElement> worklist, VisitedMap restrictive,
            HashSet<SDGNode> slice) {

        VirtualNode v = new VirtualNode(reached, thread);

        // for all valid contexts of source ...
        while (mu.hasNext()) {
        	TopologicalNumber m = mu.next();

            // ... clone the current state tuple and update the clone ...
            States newStates = update(oldStates, v, m, thread);

            // ... run the restrictive state tuple optimization ...
            if (optimise(v, m, newStates, restrictive)){
                continue;
            }

            // ... annotate the remaining contexts with the updated state tuple ...
            WorklistElement newElement = new WorklistElement(v, m, newStates);

            // ... add them to the worklist ...
            worklist.addLast(newElement);

            // ... and put them to the slicing result
            slice.add(reached);
            restrictive.put(v, m.getNumber(), newStates);
        }
    }

    protected void insert2(SDGNode reached, int thread, Iterator<TopologicalNumber> mu,
            States oldStates, LinkedList<WorklistElement> worklist, VisitedMap restrictive_1, VisitedMap restrictive_2,
            HashSet<SDGNode> slice) {

        VirtualNode v = new VirtualNode(reached, thread);

        // for all valid contexts of source ...
        while (mu.hasNext()) {
        	TopologicalNumber m = mu.next();

            // ... clone the current state tuple and update the clone ...
            States newStates = update(oldStates, v, m, thread);

            // ... run the restrictive state tuple optimization ...
            if (optimise(v, m, newStates, restrictive_1)){
                continue;
            }

            if (optimise(v, m, newStates, restrictive_2)){
                continue;
            }

            // ... annotate the remaining contexts with the updated state tuple ...
            WorklistElement newElement = new WorklistElement(v, m, newStates);

            // ... add them to the worklist ...
            worklist.addLast(newElement);

            // ... and put them to the slicing result
            slice.add(reached);
            restrictive_2.put(v, m.getNumber(), newStates);
        }
    }

    /** Entering a class initializer needs a special treatment in C. Hammers SDG format.
     * Method is called when a class initializer is entered via an param-in edge.
     * @param newNode  The reached node.
     * @param newNodeRegion  Its thread region.
     * @param states  The current state tuple.
     * @param worklist  The worklist where the created elements shall be inserted.
     */
    protected final void handleClassInitialiser(SDGNode newNode, int thread, States states,
            LinkedList<WorklistElement> worklist, HashSet<WorklistElement> marked, HashSet<SDGNode> slice) {

        // get all contexts of new node - all contexts are valid.
        VirtualNode v = new VirtualNode(newNode, thread);
        Iterator<TopologicalNumber> tnrs = mode.getTopologicalNumbers(newNode, thread);

        // iterate over all contexts
        while (tnrs.hasNext()) {
        	TopologicalNumber tnt = tnrs.next();

            // clone the current state tuple ...
        	States newStates = update(states, v, tnt, thread);

            // annotate the context with the updated state tuple
            WorklistElement elem = new WorklistElement(v, tnt, newStates);

            // add the annotated context to the worklist and to the slice
            if (marked.add(elem)) {
                worklist.addLast(elem);
                slice.add(newNode);
            }
        }
    }

    /** Implements Nanda's restrictive state tuple optimization.
     * @param source  The reached node.
     * @param thread  The current thread.
     * @param m  The context of source.
     * @param toCheck  The state tuple to check against restrictiveness.
     * @return  true if toCheck is a restrictive state tuple.
     */
    protected final boolean optimise(VirtualNode reached, TopologicalNumber m,
            States toCheck, VisitedMap restrictive) {

        // get the state tuples of m so far
        List<States> marks = restrictive.get(reached, m.getNumber());

        if (marks == null) {
            return false;
        }

        // for all previous states of m ...
        for (States prev : marks) {
            boolean redundant = true;

            // ... iterate over all single states ...
            for (int i = 0; i < toCheck.size(); i++) {
                // ... if the state in toCheck can not reach the state of prev ...
                if (!isRestrictive(toCheck.get(i), prev.get(i))) {
                    // ... toCheck is not restrictive
                    redundant = false;
                    break;
                }
            }

            // ... else it is restrictive
            if (redundant) {
                return true;
            }
        }

        return false;
    }

    protected final LinkedList<WorklistElement> initWorklist_0(Collection<SDGNode> criteria, VisitedMap restrictive) {
        // create a new worklist
        LinkedList<WorklistElement> worklist_0 = new LinkedList<WorklistElement>();
        // ... and create an initial state tuple
        States s = createInitialStates();

        // for all slicing criteria ...
        for (SDGNode criterion : criteria) {
            // .. get its threads ...
            int[] threads = criterion.getThreadNumbers();

            // for all of its threads ...
            for (int t : threads) {
                // ... get all of its contexts
                Iterator<TopologicalNumber> mu = mode.getTopologicalNumbers(criterion, t);
                VirtualNode v = new VirtualNode(criterion, t);

                // for all these contexts ...
                while (mu.hasNext()) {
                	TopologicalNumber tnt = mu.next();
                    // ... clone the initial state tuple and update the clone...
                    States newStates = update(s, v, tnt, t);

                    // ... run the restrictive state tuple optimization ...
                    if (optimise(v, tnt, newStates, restrictive)){
                        continue;
                    }

                    // ... annotate the context with the state tuple ...
                    WorklistElement newElement = new WorklistElement(v, tnt, newStates);

                    // ... add it to the worklist ...
                    worklist_0.addLast(newElement);
                    restrictive.put(v, tnt.getNumber(), newStates);
                }
            }
        }

        // return the initial worklist
        return worklist_0;
    }

    protected States createInitialStates() {
    	States s = new States(mhp.getThreadRegions().size());
        LinkedList<VirtualNode> initialThreadStates = new LinkedList<VirtualNode>();

        for (int x = 0; x < graph.getNumberOfThreads(); x++) {
            VirtualNode vn = mode.initialState(x);
            initialThreadStates.addLast(vn);
        }

        for (ThreadRegion r : mhp.getThreadRegions()) {
            VirtualNode vn = initialThreadStates.get(r.getThread());
            s.setInitialState(vn, r.getID(), null);
        }

    	return s;
    }

    protected States update(States s, VirtualNode v, TopologicalNumber tnt, int thread) {
    	States newStates = s.clone();

        for (int x = 0; x < newStates.size(); x++) {
        	if (!mhp.isDynamic(thread) && mhp.getThreadRegion(x).getThread() == thread) {
                newStates.setState(v, x, tnt);

        	} else if (!mhp.isParallel(v.getNode(), thread, x)) {
                newStates.setState(v, x, tnt);
            }
        }

        return newStates;
    }

    /** Implements the RealizablePath' function of Nanda's algorithm.
     * Determines the valid contexts of a given node.
     * @param from  The node.
     * @param fromRegion  Its thread region.
     * @param state  The current state tuple.
     */
    protected Iterator<TopologicalNumber> realisableInterference(SDGNode reached, int reachedThread, State state) {
		if (state == State.NONRESTRICTIVE || !TIME_TRAVELS) {
			// in case time travel detection is disabled or the state is nonrestrictive, all contexts are valid
			return mode.getTopologicalNumbers(reached, reachedThread);

		} else if (state == State.NONE) {
			// the reached thread is inactive
			return emptyIterator;

		} else {
			// commit a reaching analysis
			return mode.reachingContexts(reached, reachedThread, state);
		}
    }

    protected boolean isRestrictive(State toCheck, State old) {
    	if (old.getTopolNr() == null) {
    		return true;

    	} else if (toCheck.getTopolNr() == null) {
    		return false;

    	} else if (toCheck.getTopolNr().getThread() == old.getTopolNr().getThread()) {
    		// commit a reaching analysis
    		return mode.restrictiveTest(toCheck, old);
    	} else {
    		ThreadRegion from = mhp.getThreadRegion(toCheck.getActualNode());
    		ThreadRegion to = mhp.getThreadRegion(old.getActualNode());
    		return reach.reaches(from.getID(), to.getID()); // TODO: hardcoded for backward slice!
    	}
    }

    /** Implements the RealizablePath' function of Nanda's algorithm.
     * Determines the valid contexts of a given node.
     * @param from  The node.
     * @param fromRegion  Its thread region.
     * @param state  The current state tuple.
     */
    protected Iterator<TopologicalNumber> realisableInterference(SDGNode reached, int reachedThread, State state, int reachedRegion) {
		if (mhp.isDynamic(reachedThread) || state.getTopolNr() == null || !TIME_TRAVELS) {
        	 // if the reached thread is dynamic or the state is still the initial one, all contexts are valid
            return mode.getTopologicalNumbers(reached, reachedThread);

        } else if (reachedThread == state.getTopolNr().getThread()) {
        	// commit a reaching analysis
        	return mode.reachingContexts(reached, reachedThread, state);
        }
		
		ThreadRegion r = mhp.getThreadRegion(state.getActualNode());
    	if (reach.reaches(reachedRegion, r.getID())) { // TODO: hardcoded for backward slice!
    		return mode.getTopologicalNumbers(reached, reachedThread);
    	} else {
    		return emptyIterator;
    	}
    }
}
