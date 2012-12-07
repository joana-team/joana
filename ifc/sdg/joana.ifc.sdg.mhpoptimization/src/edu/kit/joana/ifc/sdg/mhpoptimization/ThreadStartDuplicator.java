/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.mhpoptimization;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ifc.sdg.util.JavaType.Format;
import edu.kit.joana.ifc.sdg.util.maps.MapUtils;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;


/**
 *
 * -- Created on October 18, 2006<br>
 * -- Modified in 2012
 * @author  Dennis Giffhorn
 * @author Martin Mohr
 */
public class ThreadStartDuplicator {
	public static final int CHRISTIAN = 1;
	public static final int JUERGEN = 2;

	private SDG sdg;

	/** the cfg extracted from the sdg */
	private CFG icfg;

	private Map<JavaType, SDGNode> threadClass_runEntry; // maps subclasses of java.lang.threads to the entry nodes of their run method
	private Map<SDGNode, Set<SDGNode>> alloc_run; // maps thread allocations to thread entries
	private Map<SDGNode, Set<SDGNode>> callStart_alloc; // maps thread allocations to calls of Thread::start
	private Map<SDGNode, Set<SDGNode>> callStart_run; // maps calls of Thread::start to thread entries
	private Map<SDGNode, Set<SDGNode>> run_callStart; // maps thread entries to calls of Thread::start

	/** Creates a new instance of ThreadAllocation
	 * @param g  A SDG.
	 */
	public ThreadStartDuplicator(SDG g, int type) {
		sdg = g;

		// build the threaded ICFG
		icfg = ICFGBuilder.extractICFG(sdg);
		init();
	}

	public Collection<SDGNode> getAllocs() {
		return alloc_run.keySet();
	}

	private void init() {
		// determine all existing thread run methods
		// the List will contain their entry nodes
		// runEntries = getAllRunEntryNodes();

		// determine all subclasses of java.lang.Thread and their respective run
		// methods
		threadClass_runEntry = mapThreadClassToRunEntry();

		final Logger debug = Log.getLogger(Log.L_MHP_DEBUG);
		debug.outln("run-method entries                : " + threadClass_runEntry.values());

		Map<JavaType, Set<SDGNode>> threadClasses_alloc = mapThreadClassToAllocationSites(threadClass_runEntry.keySet());
		Map<SDGNode, Set<JavaType>> runEntry_threadClasses = MapUtils.invertSimple(threadClass_runEntry);

		// determine thread allocation sites
		Map<SDGNode, Set<SDGNode>> run_alloc = MapUtils.concat(runEntry_threadClasses, threadClasses_alloc);
		// = mapRunEntriesToThreadAllocations(threadClass_runEntry);

		// reverse mapping
		// alloc_run = mapThreadAllocationsToRunEntries(run_alloc);
		alloc_run = MapUtils.invert(run_alloc);

		debug.outln("run-entries -> thread allocations : " + run_alloc);
		debug.outln("thread allocations -> run-entries : " + alloc_run);

		// compute Thread::start invocations for each thread allocation site
		callStart_alloc = mapThreadStartToAllocationSites();

		// map calls of Thread::start to thread entries
		callStart_run = MapUtils.concat(callStart_alloc, alloc_run);
		run_callStart = MapUtils.invert(callStart_run);

		debug.outln("Thread::start calls -> thread allocations: " + callStart_alloc);
		debug.outln("Thread::start calls -> run-entries: " + callStart_run);
		debug.outln("run-entries -> Thread::start calls: " + run_callStart);

		// clone java.lang.Thread::start()
	}

	/**
	 * Executes the thread allocation analysis.
	 */
	public void dupe() {

		cloneThreadStart();
	}

	/**
	 * Computes a map which assigns each subclass of java/lang/Thread occurring in the sdg the entry of its run() method.
	 * The idea is to look at the fork edges: A fork edge's source is always located in the Thread::start() method and
	 * its target is always the entry node of the run() method of the thread that has been spawned.
	 * @return a map which maps each subclass of java/lang/Thread occurring in the sdg to the entry of its run() method.
	 */
	private Map<JavaType, SDGNode> mapThreadClassToRunEntry() {
		Map<JavaType, SDGNode> result = new HashMap<JavaType, SDGNode>();
		//List<SDGNode> result = new LinkedList<SDGNode>();

		// traverse all fork edges to find the entries
		for (SDGEdge fork : icfg.edgeSet()) {
			if (fork.getKind() != SDGEdge.Kind.FORK) continue;

			SDGNode runEntry = fork.getTarget();
			JavaMethodSignature runSig = JavaMethodSignature.fromString(runEntry.getBytecodeMethod());


			result.put(runSig.getDeclaringType(), runEntry);

		}

		return result;
	}

	/**
	 * Computes a map which maps each subclass of java/lang/Thread in the given set to the set of possible allocation
	 * sites of that subclass.
	 * @param threadClasses subclasses of java/lang/Thread for which the possible allocation sites are to be computed
	 * @return a map containing for each subclass of java/lang/Thread from the given set the possible allocation
	 * sites of that subclass.
	 */
	private Map<JavaType, Set<SDGNode>> mapThreadClassToAllocationSites(Set<JavaType> threadClasses) {
		Map<JavaType, Set<SDGNode>> result = new HashMap<JavaType, Set<SDGNode>>();
		for (SDGNode n : icfg.vertexSet()) {
			if (n.getOperation() != SDGNode.Operation.DECLARATION) {
				continue;
			} else {
				JavaType nType = JavaType.parseSingleTypeFromString(n.getType(), Format.BC);
				if (threadClasses.contains(nType)) {
					Set<SDGNode> allocSites;
					if (result.containsKey(nType)) {
						allocSites = result.get(nType);
					} else {
						allocSites = new HashSet<SDGNode>();
						result.put(nType, allocSites);
					}
					allocSites.add(n);
				}
			}
		}

		return result;
	}

	/**
	 * Computes a map which maps each call of a Thread::start() method to possible allocation sites of the thread object
	 * the Thread::start() method is called on. To achieve this, first the calls to Thread::start() are collected. For
	 * each such calls, the corresponding sdg node contains information about the possible allocation sites of the this-
	 * parameter.
	 * @return a map which maps each call of a Thread::start() method to possible allocation sites of the thread object
	 * the Thread::start() method is called on
	 */
	private Map<SDGNode, Set<SDGNode>> mapThreadStartToAllocationSites() {

		Map<SDGNode, Set<SDGNode>> ret = new HashMap<SDGNode, Set<SDGNode>>();

		// search calls of Thread::start()
		LinkedList<SDGNode> callsOfThreadStart = new LinkedList<SDGNode>();


		for (SDGEdge e : sdg.edgeSet()) {
			if (e.getKind() == SDGEdge.Kind.FORK) {
				// fork edges have a node of the Thread::start() method as source
				SDGNode forkSite = e.getSource();
				SDGNode threadStartEntry = sdg.getEntry(forkSite);
				for (SDGEdge incEdge : sdg.incomingEdgesOf(threadStartEntry)) {
					if (incEdge.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.FORK) {
						callsOfThreadStart.add(incEdge.getSource());
					}
				}
			}
		}

		for (SDGNode callOfThreadStart : callsOfThreadStart) {
			assert callOfThreadStart.getAllocationSites() != null;
			Set<SDGNode> allocSites = new HashSet<SDGNode>();
			for (int allocId : callOfThreadStart.getAllocationSites()) {
				SDGNode allocNode = sdg.getNode(allocId);
				assert allocNode != null;
				allocSites.add(allocNode);
			}

			ret.put(callOfThreadStart, allocSites);
		}

		return ret;

	}

	private void cloneThreadStart() {
		/* collect the nodes and edges of Thread::start */
		//        SDGNode n = ti.getThreadFork(1);
		if (threadClass_runEntry.size() < 1) return;

		/** select a run method entry node */
		SDGNode run = threadClass_runEntry.values().iterator().next();
		SDGNode aNodeOfThreadStart = null;
		for (SDGEdge e : icfg.getIncomingEdgesOfKind(run, SDGEdge.Kind.FORK)) {
			aNodeOfThreadStart = e.getSource();
			break;
		}


		if (aNodeOfThreadStart == null) return;

		/** n is the call site of the call to Thread::start() inside the selected run method */
		int newLastID = sdg.lastId() + 1;
		int newLastProc = sdg.lastProc() + 1;

		HashSet<SDGNode> threadStartNodes = new HashSet<SDGNode>();
		HashSet<SDGEdge> threadStartEdges = new HashSet<SDGEdge>();
		threadStartNodes.addAll(sdg.getNodesOfProcedure(aNodeOfThreadStart));

		for (SDGNode m : threadStartNodes) {
			threadStartEdges.addAll(sdg.incomingEdgesOf(m));
			threadStartEdges.addAll(sdg.outgoingEdgesOf(m));
		}

		/* clone Thread::start */
		// insert the new nodes - we need one instance of Thread::start for every call site
		Collection<SDGNode> calls = callStart_run.keySet();

		/** map each call of Thread::start to a copy of the pdg of Thread::start */
		Map<SDGNode, Set<SDGNode>> call_start = new HashMap<SDGNode, Set<SDGNode>>();
		Iterator<SDGNode> iter = calls.iterator();
		SDGNode firstCallOfThreadStart = iter.next();

		/** one of the calls is mapped to the original version of the Thread::start pdg */
		call_start.put(firstCallOfThreadStart, threadStartNodes);

		/**
		 * now clone Thread::start for each of the other calls
		 */
		while (iter.hasNext()) {
			SDGNode callOfThreadStart = iter.next();
			HashSet<SDGNode> nodesOfClone = new HashSet<SDGNode>();

			/** map each node of Thread::start to its clone */
			HashMap<SDGNode, SDGNode> original_clone = new HashMap<SDGNode, SDGNode>();

			for (SDGNode m : threadStartNodes) {
				/** clone node */
				SDGNode mClone = m.clone();
				mClone.setId(newLastID);
				mClone.setProc(newLastProc);
				newLastID++;

				/** add cloned node to cloned pdg */
				nodesOfClone.add(mClone);

				/** map original node to its clone */
				original_clone.put(m, mClone);

				/** add cloned node to sdg */
				sdg.addVertex(mClone);
			}

			/** map current call of Thread::start() to newly created copy */
			call_start.put(callOfThreadStart, nodesOfClone);
			newLastProc++;


			Set<SDGNode> callSiteOfThreadStart = callSite(callOfThreadStart);
			Set<SDGNode> runsCalledFromThreadStart = calledRunsOfThreadStart(callOfThreadStart);

			/** clone edges */
			for (SDGEdge e : threadStartEdges) {

				/**
				 * easy for intraprocedural edges...
				 */
				if (e.getKind().isIntraproceduralEdge()) {
					SDGNode sourceClone = original_clone.get(e.getSource());
					SDGNode targetClone = original_clone.get(e.getTarget());
					sdg.addEdge(new SDGEdge(sourceClone, targetClone, e.getKind()));

				} else {
					// interprocedural edge - check if it connects Thread::start to the right caller or run method
					boolean add = true;


					SDGNode sourceClone;
					if (!original_clone.containsKey(e.getSource())) {
						/**
						 * source of edge is in another procedure and target of edge is inside Thread::start()
						 * (we know the latter since either e.getSource() or e.getTarget() must be inside of
						 * Thread.start() and e.getSource() is not)
						 */
						sourceClone = e.getSource();

						if ((e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN)
								&& !callSiteOfThreadStart.contains(e.getSource())) {

							/**
							 * e is a call or param-in edge, but not from the call site of the
							 * current call to Thread::start()
							 * ==> do not clone e, since it does not connect the right caller to the current copy
							 **/

							add = false;

						} else if (e.getKind() == SDGEdge.Kind.FORK_OUT && !runsCalledFromThreadStart.contains(e.getSource())) {
							/**
							 * e is a fork-out coming from another thread than the one invoked by this copy of Thread::start()
							 * ==> do not clone e
							 */

							add = false;

						}
					} else {
						sourceClone = original_clone.get(e.getSource());
					}

					SDGNode targetClone;
					if (!original_clone.containsKey(e.getTarget())) {
						/**
						 * source of edge is inside of Thread::start() and
						 * target of edge is in another procedure
						 * ==> edge is param-out or return edge
						 */
						targetClone = e.getTarget();


						if ((e.getKind() == SDGEdge.Kind.PARAMETER_OUT || e.getKind() == SDGEdge.Kind.RETURN)
								&& !callSiteOfThreadStart.contains(e.getTarget())) {

							/**
							 * e is a (control or data) return edge, but it returns to a call site different
							 * from the one which called this copy of Thread::start()
							 * ==> do not clone e!
							 */
							add = false;

						} else if ((e.getKind() == SDGEdge.Kind.FORK_IN || e.getKind() == SDGEdge.Kind.FORK)
								&& !runsCalledFromThreadStart.contains(e.getTarget())) {

							/**
							 * e connects a fork site to a thread's entry method, but that thread is not the
							 * thread forked by this copy
							 * ==> do not clone e!
							 */
							add = false;
						}
					} else {
						targetClone = original_clone.get(e.getTarget());
					}

					if (add) {
						sdg.addEdge(new SDGEdge(sourceClone, targetClone, e.getKind()));
					}
				}
			}
		}

		// remove the spurious interprocedural edges from or to the original method

		/** all nodes belonging to the call site of the original Thread::start() method */
		Set<SDGNode> callSiteOfOrigThreadStart = callSite(firstCallOfThreadStart);

		/** all nodes belonging to the entries of all run methods called by the original Thread::start() method */
		Set<SDGNode> calledRunOfOrigThreadStart = calledRunsOfThreadStart(firstCallOfThreadStart);

		/** procedure id of original Thread::start() method */
		int origProc = threadStartNodes.iterator().next().getProc();

		/**
		 * iterate through all egdes of original Thread::start() method
		 */
		for (SDGEdge e : threadStartEdges) {

			if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN) {
				if (e.getSource().getProc() != origProc && !callSiteOfOrigThreadStart.contains(e.getSource())) {
					/** incoming egde does not belong to this copy of Thread::start  ==> e is spurious! */
					sdg.removeEdge(e);
				}

			} else if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT || e.getKind() == SDGEdge.Kind.RETURN) {
				/** e represents the return to the call site of the call to another Thread::start() copy ==> e is spurious! */
				if (e.getTarget().getProc() != origProc && !callSiteOfOrigThreadStart.contains(e.getTarget())) {
					sdg.removeEdge(e);
				}

			} else if (e.getKind() == SDGEdge.Kind.FORK || e.getKind() == SDGEdge.Kind.FORK_IN) {
				/** a thread different from the thread associated to this copy of Thread::start() is forked here ==> e is spurious! */
				if (!calledRunOfOrigThreadStart.contains(e.getTarget())) {
					sdg.removeEdge(e);
				}

			} else if (e.getKind() == SDGEdge.Kind.FORK_OUT) {
				if (!calledRunOfOrigThreadStart.contains(e.getSource())) {
					/** a thread different from the thread associated to this copy of Thread::start() "returns" here ==> e is spurious! */
					sdg.removeEdge(e);
				}
			}
		}

		removeSpuriousReturnEdges();
	}

	private SDGNode findExit(SDGNode entry) {
		assert entry.kind == SDGNode.Kind.ENTRY;
		LinkedList<SDGNode> w = new LinkedList<SDGNode>();
		Set<SDGNode> visited = new HashSet<SDGNode>();
		w.add(entry);
		while (!w.isEmpty()) {
			SDGNode next = w.poll();
			visited.add(next);
			if (next.getKind() == SDGNode.Kind.EXIT) {
				return next;
			} else {
				for (SDGEdge e : sdg.outgoingEdgesOf(next)) {
					if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW || e.getKind() == SDGEdge.Kind.NO_FLOW || e.getKind() == SDGEdge.Kind.JUMP_FLOW) {
						w.add(e.getTarget());
					}
				}
			}
		}

		throw new IllegalStateException("no exit node cf-reachable from an entry node?!");

	}

	private void removeSpuriousReturnEdges() {
		Map<SDGNode, Set<SDGNode>> calleeExit_possibleRetTargets = new HashMap<SDGNode, Set<SDGNode>>();
		List<SDGEdge> toRemove = new LinkedList<SDGEdge>();

		/**
		 * first pass: determine for each run method from which Thread::start() methods it was called
		 */
		for (SDGEdge edge : sdg.edgeSet()) {
			if (edge.getKind() == SDGEdge.Kind.FORK) {
				SDGNode calleeExit = findExit(edge.getTarget());
				assert sdg.getOutgoingEdgesOfKind(edge.getSource(), SDGEdge.Kind.CONTROL_FLOW).size() == 1;
				SDGNode callRetNode = sdg.getOutgoingEdgesOfKind(edge.getSource(), SDGEdge.Kind.CONTROL_FLOW).get(0).getTarget();
				Set<SDGNode> possibleRetTargets;
				if (!calleeExit_possibleRetTargets.containsKey(calleeExit)) {
					possibleRetTargets = new HashSet<SDGNode>();
					calleeExit_possibleRetTargets.put(calleeExit, possibleRetTargets);
				} else {
					possibleRetTargets = calleeExit_possibleRetTargets.get(calleeExit);
				}
				possibleRetTargets.add(callRetNode);
			}
		}

		/**
		 * second pass: collect all spurious return edges
		 */
		for (SDGEdge edge : sdg.edgeSet()) {
			if (edge.getKind() == SDGEdge.Kind.RETURN) {
				if (calleeExit_possibleRetTargets.containsKey(edge.getSource())) {
					toRemove.add(edge);
				}
			}
		}

		for (SDGEdge edge : toRemove) {
			sdg.removeEdge(edge);
		}
	}

	/**
	 * Given a call node, returns that call node together with all nodes (transitively) connected by control-expression dependencies,
	 * i.e. nodes representing the parameters and their parts.
	 * @param call given call site
	 * @return call node and all nodes representing the parameters of the calls and their parts
	 */
	private Set<SDGNode> callSite(SDGNode call) {
		return getAllNodesOfExpression(call);
	}

	/**
	 * Given an expression node, returns the reflexive-transitive closure with respect to control-expression
	 * dependencies.
	 * @param exprNode node for which the control-expression closure is to be computed
	 * @return the reflexive-transitive closure of the given node with respect to control-expression
	 * dependencies
	 */
	private Set<SDGNode> getAllNodesOfExpression(SDGNode exprNode) {
		return getAllNodesOfExpressions(Collections.singleton(exprNode));
	}

	/**
	 * Given a collection of expression nodes, returns the reflexive-transitive closure with respect to control-expression
	 * dependencies.
	 * @param exprNodes nodes for which the control-expression closure is to be computed
	 * @return the reflexive-transitive closure of the given node with respect to control-expression
	 * dependencies
	 */
	private Set<SDGNode> getAllNodesOfExpressions(Collection<SDGNode> exprNodes) {
		Set<SDGNode> result = new HashSet<SDGNode>();
		LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();

		worklist.addAll(exprNodes);
		result.addAll(exprNodes);

		while (!worklist.isEmpty()) {
			SDGNode next = worklist.poll();
			for (SDGEdge e : sdg.outgoingEdgesOf(next)) {
				if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR && !result.contains(e.getTarget())) {
					worklist.add(e.getTarget());
					result.add(e.getTarget());
				}
			}
		}

		return result;
	}



	/**
	 * Given a call node of Thread::start(), returns for each run method called by Thread::start() the entry nodes together with all nodes
	 * (transitively) connected by control-expression dependencies, i.e. the nodes representing the entry and the formal parameters of each
	 * run()-method called by the given call.
	 * @param call given call site
	 * @return for each run method called by Thread::start() the entry nodes together with all nodes representing the parameters and
	 * their parts.
	 */
	private Set<SDGNode> calledRunsOfThreadStart(SDGNode call) {
		return getAllNodesOfExpressions(callStart_run.get(call));
	}
}
