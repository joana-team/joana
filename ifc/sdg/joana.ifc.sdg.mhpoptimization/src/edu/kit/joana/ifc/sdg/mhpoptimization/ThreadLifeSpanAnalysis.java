/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.mhpoptimization;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
public class ThreadLifeSpanAnalysis {
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
	public ThreadLifeSpanAnalysis(SDG g) {
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
}
