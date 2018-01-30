/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.threads;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGJoinSensitiveForward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.util.Pair;


/**
 * @author giffhorn
 *
 */
public class PreciseMHPAnalysis implements MHPAnalysis {

	private static final Logger debug = Log.getLogger(Log.L_MHP_DEBUG);
	
    private final ThreadsInformation info;
    private final SymmetricBitMatrix map;
    private final ThreadRegions regions;
    private HashMap<Integer, Collection<ThreadRegion>> mayExist;

    private PreciseMHPAnalysis(ThreadsInformation info, SymmetricBitMatrix map, ThreadRegions regions) {
        this.info = info;
        this.map = map;
        this.regions = regions;
    }

    private void setMayExistMap(HashMap<Integer, Collection<ThreadRegion>> mayExist) {
    	this.mayExist = mayExist;
    }

    public ThreadRegions getTR() {
    	return regions;
    }

	public Collection<ThreadRegion> getThreadRegions() {
		return regions.getThreadRegions();
	}

	public ThreadRegion getThreadRegion(SDGNode node, int thread) {
		return regions.getThreadRegion(node, thread);
	}

	public ThreadRegion getThreadRegion(VirtualNode node) {
		return regions.getThreadRegion(node.getNode(), node.getNumber());
	}

	public ThreadRegion getThreadRegion(int id) {
		return regions.getThreadRegion(id);
	}

    /** Konservative Parallelitaetsabfrage - m und n sind sequentiell, wenn alle moeglichen Instanzen zueinander sequentiell sind.
     *
     * Geeignet z.B. fuer statisches Graph-Preprocessing
     *
     * @param m
     * @param n
     * @return
     */
    public boolean isParallel(SDGNode m, SDGNode n) {
        for (int mt : m.getThreadNumbers()) {
            for (int nt : n.getThreadNumbers()) {
                if (isParallel(m, mt, n, nt)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param m
     * @param n
     * @return
     */
    public boolean isParallel(SDGNode m, int mThread, SDGNode n, int nThread) {
        if (!isDynamic(mThread) && mThread == nThread) {
           return false;
        } else {
            ThreadRegion mRegion = regions.getThreadRegion(m, mThread);
            ThreadRegion nRegion = regions.getThreadRegion(n, nThread);
            return map.get(mRegion.getID(), nRegion.getID());
        }
    }

    public boolean isParallel(VirtualNode m, VirtualNode n) {
        if (!isDynamic(m.getNumber()) && m.getNumber() == n.getNumber()) {
            return false;
        } else {
            ThreadRegion mRegion = regions.getThreadRegion(m);
            ThreadRegion nRegion = regions.getThreadRegion(n);
            return map.get(mRegion.getID(), nRegion.getID());
        }
    }

	public boolean isParallel(SDGNode m, int mThread, int region) {
        ThreadRegion mRegion = regions.getThreadRegion(m, mThread);
        if (mThread == regions.getThreadRegion(region).getThread() && !isDynamic(mThread)) {
            return false;
        } else {
            return map.get(mRegion.getID(), region);
        }
	}

    public boolean isParallel(ThreadRegion r, ThreadRegion s) {
        if (!isDynamic(r.getThread()) && s.getThread() == r.getThread()) {
            return false;
        } else {
            return map.get(r.getID(), s.getID());
        }
    }
    
    
    private class ColIterator implements Iterator<ThreadRegion> {
    	private final IntIterator it;
        final boolean isDynamic;
        final int     rThread;
        
        ThreadRegion next = null;

        private ColIterator(IntIterator it, boolean isDynamic, int rThread) {
            this.it = it;
            this.isDynamic = isDynamic;
            this.rThread = rThread;
        }

        private void findNext() {
            while (next == null && it.hasNext()) {
                final int sId = it.next();
                final ThreadRegion s = regions.getThreadRegion(sId);
                if (!isDynamic && rThread == s.getThread()) {
                    next = null;
                } else {
                    next = s;
                }
            }
        }

        @Override
        public boolean hasNext() {
        	findNext();
        	return next != null;
        }
        @Override
        public ThreadRegion next() {
        	findNext();
        	final ThreadRegion result = next;
        	next = null;
        	return result;
        }
    };

    /**
     * @param r
     * @return all ThreadRegion s such that {@link PreciseMHPAnalysis#isParallel(r, s)}
     */
    public Iterable<ThreadRegion> parallelTo(ThreadRegion r) {
        final boolean isDynamic = isDynamic(r.getThread());
        final int     rThread = r.getThread();
        final IntIterator it = map.onCol(r.getID());

        return new Iterable<ThreadRegion>() {
            @Override
            public Iterator<ThreadRegion> iterator() {
                return new ColIterator(it, isDynamic, rThread);
            }
        }; 
    }
    
    /**
     * @param r
     * @return all ThreadRegion s such that s.getID() <= r.getID() && {@link PreciseMHPAnalysis#isParallel(r, s)}
     */
    public Iterable<ThreadRegion> parallelToAsymmetric(ThreadRegion r) {
        final boolean isDynamic = isDynamic(r.getThread());
        final int     rThread   = r.getThread();
        final IntIterator it = map.onColAsymemtric(r.getID());

        return new Iterable<ThreadRegion>() {
            @Override
            public Iterator<ThreadRegion> iterator() {
                return new ColIterator(it, isDynamic, rThread);
            }
        }; 
    }


    public String toString() {
//        return "Map size: " + map.getDimension();
        return map.toString();
    }

    public SDGNode getThreadExit(int thread) {
        return info.getThreadExit(thread);
    }

    public SDGNode getThreadEntry(int thread) {
        return info.getThreadEntry(thread);
    }

    public boolean isDynamic(int thread) {
        return info.isDynamic(thread);
    }

	@Override
	public boolean mayExist(int thread, VirtualNode v) {
		return mayExist.get(thread).stream().anyMatch( s -> s.getThread() == v.getNumber() && s.contains(v.getNode()));
	}

	@Override
	public boolean mayExist(int thread, SDGNode n, int nThread) {
		return mayExist(thread, new VirtualNode(n, nThread));
	}


    /* FACTORIES */

    /** Needs a pre-processed cSDG.
     *
     * @param sdg
     * @return
     */

    public static PreciseMHPAnalysis analyze(SDG sdg) {
        ThreadsInformation info = sdg.getThreadsInfo();
        CFG icfg = ICFGBuilder.extractICFG(sdg);
        PreciseMHPAnalysis tr = analyze(icfg, info);

        return tr;
    }

    @SuppressWarnings("unused")
	private static void addReturnEdges(CFG icfg) {
		List<SDGEdge> retEdges = new LinkedList<SDGEdge>();
		for (SDGNode node : icfg.vertexSet()) {
			if (node.getKind() == SDGNode.Kind.CALL) {
				Set<SDGNode> intraSucc = new HashSet<SDGNode>();
				for (SDGEdge intraEdge : icfg.getOutgoingEdgesOfKind(node, SDGEdge.Kind.CONTROL_FLOW)) {
					intraSucc.add(intraEdge.getTarget());
				}

				for (SDGEdge callEdge : icfg.getOutgoingEdgesOfKind(node, SDGEdge.Kind.CALL)) {
					SDGNode entryOfCalled = callEdge.getTarget();
					assert entryOfCalled.kind == SDGNode.Kind.ENTRY;
					SDGNode exitOfCalled = findExit(icfg, entryOfCalled);
					for (SDGNode iSucc : intraSucc) {
						SDGEdge retEdge = new SDGEdge(exitOfCalled, iSucc, SDGEdge.Kind.RETURN);
						retEdges.add(retEdge);
					}
				}
			}
		}

		icfg.addAllEdges(retEdges);
	}

	private static SDGNode findExit(CFG icfg, SDGNode entry) {
		assert entry.kind == SDGNode.Kind.ENTRY;
		final SDGNode exit = icfg.getExit(entry); 
		if (exit == null) {
			throw new IllegalStateException("no exit node found in control flow graph of method...");
		}
		
		return exit;
	}

    /**
     *
     * @param icfg
     * @param info
     * @return
     */

	private static PreciseMHPAnalysis analyze(CFG icfg, ThreadsInformation info) {
		final Logger log = Log.getLogger(Log.L_MHP_INFO);
        log.outln("Compute Thread Regions ...");
        ThreadRegions tr = ThreadRegions.createPreciseThreadRegions(icfg, info);
//        if (DEBUG) System.out.println(tr);
        MHPComputation mhp = new MHPComputation(icfg, info, tr);
    	PreciseMHPAnalysis result = mhp.getMHPMap();

    	log.outln("Compute MayExist Map ...");
        HashMap<Integer, Collection<ThreadRegion>> mayExist = computeMayExist(result);
        result.setMayExistMap(mayExist);
    	return result;
    }

    private static HashMap<Integer, Collection<ThreadRegion>> computeMayExistSlow(PreciseMHPAnalysis mhp) {
    	HashMap<Integer, Collection<ThreadRegion>> result = new HashMap<>();

    	for (ThreadRegion r : mhp.getThreadRegions()) {
    		for (ThreadRegion s : mhp.getThreadRegions()) {
    			if (mhp.isParallel(r, s)) {
    				Collection<ThreadRegion> c = result.get(r.getThread());
    				if (c == null) {
    					c = new HashSet<>();
    					result.put(r.getThread(), c);
    				}

    				c.add(s);
    			}
    		}
    	}

    	return result;
    }
	
    private static HashMap<Integer, Collection<ThreadRegion>> computeMayExist(PreciseMHPAnalysis mhp) {
    	HashMap<Integer, Collection<ThreadRegion>> result = new HashMap<>();

    	for (ThreadRegion r : mhp.getThreadRegions()) {
    		for (ThreadRegion s : mhp.parallelToAsymmetric(r)) {
    			result.compute(r.getThread(), (k, c) -> {
        			if (c == null) {
        				c = new HashSet<>();
        			}
       				c.add(s);
       				
       				return c;
    			});
    			result.compute(s.getThread(), (k, c) -> {
        			if (c == null) {
        				c = new HashSet<>();
        			}
       				c.add(r);
       				
       				return c;
    			});
    		}
    	}

    	assert result.equals(computeMayExistSlow(mhp));
    	return result;
    }


    /* MHP Computation */

    private static class MHPComputation {
        private SymmetricBitMatrix map;
        private final CFG icfg;
        private final ThreadsInformation info;
        private final ThreadRegions tr;
        //private HashMap<SDGNode, Set<SDGNode>> joinDominance;
        private LinkedList<DynamicContext> forks;
        private HashMap<DynamicContext, IntSet> indirectForks;
        private final CFGJoinSensitiveForward slicer;

        private MHPComputation (CFG icfg, ThreadsInformation info, ThreadRegions tr) {
            this.icfg = icfg;
            this.info = info;
            this.tr = tr;
            slicer = new CFGJoinSensitiveForward(icfg);
        }

        private PreciseMHPAnalysis getMHPMap() {
        	debug.outln("collect forks");//("Forks:\n"+forks);
        	forks = collectForks();
        	debug.outln("collect indirect forks");//("Indirect Forks:\n"+indirectForks);
        	indirectForks = collectIndirectForks();
        	//debug.outln("compute join dominance");//("Indirect Forks:\n"+indirectForks);
        	//joinDominance = computeJoinDominance();
        	debug.outln("compute parallelism");//("Indirect Forks:\n"+indirectForks);
        	map = computeParallelism();

            return new PreciseMHPAnalysis(info, map, tr);
        }

        private LinkedList<DynamicContext> collectForks() {
        	LinkedList<DynamicContext> result = new LinkedList<DynamicContext>();
        	result.add(null); // dummy value for thread 0

        	for (int i = 1; i < info.getNumberOfThreads(); i++) {
        		DynamicContext fork = new DynamicContext(info.getThreadContext(i), info.getThreadFork(i), i);
        		result.addLast(fork);
        	}

        	return result;
        }

		private HashMap<DynamicContext, IntSet> collectIndirectForks() {
        	HashMap<DynamicContext, IntSet> result = new HashMap<>();

        	for (DynamicContext fork : forks) {
        		if (fork == null) continue;

        		MutableSparseIntSet l = MutableSparseIntSet.makeEmpty();

            	for (DynamicContext other : forks) {
            		if (other == null) continue;

            		if (fork.isSuffixOf(other)) {
            			l.add(other.getThread());
            		}
            	}

            	result.put(fork, l);
        	}

        	return result;
        }

        private SymmetricBitMatrix computeParallelism() {
        	SymmetricBitMatrix result = new SymmetricBitMatrix(tr.size());

    		// process parallelism induced by forks
        	debug.outln("parallelism through forks");
        	for (DynamicContext fork : forks) {
        		debug.out(".");
        		if (fork == null) continue;
        		
        		final ThreadInstance forkInstance = info.getThread(fork.getThread());

        		LinkedList<SDGNode> succ = new LinkedList<SDGNode>();

        		for (SDGEdge e : icfg.getOutgoingEdgesOfKind(fork.getNode(), SDGEdge.Kind.CONTROL_FLOW)) {
        			succ.add(e.getTarget());
        		}

        		slicer.setJoins(forkInstance.getJoins());
        		Collection<SDGNode> joinSlice = slicer.slice(succ);
        		Collection<SDGNode> secondSlice = slicer.secondSlice(succ);
        		LinkedList<ThreadRegion> inJoinSlice = new LinkedList<ThreadRegion>();
        		LinkedList<ThreadRegion> inSecondSlice = new LinkedList<ThreadRegion>();

        		for (int x = 0; x < tr.size(); x++) {
        			ThreadRegion q = tr.getThreadRegion(x);
        			if (joinSlice.contains(q.getStart())) {
        				inJoinSlice.add(q);
        				inSecondSlice.add(q);
        			} else if (secondSlice.contains(q.getStart())) {
        				inSecondSlice.add(q);
        			}
        		}

        		// determine parallelism induced by fork
        		IntSet spawnedThreads = indirectForks.get(fork);

        		for (int i = 0; i < tr.size(); i++) {
            		ThreadRegion p = tr.getThreadRegion(i);

            		if (!spawnedThreads.contains(p.getThread())) continue;

            		if (p.getThread() == fork.getThread()
            					&& !forkInstance.isDynamic()) {
	            		for (ThreadRegion q : inJoinSlice) {
	            			result.set(p.getID(), q.getID());
	            			assert
	            			result.get(q.getID(), p.getID());
	            		}
            		} else {
	            		for (ThreadRegion q : inSecondSlice) {
	            			result.set(p.getID(), q.getID());
	            			assert
	            			result.get(q.getID(), p.getID());
	            		}
            		}
            	}
        	}
        	
        	boolean assertionsEnabled = false;
        	assert (assertionsEnabled = true);
        	
        	SymmetricBitMatrix resultLoopsSlow = null;
        	if (assertionsEnabled) {
        		resultLoopsSlow = new SymmetricBitMatrix(tr.size());
	        	// process parallelism induced by thread spawning inside loops
	        	debug.outln("\nparallelism through loops");
	        	for (int thread = 0; thread < info.getNumberOfThreads(); thread++) {
	        		debug.out(thread + ", ");
	        		if (info.isDynamic(thread)) {
	        			Collection<ThreadRegion> regs = new ArrayList<ThreadRegion>();
	        			for (Entry<DynamicContext, IntSet> entry : indirectForks.entrySet()) {
	        				final DynamicContext fork = entry.getKey();
	        				if (fork.getThread() != thread) continue;
	        				for (IntIterator it = entry.getValue().intIterator(); it.hasNext();) {
	        					final int other_thread = it.next(); 
	        					regs.addAll(tr.getThreadRegionSet(other_thread));
	        				}
	        			}
	        			for (ThreadRegion p : regs) {
	        				for (ThreadRegion q : regs) {
	        					resultLoopsSlow.set(p.getID(), q.getID());
	        					assert
	        					resultLoopsSlow.get(q.getID(), p.getID());
	        				}
	        			}
	        		}
	        	}
        	}
        	SymmetricBitMatrix resultLoops = null;
        	if (assertionsEnabled) {
        		resultLoops = new SymmetricBitMatrix(tr.size());
        	}
        	// process parallelism induced by thread spawning inside loops
        	debug.outln("\nparallelism through loops");
			for (Entry<DynamicContext, IntSet> entry : indirectForks.entrySet()) {
				final DynamicContext fork = entry.getKey();
				final int thread = fork.getThread();
        		if (info.isDynamic(thread)) {
        			Collection<ThreadRegion> regs = new ArrayList<ThreadRegion>();
       				for (IntIterator it = entry.getValue().intIterator(); it.hasNext();) {
       					final int other_thread = it.next(); 
       					regs.addAll(tr.getThreadRegionSet(other_thread));
       				}
        			for (ThreadRegion p : regs) {
        				for (ThreadRegion q : regs) {
        					result     .set(p.getID(), q.getID());
        					assert
        					result     .get(q.getID(), p.getID());
        					if (assertionsEnabled) {
	        					resultLoops.set(p.getID(), q.getID());
	        					assert
	        					resultLoops.get(q.getID(), p.getID());
        					}
        				}
        			}
        		}
        	}
        	
        	assert SymmetricBitMatrix.equals(resultLoops, resultLoopsSlow);
        	
        	
        	// refine parallelism by inspecting joins
        	/*debug.outln("\ninspecting joins");
        	int ctr = 0;
        	for (int thread = 1; thread < info.getNumberOfThreads(); thread++) {
        		SDGNode join = info.getThreadJoin(thread);
        		if (join == null) continue;

        		Collection<SDGNode> dom = joinDominance.get(join);
        		Collection<ThreadRegion> regs = tr.getThreadRegionSet(thread);

        		for (ThreadRegion r : tr) {
        			if (r.getThread() == thread) continue;

        			if (dom.contains(r.getStart())) {
        				for (ThreadRegion q : regs) {
        					ctr++;
        					result.clear(r.getID(), q.getID());
        					result.clear(q.getID(), r.getID());
        				}
        			}
        		}
        	}
        	debug.outln("parallelism removed by join-analysis: " + ctr);*/
        	debug.outln("done");

        	return result;
        }

        /* TODO: this is a proof-of-concept implementation.
         * Refine it some day with an interprocedural dominator tree.
         *
         * @param thread
         * @param r
         * @return
         */
        /*private HashMap<SDGNode, Set<SDGNode>> computeJoinDominance() {
        	HashMap<SDGNode, Set<SDGNode>> result = new HashMap<SDGNode, Set<SDGNode>>();

        	for (SDGNode join : info.getAllJoins()) {

	            // compute a thread-local slice
        		HashSet<SDGNode> phase1 = phase1(join);

	            // remove from it all nodes not dominated by join
//	            slice = close(slice);
//	            System.out.println(join+"\n"+phase1);
	            // store it in the map
        		phase1.add(join);
	            result.put(join, phase1);
        	}

            return result;
        }*/

        /*private HashSet<SDGNode> phase1(SDGNode join) {
        	HashSet<SDGNode> visited = new HashSet<SDGNode>();
        	LinkedList<SDGNode> w = new LinkedList<SDGNode>();

        	visited.add(join);
        	w.add(join);

        	// compute a phase-1 forward slice, omit forks and calls
        	while (!w.isEmpty()) {
        		SDGNode next = w.poll();

        		for (SDGEdge e : icfg.outgoingEdgesOf(next)) {
        			if (e.getKind() == SDGEdge.Kind.FORK || e.getKind() == SDGEdge.Kind.CALL) {
        				continue;
        			}

        			if (visited.add(e.getTarget())) {
        				w.add(e.getTarget());
        			}
        		}
        	}

        	// remove all nodes which can be reached by a path bypassing join
        	HashSet<SDGNode> removed = new HashSet<SDGNode>();
        	HashSet<SDGNode> remove = new HashSet<SDGNode>();

        	do {
        		remove.clear();

        		for (SDGNode n : visited) {
        			// the join always dominates itself
        			if (n == join) continue;
        			for (SDGEdge inc : icfg.incomingEdgesOf(n)) {
            			if (inc.getKind() == SDGEdge.Kind.FORK || inc.getKind() == SDGEdge.Kind.CALL) {
            				continue;
            			}

        				SDGNode from = inc.getSource();

        				//skip synthetic edges
        				if (//(from.getKind() == SDGNode.Kind.CALL && inc.getKind() == SDGEdge.Kind.CONTROL_FLOW) ||
        						(from.getKind() == SDGNode.Kind.ENTRY && n.getKind() == SDGNode.Kind.EXIT)) {
        					continue;
        				}

        				if (inc.getKind() == SDGEdge.Kind.RETURN
        						&& (removed.contains(from) || remove.contains(from))) {
        					// not dominated by join
        					remove.add(n);
        					break;

        				} else if (inc.getKind() == SDGEdge.Kind.CONTROL_FLOW
        						&& (!visited.contains(from) || remove.contains(from))) {
        					// not dominated by join
        					remove.add(n);
        					break;
        				}
        			}
        		}

        		visited.removeAll(remove);
        		removed.addAll(remove);


        	} while (!remove.isEmpty());

        	return visited;
        }*/

        @SuppressWarnings("unused")
		private HashSet<SDGNode> threadLocalSlice(SDGNode join) {
        	HashSet<SDGNode> visited = new HashSet<SDGNode>();
        	LinkedList<SDGNode> w1 = new LinkedList<SDGNode>();
        	LinkedList<SDGNode> w2 = new LinkedList<SDGNode>();

        	visited.add(join);
        	w1.add(join);

        	// compute a thread-local slice
        	while (!w1.isEmpty()) {
        		SDGNode next = w1.poll();

        		for (SDGEdge e : icfg.outgoingEdgesOf(next)) {
        			if (e.getKind() != SDGEdge.Kind.FORK
        					&& visited.add(e.getTarget())) {

        				if (e.getKind() == SDGEdge.Kind.CALL) {
            				w2.add(e.getTarget());

        				} else {
            				w1.add(e.getTarget());
        				}
        			}
        		}
        	}

        	while (!w2.isEmpty()) {
        		SDGNode next = w2.poll();

        		for (SDGEdge e : icfg.outgoingEdgesOf(next)) {
        			if (e.getKind() != SDGEdge.Kind.FORK
        					&& e.getKind() != SDGEdge.Kind.RETURN
        					&& visited.add(e.getTarget())) {

        				w2.add(e.getTarget());
        			}
        		}
        	}

        	return visited;
        }

        @SuppressWarnings("unused")
		private HashSet<SDGNode> close(HashSet<SDGNode> slice) {
        	HashSet<SDGNode> remove = new HashSet<SDGNode>();

        	do {
        		remove.clear();

        		for (SDGNode n : slice) {
        			for (SDGEdge inc : icfg.incomingEdgesOf(n)) {
        				SDGNode from = inc.getSource();

        				//skip synthetic edges
        				if (from.getKind() == SDGNode.Kind.CALL && inc.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
        					continue;
        				}

        				if (from.getKind() == SDGNode.Kind.ENTRY && n.getKind() == SDGNode.Kind.EXIT) {
        					assert false;
        				}

        				if (inc.getKind() == SDGEdge.Kind.FORK || inc.getKind() == SDGEdge.Kind.JOIN) {
        					continue;
        				}

        				if (!slice.contains(from) || remove.contains(from)) {
        					// not dominated by join
        					remove.add(n);
        					break;
        				}
        			}
        		}

        		slice.removeAll(remove);


        	} while (!remove.isEmpty());

            return slice;
        }
    }

}
