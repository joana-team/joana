/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental;

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
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;


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
		FORK,
		THREAD,
		ASCEND,
		DESCEND,
		INTRA,
		CLASS_INITIALIZER;
	}

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

    protected MayExistAnalysis mayExist;

    protected VisitedMap restrictive_1;
    protected VisitedMap restrictive_2;

    protected Collection<SDGNode> before;

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
        this.mayExist = n.mayExist;
    }

    /* predefined methods */

    /**
	 * @param edge The edge which shall be tested for omission.
	 */
    protected boolean omit(SDGEdge edge) {
    	return false;
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
        contextGraphs = ContextGraphBuilder.build(cfg);

        // compute thread regions and parallelism relation
        mhp = mhp();

        // initialize the slicing mode
        mode.init(contextGraphs, mhp, graph);

        summarySlicer = this.mode.initSummarySlicer(this.graph);

        mayExist = MayExistAnalysis.create(contextGraphs);

        before = InterferenceFree.computeBackward(this.graph);
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
        restrictive_1 = new VisitedMap();
        restrictive_2 = new VisitedMap();

        // init the 3 worklists for this algorithm
        LinkedList<WorklistElement> worklist_1 = new LinkedList<WorklistElement>();
        LinkedList<WorklistElement> worklist_2 = new LinkedList<WorklistElement>();
        LinkedList<WorklistElement> worklist_0 = initWorklist_0(crit, slice);

        HashSet<SimpleWorklistElement> worklist_X = new HashSet<SimpleWorklistElement>();

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
                TopologicalNumber tnr = next.getTopolNr();

                for (SDGEdge edge : mode.getEdges(node)) {
                	if (omit(edge)) continue;

                	SDGNode adjacent = mode.adjacentNode(edge);
                	Nanda.Treatment treatment = mode.phase1Treatment(edge);

                	switch (treatment) {
                	case OMIT: break;

                	case FORK:
                		Collection<TopologicalNumber> forks = contextGraphs.getForkSites(thread);

                		// all fork sites are valid
                		for (TopologicalNumber nr : forks) {
                			for (int reachedThread : contextGraphs.getThreadsOf(nr)) {
                				if (reachedThread == thread && !mhp.isDynamic(thread)) continue;

                				// add to worklist 0
                            	if (before.contains(adjacent)) {
                            		worklist_X.add(new SimpleWorklistElement(adjacent, reachedThread, nr));

                            	} else {
                    				insertSingle(false, adjacent, reachedThread, nr, next.getStates(), worklist_0, slice);
                    			}
                			}

                		}
            			break;

                	case THREAD:
                		// eliminate time travels
                		for (int reachedThread : adjacent.getThreadNumbers()) {
            				if (reachedThread == thread && !mhp.isDynamic(thread)) continue;

            				TopologicalNumber state = next.getStateOf(reachedThread);
                			Iterator<TopologicalNumber> validTNRs = realisableInterference(adjacent, reachedThread, state);

                			// add to worklist 0
                        	if (before.contains(adjacent)) {
                        		procastrinate(adjacent, reachedThread, validTNRs, worklist_X);

                        	} else if (validTNRs.hasNext()) {
                				insert(false, adjacent, reachedThread, validTNRs, next.getStates(), worklist_0, slice);
                			}
                		}
                		break;

                	case ASCEND:
                		// add to worklist 1
                		if (adjacent.isInThread(thread)) {
                			Iterator<TopologicalNumber> validTNRs = mode.interproceduralNeighbours(adjacent, tnr, thread);

                			if (before.contains(adjacent)) {
                				procastrinate(adjacent, thread, validTNRs, worklist_X);

                        	} else if (validTNRs.hasNext()) {
                				insert(false, adjacent, thread, validTNRs, next.getStates(), worklist_1, slice);
                			}
                		}
                		break;

                	case DESCEND:
                		// add to worklist 2
                		Iterator<TopologicalNumber> validTNRs = mode.interproceduralNeighbours(adjacent, tnr, thread);

                		if (before.contains(adjacent)) {
                			procastrinate(adjacent, thread, validTNRs, worklist_X);

                    	} else if (validTNRs.hasNext()) {
                			insert(true, adjacent, thread, validTNRs, next.getStates(), worklist_2, slice);
                		}
                		break;

                	case INTRA:
                		Iterator<TopologicalNumber> nrs = mode.intraproceduralNeighbours(adjacent, tnr, thread);

                		if (before.contains(adjacent)) {
                			procastrinate(adjacent, thread, nrs, worklist_X);

                    	} else {
                    		insert(false, adjacent, thread, nrs, next.getStates(), worklist_1, slice);
                    	}
                		break;

                	case CLASS_INITIALIZER:
                		// class initializer - all contexts are valid.
                        Iterator<TopologicalNumber> tnrs = mode.getTopologicalNumbers(adjacent, 0);
                        if (before.contains(adjacent)) {
                        	procastrinate(adjacent, thread, tnrs, worklist_X);

                    	} else {
                    		insert(false, adjacent, thread, tnrs, next.getStates(), worklist_1, slice);
                    	}
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
            	TopologicalNumber tnr = next.getTopolNr();

            	for (SDGEdge edge : mode.getEdges(node)) {
            		if (omit(edge)) continue;

            		Nanda.Treatment treatment = mode.phase2Treatment(edge);
            		SDGNode adjacent = mode.adjacentNode(edge);

            		switch (treatment) {
            		case OMIT: break;

            		case FORK:
                		Collection<TopologicalNumber> forks = contextGraphs.getForkSites(thread);

                		// all fork sites are valid
                		for (TopologicalNumber nr : forks) {
                			for (int reachedThread : contextGraphs.getThreadsOf(nr)) {
                				if (reachedThread == thread && !mhp.isDynamic(thread)) continue;

                				// add to worklist 0
                            	if (before.contains(adjacent)) {
                            		worklist_X.add(new SimpleWorklistElement(adjacent, reachedThread, nr));

                            	} else {
                    				insertSingle(false, adjacent, reachedThread, nr, next.getStates(), worklist_0, slice);
                    			}
                			}

                		}
            			break;

            		case THREAD:
            			// eliminate time travels
            			for (int reachedThread : adjacent.getThreadNumbers()) {
            				if (reachedThread == thread && !mhp.isDynamic(thread)) continue;

            				TopologicalNumber state = next.getStateOf(reachedThread);
            				Iterator<TopologicalNumber> validTNRs = realisableInterference(adjacent, reachedThread, state);

            				if (before.contains(adjacent)) {
            					procastrinate(adjacent, reachedThread, validTNRs, worklist_X);

                        	} else if (validTNRs.hasNext()) {
            					insert(false, adjacent, reachedThread, validTNRs, next.getStates(), worklist_0, slice);
            				}
            			}
            			break;

            		case DESCEND:
            			// add to worklist 2
            			Iterator<TopologicalNumber> validTNRs = mode.interproceduralNeighbours(adjacent, tnr, thread);

            			if (before.contains(adjacent)) {
            				procastrinate(adjacent, thread, validTNRs, worklist_X);

                    	} else if (validTNRs.hasNext()) {
            				insert(true, adjacent, thread, validTNRs, next.getStates(), worklist_2, slice);
            			}
            			break;

            		case INTRA:
            			Iterator<TopologicalNumber> nrs = mode.intraproceduralNeighbours(adjacent, tnr, thread);
            			if (before.contains(adjacent)) {
            				procastrinate(adjacent, thread, nrs, worklist_X);

                    	} else {
                    		insert(true, adjacent, thread, nrs, next.getStates(), worklist_2, slice);
                    	}
            			break;

            		case CLASS_INITIALIZER:
            			// add to worklist 2
            			Iterator<TopologicalNumber> validTnrs = realisableInterference(adjacent, thread, next.getStateOf(thread));
            			if (before.contains(adjacent)) {
            				procastrinate(adjacent, thread, validTnrs, worklist_X);

                    	} else if (validTnrs.hasNext()) {
            				insert(true, adjacent, thread, validTnrs, next.getStates(), worklist_2, slice);
            			}
            			break;

            		default: break; // do nothing
            		}
            	}
            }
        }

        remainingSlice(worklist_X, slice);

        return slice;
    }


	private void procastrinate(SDGNode adjacent, int reachedThread,
			Iterator<TopologicalNumber> validTNRs, HashSet<SimpleWorklistElement> worklist_x) {
		while (validTNRs.hasNext()) {
			TopologicalNumber nr = validTNRs.next();
			worklist_x.add(new SimpleWorklistElement(adjacent, reachedThread, nr));
		}
	}

    protected void remainingSlice(HashSet<SimpleWorklistElement> visited, Collection<SDGNode> slice) {
    	LinkedList<SimpleWorklistElement> worklist_1 = new LinkedList<SimpleWorklistElement>();
    	LinkedList<SDGNode> worklist_2 = new LinkedList<SDGNode>();
    	HashSet<SDGNode> visited2 = new HashSet<SDGNode>();

    	worklist_1.addAll(visited);

		// === phase 1 ===
		// only ascend to calling procedures
		while (!worklist_1.isEmpty()) {
			SimpleWorklistElement next = worklist_1.poll();
			SDGNode node = next.getNode();
			int thread = next.getThread();
			TopologicalNumber tnr = next.getTopolNr();

			slice.add(node);

			for (SDGEdge edge : mode.getEdges(node)) {
				if (omit(edge)) continue;

				Nanda.Treatment treatment = mode.phase1Treatment(edge);
				SDGNode adjacent = mode.adjacentNode(edge);

				switch (treatment) {
				case OMIT: break;

				case THREAD:
					// should never happen
					throw new RuntimeException();

				case ASCEND:
					// add to worklist 1
					if (adjacent.isInThread(thread)) {
						Iterator<TopologicalNumber> validTNRs = mode.interproceduralNeighbours(adjacent, tnr, thread);

						while (validTNRs.hasNext()) {
				        	TopologicalNumber m = validTNRs.next();
				            SimpleWorklistElement newElement = new SimpleWorklistElement(adjacent, thread, m);

				            if (visited.add(newElement)) {
				            	visited2.add(adjacent);
				            	worklist_1.add(newElement);
				            }
				        }
					}

					break;

				case DESCEND:
					// add to worklist 2
		            if (visited2.add(adjacent)) {
		            	worklist_2.add(adjacent);
		            }

					break;

				case INTRA:
					Iterator<TopologicalNumber> nrs = mode.intraproceduralNeighbours(adjacent, tnr, thread);

					while (nrs.hasNext()) {
			        	TopologicalNumber m = nrs.next();
			            SimpleWorklistElement newElement = new SimpleWorklistElement(adjacent, thread, m);

			            if (visited.add(newElement)) {
			            	visited2.add(adjacent);
			            	worklist_1.add(newElement);
			            }
			        }

					break;

				case CLASS_INITIALIZER:
					// class initializer - all contexts are valid.
					Iterator<TopologicalNumber> tnrs = mode.getTopologicalNumbers(adjacent, 0);

					while (tnrs.hasNext()) {
			        	TopologicalNumber m = tnrs.next();
			            SimpleWorklistElement newElement = new SimpleWorklistElement(adjacent, 0, m);

			            if (visited.add(newElement)) {
			            	visited2.add(adjacent);
			            	worklist_1.add(newElement);
			            }
			        }

					break;

				default: break; // do nothing
				}
			}
		}

		// === phase 2 ===
		while (!worklist_2.isEmpty()) {
			SDGNode next = worklist_2.poll();

			slice.add(next);

			for (SDGEdge edge : mode.getEdges(next)) {
				if (omit(edge)) continue;

				Nanda.Treatment treatment = mode.phase2Treatment(edge);
				SDGNode adjacent = mode.adjacentNode(edge);

				switch (treatment) {
				case OMIT: break;

				case THREAD:
					// should never happen
					throw new RuntimeException();

				case DESCEND:
					if (visited2.add(adjacent)) {
						worklist_2.add(adjacent);
					}

					break;

				case INTRA:
					if (visited2.add(adjacent)) {
						worklist_2.add(adjacent);
					}

					break;

				case CLASS_INITIALIZER:
					if (visited2.add(adjacent)) {
						worklist_2.add(adjacent);
					}

					break;

				default: break; // do nothing
				}
			}
		}
    }

    /** Annotates the valid contexts of a reached node with updated state tuples
     * and inserts them into a worklist.
     * @param source  The reached node.
     * @param sourceRegion  The thread region of source.
     * @param mu  The valid contexts.
     * @param oldStates  The current state tuple.
     * @param worklist  The worklist where the annotated contexts shall be inserted.
     */
    protected void insert(boolean phase2, SDGNode reached, int thread, Iterator<TopologicalNumber> mu,
            States oldStates, LinkedList<WorklistElement> worklist, HashSet<SDGNode> slice) {

        // for all valid contexts of source ...
        while (mu.hasNext()) {
        	TopologicalNumber m = mu.next();
            // ... clone the current state tuple and update the clone ...
            States newStates = update(oldStates, m, thread);

            // ... run the restrictive state tuple optimization ...
            if (optimise(reached, thread, m, newStates, restrictive_1)){
                continue;
            }

            if (phase2 && optimise(reached, thread, m, newStates, restrictive_2)) {
            	continue;
            }

            // ... annotate the remaining contexts with the updated state tuple ...
            WorklistElement newElement = new WorklistElement(reached, thread, m, newStates);

            // ... add them to the worklist ...
            worklist.addLast(newElement);

            // ... and put them to the slicing result
            slice.add(reached);

            if (phase2) {
                restrictive_2.put(reached, thread, m.getNumber(), newStates);

            } else {
            	restrictive_1.put(reached, thread, m.getNumber(), newStates);
            }
        }
    }

    protected void insertSingle(boolean phase2, SDGNode reached, int thread, TopologicalNumber m,
            States oldStates, LinkedList<WorklistElement> worklist, HashSet<SDGNode> slice) {

        // ... clone the current state tuple and update the clone ...
        States newStates = update(oldStates, m, thread);

        // ... run the restrictive state tuple optimization ...
        if (optimise(reached, thread, m, newStates, restrictive_1)){
            return;
        }

        if (phase2 && optimise(reached, thread, m, newStates, restrictive_2)) {
        	return;
        }

        // ... annotate the remaining contexts with the updated state tuple ...
        WorklistElement newElement = new WorklistElement(reached, thread, m, newStates);

        // ... add them to the worklist ...
        worklist.addLast(newElement);

        // ... and put them to the slicing result
        slice.add(reached);

        if (phase2) {
            restrictive_2.put(reached, thread, m.getNumber(), newStates);

        } else {
        	restrictive_1.put(reached, thread, m.getNumber(), newStates);
        }
    }

    /** Implements Nanda's restrictive state tuple optimization.
     * @param source  The reached node.
     * @param thread  The current thread.
     * @param m  The context of source.
     * @param toCheck  The state tuple to check against restrictiveness.
     * @return  true if toCheck is a restrictive state tuple.
     */
    protected final boolean optimise(SDGNode n, int thread, TopologicalNumber m,
            States toCheck, VisitedMap restrictive) {

        // get the state tuples of m so far
        List<States> marks = restrictive.get(n, thread, m.getNumber());

        if (marks == null) {
            return false;
        }

        // for all previous states of m ...
        for (States prev : marks) {
            boolean redundant = true;

            // ... iterate over all single states ...
            for (int i = 0; i < toCheck.size(); i++) {
                // ... if the state in toCheck cannot reach the state of prev ...
                if (!isRestrictive(toCheck.get(i), prev.get(i), i)) {
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

    protected final LinkedList<WorklistElement> initWorklist_0(Collection<SDGNode> criteria, HashSet<SDGNode> slice) {
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
                insert(false, criterion, t, mu, s, worklist_0, slice);
            }
        }

        // return the initial worklist
        return worklist_0;
    }

    protected States createInitialStates() {
    	return new States(graph.getNumberOfThreads());
    }

    protected States update(States s, TopologicalNumber tnt, int thread) {
    	States newStates = s.clone();

        for (int x = 0; x < graph.getNumberOfThreads(); x++) {
	    	if (x == thread) {
	    		if (!mhp.isDynamic(x)) {
	    			// adjust the state of the thread
	    			newStates.setState(x, tnt);
	    		}

	    	} else if (newStates.get(x) == TopologicalNumber.NONE) {
	    		if (mayExist.mayExist(x, tnt, thread)) {
	    			// activate that thread
	    			newStates.setState(x, TopologicalNumber.NONRESTRICTIVE);

	    		}
	    	} else if (!mayExist.mayExist(x, tnt, thread)) {
    			// activate that thread
	    		newStates.setState(x, TopologicalNumber.NONE);
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
    protected Iterator<TopologicalNumber> realisableInterference(SDGNode reached, int reachedThread, TopologicalNumber state) {
		if (state == TopologicalNumber.NONRESTRICTIVE || !TIME_TRAVELS) {
			// in case time travel detection is disabled or the state is nonrestrictive, all contexts are valid
			return mode.getTopologicalNumbers(reached, reachedThread);

		} else if (state == TopologicalNumber.NONE) {
			// the reached thread is inactive
			return emptyIterator;

		} else {
			// commit a reaching analysis
			return mode.reachingContexts(reached, reachedThread, state);
		}
    }

    protected boolean isRestrictive(TopologicalNumber toCheck, TopologicalNumber old, int thread) {
    	return mode.restrictiveTest(toCheck, old, thread);
    }
}
