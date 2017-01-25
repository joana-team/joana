/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.chopper.TruncatedNonSameLevelChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.ContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.StaticContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;



/**
 * This class realizes Krinke's optimized Algorithm for threaded interprocedural slicing.
 * Uses thread regions.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class Slicer implements edu.kit.joana.ifc.sdg.graph.slicer.Slicer {
	private class Visited {
	    private HashMap<Context, List<States>> markedStates;

	    private Visited() {
	    	markedStates = new HashMap<Context, List<States>>();
	    }

	    private void add(WorklistElement w) {
	        // get the place for the context
	        List<States> s = markedStates.get(w.getContext());

	        if (s == null) {
	            s = new LinkedList<States>();
	            markedStates.put(w.getContext(), s);
	        }

	        // add the state tuple
	        s.add(w.getStates());
	    }

	    /** Checks if the given worklist element is restrictive according to a list of states.
	     * @param check   The element to check.
	     * @param marked  Contains the state tuple list for the context of check.
	     */
	    private boolean isRedundant(WorklistElement check) {
	        List<States> oldStates = markedStates.get(check.getContext());

	        if (oldStates == null) return false;

	        // iterate over all states
	        for (States old : oldStates) {
	            // check for restrictiveness
	            if (isRestrictive(check.getStates(), old)){
	                return true;
	            }
	        }

	        return false;
	    }

	    /** Checks if a state tuple `actual' is restrictive to a state tuple `previous'
	     *
	     */
	    private boolean isRestrictive(States actual, States previous) {
	        // iterate over all states
	        for (int i = 0; i < previous.size(); i++) {
	        	if (previous.state(i) == States.NONRESTRICTIVE) {
	    			continue;

	    		} else if (actual.state(i) == States.NONRESTRICTIVE) {
	    			return false;

	    		} else if (actual.state(i) == States.NONE) {
	    			continue;

	    		} else if (previous.state(i) == States.NONE) {
	    			return false;

	    		} else if (!reaches(actual.state(i), previous.state(i))) {
	    			return false;
	    		}
	        }

	        return true;
	    }
	}


	public static long elems = 0L;
	private static boolean TIME_TRAVELS = true;

    /** the corresponding interprocedural control flow graph */
    protected CFG icfg;
    /** The graph to be sliced. */
    protected SDG sdg;
    /** The folded ICFG. */
    protected FoldedCFG foldedIcfg;
    protected ContextManager conMan;

    /** A reachability checker for control flow graphs. */
    protected ReachabilityChecker reachable;
    /** the call site of the threads of the program to slice */
    protected SDGNode threadCall;
    /** the call site for the main() thread **/
    protected SDGNode mainCall;

    protected Context2PhaseSlicer c2pSlicer;
    /** A summary slicer. */
    protected SummarySlicer summarySlicer;
    /** A truncated non-same-level chopper. */
    protected TruncatedNonSameLevelChopper truncated;

    protected MHPAnalysis mhp;

    protected LinkedList<Context> empty = new LinkedList<Context>();
    protected Visited visited;


    /** DEBUG information*/
    public static long eingefuegt = 0l;
    public static int aufruf = 0;
    public static int chopaufruf = 0;
    public static int summaryaufruf = 0;
    public long eingefuegt() {
        return eingefuegt;
    }
    public int optimierung = 0;
    public int opt() {
        return optimierung;
    }
    public int opt1Sliced, opt1NotSliced = 0;
    public String opt1() {
        return "(slicer: "+opt1Sliced + ", map: "+opt1NotSliced+")";
    }
    public int noReach, reach, reducedReach = 0;
    public String opt2() {
        return "(no reach: "+noReach+", CFG: "+reach + ", reduced CFG: "+reducedReach+")";
    }

    public Slicer() { }

    /**
     * Creates a new instance of this slicer.
     *
     * @param graph A threaded interprocedural program dependencies graph that shall be sliced.
     *              It has to contain control flow edges.
     */
    public Slicer(SDG graph) {
        setGraph(graph);
    }

    /**
     * Initializes the fields of the slicer.
     *
     * @param graph A threaded interprocedural program dependencies graph that shall be sliced.
     *              It has to contain control flow edges.
     */
    public void setGraph(SDG graph) {
        // init context-using 2-phase slicer
        sdg = graph;
        conMan = StaticContextManager.create(sdg);

        // build the threaded ICFG
        icfg = ICFGBuilder.extractICFG(sdg);

        // fold ICFG with Krinke's two-pass folding algorithm
        foldedIcfg = GraphFolder.twoPassFolding(icfg);

        c2pSlicer = new Context2PhaseSlicer(sdg, conMan);

        // a simple 2-phase slicer
        // the summary slicer shall not traverse interference edges
        summarySlicer = new SummarySlicer(sdg);

        // a truncated non-same-level chopper
        truncated = new TruncatedNonSameLevelChopper(sdg);

        mhp = PreciseMHPAnalysis.analyze(sdg);

        // a reachability checker for ICFGs
        reachable = new ReachabilityChecker(foldedIcfg);
    }

    public Collection<SDGNode> slice(SDGNode criterion) {
    	return slice(Collections.singleton(criterion));
    }

    /**
     * The slicing algorithm.
     *
     * @param criterion  Contains a node and a thread ID for which the program
     *                   shall be sliced.
     */
    public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
        // the slice
        HashSet<SDGNode> slice = new HashSet<SDGNode>();

        /*** slicing.. ***/
        // sequential slice for the contexts of the slicing criteria
        Collection<SDGNode> interferingNodes = summarySlicer.slice(criteria, slice);

        if (interferingNodes.isEmpty()) {elems++;
            return slice;

        } else {
        	Collection<SDGNode> chop = truncated.chop(interferingNodes, criteria);
            krinkeSlice(criteria, chop, slice);
            return slice;
        }
    }

    private void krinkeSlice(Collection<SDGNode> criteria, Collection<SDGNode> restrict, Collection<SDGNode> slice) {
        // all appeared state tuples for every appeared context
        visited = new Visited();

        // the three worklists
        LinkedList<WorklistElement> w = initialWorklist(criteria);
        LinkedList<WorklistElement> w0 = new LinkedList<WorklistElement>();

        for(WorklistElement o : w){
            visited.add(o);
            elems++;
        }

    	threadLocalSlice(w, slice, w0, restrict);

        while (!w0.isEmpty()) {
        	LinkedList<WorklistElement> next = new LinkedList<WorklistElement>();
        	WorklistElement elem = w0.poll();
        	next.add(elem);
        	for (WorklistElement we : w0) {
        		if (we.getThread() == elem.getThread()) {
        			next.add(we);
        		}
        	}
        	w0.removeAll(next);

        	Collection<SDGNode> interferingNodes = c2pSlicer.contextSlice(next, slice);

        	if (!interferingNodes.isEmpty()) {
        		HashSet<SDGNode> nodes = new HashSet<SDGNode>();
            	next.add(elem);
            	for (WorklistElement we : next) {
        			nodes.add(we.getNode());
            	}

        		restrict = truncated.chop(interferingNodes, Collections.singleton(elem.getNode()));
        		threadLocalSlice(next, slice, w0, restrict);
        	}
        }
    }

    private void threadLocalSlice(Collection<WorklistElement> elem, Collection<SDGNode> slice,
    							  LinkedList<WorklistElement> wNext, Collection<SDGNode> restrict) {
        LinkedList<WorklistElement> w1 = new LinkedList<WorklistElement>();
        LinkedList<WorklistElement> w2 = new LinkedList<WorklistElement>();

        w1.addAll(elem);

    	while (!w1.isEmpty()) {
			// process the next element
			WorklistElement next = w1.poll();
			Context context = next.getContext();
			States states = next.getStates();
			int thread = next.getThread();
			slice.add(next.getNode());

			// handle all incoming edges of 'next'
			for(SDGEdge e : sdg.incomingEdgesOf(next.getNode())) {
				if (!e.getKind().isSDGEdge()) continue;

				SDGNode source = e.getSource();

				if (e.getKind().isThreadEdge()) {
					for (int t : source.getThreadNumbers()) {
						// make sure we in fact change threads
						if (t != thread || mhp.isDynamic(thread)) {
							// get all valid context for 'source'
							Collection<Context> valid = reachingContexts(source, t, next);

							// create new worklist elements
							for (Context con : valid) {
								States newStates = update(states, con);
								WorklistElement we =  new WorklistElement(con, newStates);
								if (!visited.isRedundant(we)) {
									visited.add(we);
									wNext.add(we);
						            elems++;
								}
							}
						}
					}

				} else if (restrict.contains(source)) {
    				// distinguish between different kinds of edges
    				if (e.getKind() == SDGEdge.Kind.PARAMETER_IN
    						&& source.getKind() == SDGNode.Kind.FORMAL_OUT) {

    					// Class initializer methods have a special integration into our SDGs.
    					// Their formal-out vertices have outgoing param-in edge, which connect them with
    					// the rest of the SDG.
    					Collection<Context> newContexts = conMan.getAllContextsOf(source);

    					// update the worklist
    					for (Context con : newContexts) {
    						States newStates = update(states, con);
    						WorklistElement we =  new WorklistElement(con, newStates);
    						if (!visited.isRedundant(we)) {
    							visited.add(we);
    							w1.add(we);
    						}
    					}

    				} else if (e.getKind() == SDGEdge.Kind.CALL
    						|| e.getKind() == SDGEdge.Kind.PARAMETER_IN) {
    					// go to the calling procedure
    					if (source.isInThread(thread) && context.isInCallingProcedure(source)) {
    						SDGNodeTuple callSite = sdg.getCallEntryFor(e);
    						Context[] newContexts = conMan.ascend(source, callSite, context);

    						for (Context con : newContexts) {
    							if (con != null) {
    								States newStates = update(states, con);
    								WorklistElement we =  new WorklistElement(con, newStates);
    								if (!visited.isRedundant(we)) {
    									visited.add(we);
    									w1.add(we);
    								}
    							}
    						}
    					}

    				} else if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
    					// go to the called procedure
    					SDGNodeTuple callSite = sdg.getCallEntryFor(e);
    					Context con = conMan.descend(source, callSite, context);
    					States newStates = update(states, con);
    					WorklistElement we =  new WorklistElement(con, newStates);
    					if (!visited.isRedundant(we)) {
    						visited.add(we);
    						w2.add(we);
    					}

    				} else {
    					// intra-procedural traversal
    					Context con = conMan.level(source, context);
    					States newStates = update(states, con);
    					WorklistElement we =  new WorklistElement(con, newStates);
    					if (!visited.isRedundant(we)) {
    						visited.add(we);
    						w1.add(we);
    					}
    				}
    			}
			}
		}

		// slice
		while(!w2.isEmpty()) {
			// process the next element
			WorklistElement next = w2.poll();
			Context context = next.getContext();
			States states = next.getStates();
			int thread = next.getThread();
			slice.add(next.getNode());

			// handle all incoming edges of 'next'
			for(SDGEdge e : sdg.incomingEdgesOf(next.getNode())){
				if (!e.getKind().isSDGEdge()) continue;

				SDGNode source = e.getSource();

				if (e.getKind().isThreadEdge()) {
					for (int t : source.getThreadNumbers()) {
						// make sure we in fact change threads
						if (t != thread || mhp.isDynamic(thread)) {
							// get all valid context for 'source'
							Collection<Context> valid = reachingContexts(source, t, next);

							// create new worklist elements
							for (Context con : valid) {
								States newStates = update(states, con);
								WorklistElement we =  new WorklistElement(con, newStates);
								if (!visited.isRedundant(we)) {
									visited.add(we);
									wNext.add(we);
						            elems++;
								}
							}
						}
					}

				} else if (restrict.contains(source)) {
    				// distinguish between different kinds of edges
    				if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN) {
    					// skip

    				} else if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
    					// go to the called procedure
    					SDGNodeTuple callSite = sdg.getCallEntryFor(e);
    					Context con = conMan.descend(source, callSite, context);
    					States newStates = update(states, con);
    					WorklistElement we =  new WorklistElement(con, newStates);
    					if (!visited.isRedundant(we)) {
    						visited.add(we);
    						w2.add(we);
    					}

    				} else {
    					// intra-procedural traversal
    					Context con = conMan.level(source, context);
    					States newStates = update(states, con);
    					WorklistElement we =  new WorklistElement(con, newStates);
    					if (!visited.isRedundant(we)) {
    						visited.add(we);
    						w2.add(we);
    					}
    				}
				}
			}
    	}
    }

    /**
     * Computes all valid contexts according to an interference edge traversion.
     * It creates all possible contexts and performs a reaching analysis with
     * regard to a given state tuple.
     *
     * @param source        The node the interference edge is traversed to.
     * @param thread        The node's thread..
     * @param w             The worklist element representing the point where the old thread is left.
     *                      It contains the state tuple for the reaching analysis.
     */
    private Collection<Context> reachingContexts(SDGNode source, int thread, WorklistElement w) {
        Collection<Context> reached = new LinkedList<Context>();
        Context target = w.getStates().state(thread);

        if (target == States.NONRESTRICTIVE || !TIME_TRAVELS) {
            // if the thread was not visited yet, all contexts are valid
            return conMan.getContextsOf(source, thread);

        } else if (target == States.NONE) {
        	return empty;

        } else {
            // retrieve all possible contexts for source in its thread
            Collection<Context> contextList = conMan.getContextsOf(source, thread);

            // return every context of source that reaches target
            for (Context s : contextList) {
                // if reachable, add context to reached list
                if (reaches(s, target)) {
                    reached.add(s);
                }
            }
        }

        return reached;
    }

    /**
     * Creates the initial worklist for the slicing algorithm.
     * It consists of a LinkedList containing one ThreadedWorklistElement for
     * every context which can reach the given starting criterion.
     *
     * @param data.node  The starting criterion.
     * @return  A LinkedList, maybe empty.
     */
    private LinkedList<WorklistElement> initialWorklist(Collection<SDGNode> criteria) {
        LinkedList<WorklistElement> s = new LinkedList<WorklistElement>();

        for (SDGNode node : criteria) {
            int[] threads = node.getThreadNumbers();

            for (int thread : threads) {
                Collection<Context> contexts = conMan.getContextsOf(node, thread);

                for (Context con : contexts) {
                    States newStates = update(new States(sdg.getNumberOfThreads()), con);
                    WorklistElement w = new WorklistElement(con, newStates);
                    if (!visited.isRedundant(w)) {
                    	visited.add(w);
                    	s.add(w);
                    }
                }
            }
        }

        return s;
    }

    /** Maps a SDG-Context to a CFG-Context.
     * This mapping is nessecary as SDG and CFG are folded differently.
     *
     * @param origin  A SDG-Context.
     * @param foldedCall  The call graph.
     * @param foldedIcfg  The CFG
     * @return  A CFG-Context.
     */
    private DynamicContext map(Context origin) {
        // list for building the context that maps to Context 'con'
        LinkedList<SDGNode> res = new LinkedList<SDGNode>();

        // for every vertex in the call stack of 'con' determine the fold vertex in graph 'to'
        for (SDGNode node : origin.getCallStack()) {

            // add found vertex to list 'res'
            if (node.getKind() == SDGNode.Kind.FOLDED) {

                // If the folded node is induced by a cycle of return edges,
                // and the topmost call site of 'context' is the folded call cycle
                // according to the return-cycle, create a context without this
                // topmost call site and add it to the initial worklist.
                if (node.getLabel() == GraphFolder.FOLDED_RETURN) {
                    // creates a context without the the folded call cycle
                    // according to the return-cycle
                    SDGNode call = returnCall(node);

                    if (res.size() == 0 || call != res.getLast()) {
                        res.addLast(call);
                    }
                }

                // maps node to the belonging node of the CFG
                SDGNode x = map(node);

                // prohibit redundant piling of Contexts
                if (res.size() == 0 || x != res.getLast()) {
                    res.addLast(x);
                }

            } else {
                res.addLast(foldedIcfg.map(node));
            }
        }

        SDGNode node = foldedIcfg.map(origin.getNode());
        if (res.size() > 0 && node == res.getLast()) {
            res.removeLast();
        }

        return new DynamicContext(res, node, origin.getThread());
    }

    /** Maps a SDG-node to a CFG-node.
     *
     * @param node  A SDG-node.
     * @param foldedCall  The call graph.
     * @param foldedIcfg  The CFG
     * @return  A CFG-node.
     */
    private SDGNode map(SDGNode node) {
        // 1. get one of the unmapped nodes
        SDGNode unmapped = conMan.unmap(node);

        // 2. get mapping in the other graph
        return foldedIcfg.map(unmapped);
    }

    /** Computes the folded call cycle belonging to a folded return cycle.
     *
     * @param returnFold  The folded return cycle.
     * @return  The belonging call cycle.
     */
    private SDGNode returnCall(SDGNode returnFold) {
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        SDGNode callFold = null;

        worklist.add(returnFold);

        // find the corresponding folded call cycle
        loop:
            while (!worklist.isEmpty()) {
                SDGNode next = worklist.poll();
                for (SDGEdge cf : foldedIcfg.getIncomingEdgesOfKind(next, SDGEdge.Kind.CONTROL_FLOW)) {
                    if (cf.getSource().getKind() == SDGNode.Kind.FOLDED
                            && cf.getSource().getLabel() == GraphFolder.FOLDED_CALL) {

                        callFold = cf.getSource();
                        break loop;
                    }
                    worklist.addFirst(cf.getSource());
                }
            }

        return callFold;
    }

    private boolean reaches(Context start, Context target) {
	    // map the target to the folded ICFG
	    DynamicContext mappedTarget = map(target);
	    DynamicContext mappedStart = map(start);
    	return reachable.reaches(mappedStart, mappedTarget);
    }

    protected States update(States s, Context c) {
    	States newStates = s.clone();
    	int thread = c.getThread();

    	for (int x = 0; x < sdg.getNumberOfThreads(); x++) {
	    	if (x == thread) {
	    		if (!mhp.isDynamic(x)) {
	    			// adjust the state of the thread
	    			newStates.set(x, c);
	    		} else System.out.println(2);

	    	} else if (newStates.state(x) == States.NONE) {
	    		if (mhp.mayExist(x, c.getNode(), thread)) {
	    			// activate that thread
	    			newStates.set(x, States.NONRESTRICTIVE);
	    		}

	    	} else if (!mhp.mayExist(x, c.getNode(), thread)) {
    			// activate that thread
	    		newStates.set(x, States.NONE);
	    	}
        }

        return newStates;
    }
}
