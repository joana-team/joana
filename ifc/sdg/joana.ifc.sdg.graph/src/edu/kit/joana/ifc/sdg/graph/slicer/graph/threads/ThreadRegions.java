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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.Sets;
import com.ibm.wala.util.collections.SimpleVector;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGSlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.collections.ArrayMap;
import edu.kit.joana.util.collections.ArraySet;
import edu.kit.joana.util.collections.Arrays;
import edu.kit.joana.util.collections.ModifiableArraySet;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;


/**
 * @author giffhorn
 *
 */
public class ThreadRegions implements Iterable<ThreadRegion> {

	/** the control flow graph used to determine the thread regions */
	private final CFG icfg;

	/** the thread regions of the control flow graph */
	private final ArrayList<ThreadRegion> regions;
	

	/** a map 
	 *    thread -> (thread Regions)
	 *  from thread its to the threads regions of the control flow graph
	 */
	private final SimpleVector<List<ThreadRegion>> thread2regions;

	/** maps thread -> (node of thread -> thread region of node) */
	private static interface ThreadNodeRegionMap {
		ThreadRegion getRegion(int thread, SDGNode node);
	}

	private static class PreciseThreadNodeRegionMap implements ThreadNodeRegionMap {
		private TIntObjectHashMap<Map<SDGNode, ThreadRegion>> map;

		private PreciseThreadNodeRegionMap(TIntObjectHashMap<Map<SDGNode, ThreadRegion>> map) {
			this.map = map;
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegions.ThreadNodeRegionMap#getRegion(int, edu.kit.joana.ifc.sdg.graph.SDGNode)
		 */
		@Override
		public ThreadRegion getRegion(int thread, SDGNode node) {
			return map.get(thread).get(node);
		}
	}

	private static class SimpleThreadNodeRegionMap implements ThreadNodeRegionMap {
		private SimpleVector<List<ThreadRegion>> regions;

		private SimpleThreadNodeRegionMap(SimpleVector<List<ThreadRegion>> regions) {
			this.regions = regions;
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegions.ThreadNodeRegionMap#getRegion(int, edu.kit.joana.ifc.sdg.graph.SDGNode)
		 */
		@Override
		public ThreadRegion getRegion(int thread, SDGNode node) {
			return regions.get(thread).get(0);
		}
	}

	/** map thread -> (node of thread -> thread region of node) */
	private final ThreadNodeRegionMap map;

	protected ThreadRegions(SimpleVector<List<ThreadRegion>> thread2regions, CFG icfg, ThreadNodeRegionMap map) {
		this.thread2regions = thread2regions;
		this.icfg = icfg;
		this.map = map;

		this.regions = new ArrayList<>(); // TODO: pre-calc exact size
		int id = 0;
		for (List<ThreadRegion> regionsFoCurrentThread : thread2regions) {
			for (ThreadRegion region : regionsFoCurrentThread) {
				assert region.getID() == id;
				regions.add(region);
				id++;
			}
			
		}
		regions.trimToSize();

		assert verify();
	}

	private boolean verify() {
		for (ThreadRegion tr : regions) {
			if (!tr.verify()) {
				return false;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<ThreadRegion> iterator() {
		return regions.iterator();
	}

	public Collection<ThreadRegion> getThreadRegions() {
		return regions;
	}

	public ThreadRegion getThreadRegion(int id) {
		final ThreadRegion region = regions.get(id);
		if (region.getID() != id) throw new RuntimeException("Invalid Order");
		return region;
	}

	public Collection<ThreadRegion> getAllThreadRegions(SDGNode node) {
		return getThreadRegions(node);
	}

	/**
	 * Returns all thread regions belonging to the given thread.
	 * @param thread thread to determine thread regions of
	 * @return all thread regions belonging to the given thread
	 */
	public List<ThreadRegion> getThreadRegionSet(int thread) {
		//assert (thread2regions.get(thread) == null && getThreadRegionSetSlow(thread).isEmpty())
		assert thread2regions.get(thread).equals(getThreadRegionSetSlow(thread));
		
		return thread2regions.get(thread);
	}
	
	private List<ThreadRegion> getThreadRegionSetSlow(int thread) {
		LinkedList<ThreadRegion> result = new LinkedList<ThreadRegion>();

		for (ThreadRegion tr : regions) {
			if (tr.getThread() == thread) {
				result.add(tr);
			}
		}

		return result;
	}


	/**
	 * Returns the thread region of the given thread, which the given node belongs to. Note, that the given node has
	 * to be part of the given thread.
	 * @param node node to determine the thread region of
	 * @param thread thread to determine the thread region of
	 * @return
	 */
	public ThreadRegion getThreadRegion(SDGNode node, int thread) {
		if (!node.isInThread(thread)) {
			throw new IllegalArgumentException();
		}

		return map.getRegion(thread, node);
	}

	/**
	 * Returns all thread regions which the given node belongs to.
	 * @param node node to determine thread regions of
	 * @return all thread regions which the given node belongs to
	 */
	public List<ThreadRegion> getThreadRegions(SDGNode node) {
		LinkedList<ThreadRegion> result = new LinkedList<ThreadRegion>();

		for (int thread : node.getThreadNumbers()) {
			result.add(getThreadRegion(node, thread));
		}

		return result;
	}

	public ThreadRegion getThreadRegion(VirtualNode node) {
		return getThreadRegion(node.getNode(), node.getNumber());
	}

	/**
	 * Returns true if source may reach target.
	 * Needed for backwards compatibility.
	 * @param source
	 * @param target
	 * @return
	 */
	 public boolean reaches(SDGNode source, SDGNode target) {
		 LinkedList<SDGNode> w1 = new LinkedList<SDGNode>();
		 LinkedList<SDGNode> w2 = new LinkedList<SDGNode>();
		 HashSet<SDGNode> marked = new HashSet<SDGNode>();

		 w1.add(source);
		 marked.add(source);

		 while (!w1.isEmpty()) {
			 SDGNode next = w1.poll();

			 if (next == target) return true;

			 for (SDGEdge edge : icfg.outgoingEdgesOf(next)) {
				 if (edge.getKind() == SDGEdge.Kind.CALL || edge.getKind() == SDGEdge.Kind.FORK) {
					 if (marked.add(edge.getTarget())) {
					 w2.addFirst(edge.getTarget());
				 }
			 } else if (marked.add(edge.getTarget())) {
					 // don't traverse other region start nodes or already visited nodes
					 w1.addFirst(edge.getTarget());
				 }
			 }
		 }

		 while (!w2.isEmpty()) {
			 SDGNode next = w2.poll();

			 if (next == target) return true;

			 for (SDGEdge edge : icfg.outgoingEdgesOf(next)) {
				 if (edge.getKind() != SDGEdge.Kind.RETURN) {
					 if (marked.add(edge.getTarget())) {
						 w2.addFirst(edge.getTarget());
					 }
				 }
			 }
		 }

		 return false;
	 }

	 public int size() {
		 return regions.size();
	 }

	 /**
	  * Prints the ThreadRegions in attribute 'regions'.
	  */
	 public String toString() {
		 String str = "Thread Regions: "+regions.size()+"\n";
		 for (ThreadRegion r : regions) {
			 str += r.toString() + "-----------------------------\n";
		 }

		 return str;
	 }



	 /* Factories */

	 public static ThreadRegions createPreciseThreadRegions(SDG sdg) {
		 ThreadsInformation info = sdg.getThreadsInfo();
		 CFG icfg = ICFGBuilder.extractICFG(sdg);
		 ThreadRegions tr = ThreadRegions.createPreciseThreadRegions(icfg, info);

		 return tr;
	 }

	 public static ThreadRegions allThreadsParallel(SDG sdg) {
		 ThreadsInformation info = sdg.getThreadsInfo();
		 CFG icfg = ICFGBuilder.extractICFG(sdg);
		 ThreadRegions tr = ThreadRegions.allThreadsParallel(icfg, info);

		 return tr;
	 }

	 public static ThreadRegions createPreciseThreadRegions(CFG icfg, ThreadsInformation info) {
		 //        System.out.println("    computing regions...");
		 RegionBuilder builder = new RegionBuilder(icfg, info);
		 ThreadRegions tr = builder.computeRegions();
		 //        System.out.println("    ...done");
		 return tr;
	 }

	 public static ThreadRegions allThreadsParallel(CFG icfg, ThreadsInformation info) {
		 /** map thread -> (thread Regions) */
		 final SimpleVector<List<ThreadRegion>> regions = new SimpleVector<>();

		 for (ThreadInstance ti : info) {
			 //    		System.out.println(ti);
			 ThreadRegion r = new ThreadRegion(ti.getId(), ti.getEntry(), ti.getId(), ti.isDynamic(), new ModifiableArraySet<SDGNode>(SDGNode.class));
			 LinkedList<ThreadRegion> singleton = new LinkedList<>();
			 singleton.add(r);
			 regions.set(ti.getId(), singleton);
		 }

		 for (SDGNode n : icfg.vertexSet()) {
			 for (int t : n.getThreadNumbers()) {
				 regions.get(t).get(0).getNodes().add(n);
			 }
		 }

		 ThreadRegions tr = new ThreadRegions(regions, icfg, new SimpleThreadNodeRegionMap(regions));

		 return tr;
	 }

	 static class RegionBuilder {
		 private final CFG icfg;
		 private final ThreadsInformation info;
		 /** map thread -> (thread Regions) */
		 private final SimpleVector<List<ThreadRegion>> regions;
		 
		 /** map thread -> (node of thread -> thread region of node) */ 
		 private final TIntObjectHashMap<Map<SDGNode, ThreadRegion>> map;
		 
		 /** map (node -> thread region of node) */ 
		 private final Map<SDGNode, GlobalThreadRegion> globalMap;
		 
		 /** map (thread start-node -> thread-region-start-node -> Set <thread-region-start-node> ) */ 
		 private final Map<SDGNode, Map<SDGNode, Set<SDGNode>>> globalDirectSuccessorStartNodesForThread;
		 
		 /** map (                     thread-region-start-node -> Set <thread-region-start-node> ) */ 
		 private final Map<SDGNode, Set<SDGNode>> globalDirectSuccessorStartNodes;

		 private int id;

		 private RegionBuilder(CFG icfg, ThreadsInformation info) {
			 this.icfg = icfg;
			 this.info = info;
			 regions = new SimpleVector<>();
			 map = new TIntObjectHashMap<>();
			 globalMap = new edu.kit.joana.util.collections.SimpleVector<>(10, icfg.lastId());
			 globalDirectSuccessorStartNodesForThread = new HashMap<>();
			 globalDirectSuccessorStartNodes = new HashMap<>();
			 id = 0;
		 }

		 public ThreadRegions computeRegions() {
			 computeStartNodesGlobal();
			 computeRegionsGlobal(computeStartNodesGlobal());
			 for (int thread = 0; thread < info.getNumberOfThreads(); thread++) {
				 computeRegions(thread);
			 }
			 
			 for (int thread = 0; thread < info.getNumberOfThreads(); thread++) {
				 Set<SDGNode> startNodes = initialStartNodes(thread);

				 Color MARKED = new Color();
				 final Color INIT = new Color();
				 Set<Color> PREVIOUSLY_MARKED;

				 boolean newInit;
				 do {
					 newInit = false;
					 
					 SDGNode[] init = startNodes.toArray(new SDGNode[startNodes.size()]);
					 
					 for (SDGNode n : init) {
						 n.customData = INIT;
					 }
					 
					 PREVIOUSLY_MARKED = new HashSet<>();

					 
					 for (SDGNode node : init) {
						 //final Map<SDGNode, Set<SDGNode>> globalDirectSuccessorStartNodes = globalDirectSuccessorStartNodesForThread.get(node);
						 Set<SDGNode> markedStart = new HashSet<>();
						 markedStart.add(node);
						 LinkedList<SDGNode> workList = new LinkedList<>();
						 workList.add(node);
						 
						 MARKED = new Color();
						 
						 while (!workList.isEmpty()) {
							 final SDGNode next = workList.poll();
							 
							 Set<SDGNode> successors = globalDirectSuccessorStartNodes.get(next);
							 
							 if (successors == null) continue;
							 
							 for (SDGNode reached : successors) {
								 if (!reached.isInThread(thread)) continue;
								 if (reached.customData == INIT) continue;

								 if (PREVIOUSLY_MARKED.contains(reached.customData)) {
									 newInit |= startNodes.add(reached);
									 continue;
								 }
								 if (reached.customData != MARKED) {
									 reached.customData = MARKED;
									 workList.add(reached);
								 }
							 }
						 }
							 
						 PREVIOUSLY_MARKED.add(MARKED);
					 }

				 } while (newInit);
				 
				 
				 final Set<SDGNode> regionsStartNodes = new HashSet<>(regions.get(thread).size());
				 for (ThreadRegion r : regions.get(thread)) {
					 boolean changed = regionsStartNodes.add(r.getStart());
					 if (!changed) {
						 throw new AssertionError();
					 }
				 }
				 if (!startNodes.equals(regionsStartNodes)) {
					 Set<SDGNode> globalStartNodess = new TreeSet<>(SDGNode.getIDComparator());
					 globalStartNodess.addAll(startNodes);
					 Set<SDGNode> regionStartNodess = new TreeSet<>(SDGNode.getIDComparator());
					 regionStartNodess.addAll(regionsStartNodes);
					 throw new AssertionError();
				 }
				 
				 final Set<SDGNode> inThreadViaGlobalThreadRegions = new HashSet<>();
				 
				 final ThreadInstance threadInstance = info.getThread(thread);
				 //final Map<SDGNode, Set<SDGNode>> globalDirectSuccessorStartNodes = globalDirectSuccessorStartNodesForThread.get(threadInstance.getEntry());

				 
				 for (SDGNode startNode : startNodes) {
					 Set<SDGNode> startNodesInSameRegion = new HashSet<>();
					 startNodesInSameRegion.add(startNode);
					 LinkedList<SDGNode> workList = new LinkedList<>();
					 workList.add(startNode);
					 while (!workList.isEmpty()) {
						 SDGNode next = workList.poll();
						 final Set<SDGNode> directSuccessors =  globalDirectSuccessorStartNodes.get(next);
						 if (directSuccessors == null) continue;
						 for (SDGNode successor : directSuccessors) {
							 if (successor.isInThread(thread) && !startNodes.contains(successor) && startNodesInSameRegion.add(successor)) {
								 workList.add(successor);
							 }
						 }
					 }
					 
					 for (SDGNode startNodeInSameRegion : startNodesInSameRegion) {
						 final GlobalThreadRegion globalThreadRegion = globalMap.get(startNodeInSameRegion);
							 
						 
						 assert globalThreadRegion.getStart() == startNodeInSameRegion;
						 
						 if (!(Collections.disjoint(inThreadViaGlobalThreadRegions, globalThreadRegion.getNodes()))) {
							 throw new AssertionError();
						 }
						 
						 inThreadViaGlobalThreadRegions.addAll(globalThreadRegion.getNodes());
					 }
					 
				 }
				 
				 Set<SDGNode> inThreadViaThreadRegion = new HashSet<>();
				 for (ThreadRegion region : regions.get(thread)) {
					 if (!(Collections.disjoint(inThreadViaThreadRegion, region.getNodes()))) {
						 throw new AssertionError();
					 }
					 inThreadViaThreadRegion.addAll(region.getNodes());
				 }
				 
				 if (!(inThreadViaGlobalThreadRegions.equals(inThreadViaThreadRegion))) {
					 throw new AssertionError();
				 }
			 }

			 // collect nodes without a thread region
			 List<SDGNode> nodesWithoutRegions = new LinkedList<SDGNode>();
			 Set<Integer> threadsWithoutRegions = new HashSet<Integer>();
			 for (SDGNode node : icfg.vertexSet()) {
				 boolean nodeDangling = false;
				 for (int threadId : node.getThreadNumbers()) {
					 if (!map.containsKey(threadId)) {
						 threadsWithoutRegions.add(threadId);
						 nodeDangling = true;
					 } else if (!map.get(threadId).containsKey(node.getId())) {
						 nodeDangling = true;
					 }
				 }

				 if (nodeDangling) {
					 nodesWithoutRegions.add(node);
				 }


			 }

			 final Logger debug = Log.getLogger(Log.L_MHP_DEBUG);
			 debug.outln("threads without regions: " + threadsWithoutRegions);
			 debug.outln("nodes without thread regions: " + nodesWithoutRegions);

			 return new ThreadRegions(regions, icfg, new PreciseThreadNodeRegionMap(map));
		 }

		 /**
		  * Computes the thread regions of the given CFG.
		  * First, all the region starts are computed using a fix-point iteration,
		  * then the ThreadRegions are initialized.
		  * The fix-point iteration is needed to determine the merge nodes of
		  * different thread regions, which are region start nodes themselves.
		  *
		  */
		 private void computeRegions(int thread) {
			 HashSet<SDGNode> startNodes = computeStartNodes(thread);
			 regions.set(thread, computeRegions(startNodes, thread));
		 }

		 /**
		  * Computes the start nodes of the thread regions of the given thread. A thread region either starts at
		  * a fork site or at a join site or at a point, where to thread regions meet. So, the algorithm first
		  * adds all fork and join sites of the given thread and then successively finds nodes which are reachable
		  * from at least to distinct start nodes. Since each start node represents a different thread region, these
		  * nodes are points where two distinct thread regions meet, so they are added to the set of start regions. Since
		  * new start nodes could be discovered after a new start node was added, this operation has to be iterated until
		  * a fixpoint is reached.
		  * @param thread
		  * @return
		  */
		 private HashSet<SDGNode> computeStartNodesSlow(int thread) {
			 // initial start nodes
			 HashSet<SDGNode> init = new HashSet<SDGNode>();
			 init.add(info.getThreadEntry(thread));
			 Collection<SDGNode> forks = info.getAllForks();

			 for (SDGNode fork : forks) {
				 for (SDGEdge e: icfg.getOutgoingEdgesOfKindUnsafe(fork, SDGEdge.Kind.CONTROL_FLOW)) {
					 if (e.getTarget().isInThread(thread)) {
						 init.add(e.getTarget());
					 }
				 }
			 }

			 Collection<SDGNode> joins = info.getAllJoins();

			 for (SDGNode join : joins) {
				 if (join.isInThread(thread)) {
					 init.add(join);
				 }
			 }

			 // refine start nodes
			 HashSet<SDGNode> result = new HashSet<SDGNode>();
			 result.addAll(init);


			 /**
			  * successively add new nodes to the result set, which are reached by at least two start nodes,
			  * until the result set does not change anymore
			  */
			 do {
				 init.addAll(result);

				 HashSet<SDGNode> previouslyMarked = new HashSet<SDGNode>();

				 for (SDGNode node : init) {
					 LinkedList<SDGNode> w1 = new LinkedList<SDGNode>();
					 LinkedList<SDGNode> w2 = new LinkedList<SDGNode>();
					 HashSet<SDGNode> marked = new HashSet<SDGNode>();

					 w1.add(node);
					 marked.add(node);

					 while (!w1.isEmpty()) {
						 SDGNode next = w1.poll();

						 for (SDGEdge edge : icfg.outgoingEdgesOf(next)) {
							 if (edge.getKind() == SDGEdge.Kind.FORK
									 || (edge.getKind() == SDGEdge.Kind.RETURN && !edge.getTarget().isInThread(thread))) {
								 // don't leave the thread
								 continue;

							 } else {
								 SDGNode reached = edge.getTarget();
								 if (!reached.isInThread(thread)) throw new RuntimeException("Error at edge "+edge);

								 // don't cross thread region borders
								 if (init.contains(reached)) continue;

								 /**
								  * the reached node is reached from two different nodes in the current start set
								  * ---> another start point of a region for the current thread has been found
								  */
								 if (previouslyMarked.contains(reached)) {
									 result.add(reached);
									 continue;
								 }

								 if (marked.add(reached)) {
									 // 2-phase slicing
									 if (edge.getKind() != SDGEdge.Kind.CALL) {
										 w1.addFirst(reached);

									 } else {
										 w2.addFirst(reached);
									 }
								 }
							 }
						 }
					 }

					 while (!w2.isEmpty()) {
						 SDGNode next = w2.poll();

						 for (SDGEdge edge : icfg.outgoingEdgesOf(next)) {
							 if (edge.getKind() == SDGEdge.Kind.FORK
									 || edge.getKind() == SDGEdge.Kind.RETURN) {
								 // don't leave the thread, don't leave procedures
								 continue;

							 } else {
								 SDGNode reached = edge.getTarget();
								 if (!reached.isInThread(thread)) throw new RuntimeException("Error at edge "+edge);

								 // don't cross thread region borders
								 if (init.contains(reached)) continue;

								 /**
								  * the reached node is reached from two different nodes in the current start set
								  * ---> another start point of a region for the current thread has been found
								  */
								 if (previouslyMarked.contains(reached)) {
									 result.add(reached);
									 continue;
								 }

								 if (marked.add(reached)) {
									 w2.addFirst(reached);
								 }
							 }
						 }
					 }

					 previouslyMarked.addAll(marked);
				 }

			 } while (result.size() > init.size());

			 return result;
		 }

		 /**
		  * Computes the start nodes of the thread regions of the given thread. A thread region either starts at
		  * a fork site or at a join site or at a point, where to thread regions meet. So, the algorithm first
		  * adds all fork and join sites of the given thread and then successively finds nodes which are reachable
		  * from at least to distinct start nodes. Since each start node represents a different thread region, these
		  * nodes are points where two distinct thread regions meet, so they are added to the set of start regions. Since
		  * new start nodes could be discovered after a new start node was added, this operation has to be iterated until
		  * a fixpoint is reached.
		  * @param thread
		  * @return
		  */
		 private static class Color {}
		 
		 private HashSet<SDGNode> initialStartNodes(int thread) {
			 // initial start nodes
			 HashSet<SDGNode> result = new HashSet<SDGNode>();
			 
			 result.add(info.getThreadEntry(thread));
			 
			 Collection<SDGNode> forks = info.getAllForks();
			 for (SDGNode fork : forks) {
				 for (SDGEdge e: icfg.getOutgoingEdgesOfKindUnsafe(fork, SDGEdge.Kind.CONTROL_FLOW)) {
					 if (e.getTarget().isInThread(thread)) {
						 result.add(e.getTarget());
					 }
				 }
			 }

			 Collection<SDGNode> joins = info.getAllJoins();

			 for (SDGNode join : joins) {
				 if (join.isInThread(thread)) {
					 result.add(join);
				 }
			 }
			 
			 return result;
		 }
		 
		 private HashSet<SDGNode> computeStartNodes(int thread) {
			 final Color INIT = new Color();
			 final Color BOTH = new Color();
			       Color MARKED = new Color();
			       Set<Color> PREVIOUSLY_MARKED;
			 
			HashSet<SDGNode> result = initialStartNodes(thread);

			 /**
			  * successively add new nodes to the result set, which are reached by at least two start nodes,
			  * until the result set does not change anymore
			  */
			 boolean newInit;
			 do {
				 newInit = false;
				 
				 SDGNode[] init = result.toArray(new SDGNode[result.size()]);
				 
				 for (SDGNode n : init) {
					 n.customData = INIT;
				 }
				 
				 PREVIOUSLY_MARKED = new HashSet<>();

				 for (SDGNode node : init) {
					 LinkedList<SDGNode> w1 = new LinkedList<SDGNode>();
					 LinkedList<SDGNode> w2 = new LinkedList<SDGNode>();
					 MARKED = new Color();
					 

					 w1.add(node);

					 while (!w1.isEmpty()) {
						 SDGNode next = w1.poll();

						 for (SDGEdge edge : icfg.outgoingEdgesOfUnsafe(next)) {
							 if (edge.getKind() == SDGEdge.Kind.FORK
									 || (edge.getKind() == SDGEdge.Kind.RETURN && !edge.getTarget().isInThread(thread))) {
								 // don't leave the thread
								 continue;

							 } else {
								 SDGNode reached = edge.getTarget();
								 assert reached.isInThread(thread); // otherwise, we would've left the thread

								 // don't cross thread region borders
								 if (reached.customData == INIT || reached.customData == BOTH) continue;

								 /**
								  * the reached node is reached from two different nodes in the current start set
								  * ---> another start point of a region for the current thread has been found
								  */
								 if (PREVIOUSLY_MARKED.contains(reached.customData)) {
									 newInit |= result.add(reached);
									 continue;
								 }

								 if (reached.customData != MARKED) {
									 reached.customData = MARKED;
									 // 2-phase slicing
									 if (edge.getKind() != SDGEdge.Kind.CALL) {
										 w1.addFirst(reached);

									 } else {
										 w2.addFirst(reached);
									 }
								 }
							 }
						 }
					 }

					 while (!w2.isEmpty()) {
						 SDGNode next = w2.poll();

						 for (SDGEdge edge : icfg.outgoingEdgesOfUnsafe(next)) {
							 if (edge.getKind() == SDGEdge.Kind.FORK
									 || edge.getKind() == SDGEdge.Kind.RETURN) {
								 // don't leave the thread, don't leave procedures
								 continue;

							 } else {
								 SDGNode reached = edge.getTarget();
								 assert reached.isInThread(thread); // otherwise, we would've left the thread

								 // don't cross thread region borders
								 if (reached.customData == INIT || reached.customData == BOTH) continue;

								 /**
								  * the reached node is reached from two different nodes in the current start set
								  * ---> another start point of a region for the current thread has been found
								  */
								 if (PREVIOUSLY_MARKED.contains(reached.customData)) {
									 newInit |= result.add(reached);
									 continue;
								 }

								 if (reached.customData != MARKED) {
									 reached.customData = MARKED;
									 w2.addFirst(reached);
								 }
							 }
						 }
					 }

					 PREVIOUSLY_MARKED.add(MARKED);

				 }

			 } while (newInit);
			 
			 assert result.equals(computeStartNodesSlow(thread));
			 
			 return result;
		 }
		 
		 private static void putAllThreads(Map<SDGNode, ModifiableArraySet<Integer>> threadsOf, SDGNode node) {
			 threadsOf.compute(node, (k, threads) -> {
				 if (threads == null) {
					 threads = new ModifiableArraySet<>(Integer.class);
				 }

				 for (int thread : node.getThreadNumbers()) {
					 threads.add(thread);
				 }

				 return threads;
			 });
		 }
		 
		 private HashSet<SDGNode> computeStartNodesGlobal() {
			 final Color INIT = new Color();
			 final Color BOTH = new Color();
			       Color MARKED = new Color();
			       Set<Color> PREVIOUSLY_MARKED;
			 

			 // initial start nodes
			 HashSet<SDGNode> result = new HashSet<SDGNode>();
			 
			 Map<SDGNode, ModifiableArraySet<Integer>> threadsOf = new HashMap<>();
			 
			 result.addAll(info.getAllEntries());
			 
			 Collection<SDGNode> forks = info.getAllForks();
			 for (SDGNode fork : forks) {
				 for (SDGEdge e: icfg.getOutgoingEdgesOfKindUnsafe(fork, SDGEdge.Kind.CONTROL_FLOW)) {
					result.add(e.getTarget());
					putAllThreads(threadsOf, e.getTarget());
				 }
			 }

			 Collection<SDGNode> joins = info.getAllJoins();

			 for (SDGNode join : joins) {
				 result.add(join);
				 putAllThreads(threadsOf, join);
			 }

			 /**
			  * successively add new nodes to the result set, which are reached by at least two start nodes,
			  * until the result set does not change anymore
			  */
			 boolean newInit;
			 do {
				 newInit = false;
				 
				 SDGNode[] init = result.toArray(new SDGNode[result.size()]);
				 
				 for (SDGNode n : init) {
					 n.customData = INIT;
				 }
				 
				 
				 PREVIOUSLY_MARKED = new HashSet<>();

				 for (SDGNode node : init) {
					 LinkedList<SDGNode> w1 = new LinkedList<SDGNode>();
					 LinkedList<SDGNode> w2 = new LinkedList<SDGNode>();
					 MARKED = new Color();
					 
					 final Set<Integer> threadsOfNode = threadsOf.get(node);
					 

					 w1.add(node);

					 while (!w1.isEmpty()) {
						 SDGNode next = w1.poll();

						 for (SDGEdge edge : icfg.outgoingEdgesOfUnsafe(next)) {
							 SDGNode reached = edge.getTarget();

							 assert (edge.getKind() != SDGEdge.Kind.FORK || (reached.customData == INIT || reached.customData == BOTH));

							 // don't cross thread region borders
							 if (reached.customData == INIT || reached.customData == BOTH) continue;
							 
							 // don't leave threads
							 if (edge.getKind() == SDGEdge.Kind.RETURN) {
								 final int[] threadNumberReached = reached.getThreadNumbers();
								 boolean containsAny = false;
								 for (int i = 0; i < threadNumberReached.length; i++) {
									 if (threadsOfNode.contains(threadNumberReached[i])) {
										 containsAny = true;
										 break;
									 }
								 }
								 if (!containsAny) continue;
							 }
							 /**
							  * the reached node is reached from two different nodes in the current start set
							  * ---> another start point of a region for the current thread has been found
							  */
							 if (PREVIOUSLY_MARKED.contains(reached.customData)) {
								 newInit |= result.add(reached);
								 if (newInit) {
										putAllThreads(threadsOf, reached);
								 }
								 continue;
							 }

							 if (reached.customData != MARKED) {
								 reached.customData = MARKED;
								 // 2-phase slicing
								 if (edge.getKind() != SDGEdge.Kind.CALL) {
									 w1.addFirst(reached);

								 } else {
									 w2.addFirst(reached);
								 }
							 }
						 }
					 }

					 while (!w2.isEmpty()) {
						 SDGNode next = w2.poll();

						 for (SDGEdge edge : icfg.outgoingEdgesOfUnsafe(next)) {
							 if (edge.getKind() == SDGEdge.Kind.RETURN) continue;
							 
							 SDGNode reached = edge.getTarget();
							 assert (edge.getKind() != SDGEdge.Kind.FORK || (reached.customData == INIT || reached.customData == BOTH));

							 // don't cross thread region borders
							 if (reached.customData == INIT || reached.customData == BOTH) continue;

							 /**
							  * the reached node is reached from two different nodes in the current start set
							  * ---> another start point of a region for the current thread has been found
							  */
							 if (PREVIOUSLY_MARKED.contains(reached.customData)) {
								 newInit |= result.add(reached);
								 if (newInit) {
									 threadsOf.compute(reached, (k, threads) -> {
										 if (threads == null) {
											 threads = new ModifiableArraySet<>(Integer.class);
										 }

										 for (int thread : reached.getThreadNumbers()) {
											 threads.add(thread);
										 }

										 return threads;
									 });
								 }
								 continue;
							 }

							 if (reached.customData != MARKED) {
								 reached.customData = MARKED;
								 w2.addFirst(reached);
							 }
						 }
					 }

					 PREVIOUSLY_MARKED.add(MARKED);

				 }

			 } while (newInit);
			 
			 return result;
		 }
		 
		 
		 
		 /**
		  * Computes the thread regions of the given thread. For each given start node, a context-sensitive, intra-thread
		  * forward slice in the control-flow graph is performed. All nodes belonging to such a slice form a thread region.
		  * @param startNodes start nodes to compute thread regions from
		  * @param thread thread for which the regions are to be determined
		  * @return all thread regions belonging to the given thread
		  */
		 private List<ThreadRegion> computeRegions(HashSet<SDGNode> startNodes, int thread) {
			 List<ThreadRegion> result = new ArrayList<>(startNodes.size());
			 
			 final Map<SDGNode, ThreadRegion> mappy = new HashMap<>();

			 final Color START = new Color();

			 for (SDGNode startNode : startNodes) {
				 startNode.customData = START;
			 }

			 for (SDGNode startNode : startNodes) {
				 LinkedList<SDGNode> w1 = new LinkedList<SDGNode>();
				 LinkedList<SDGNode> w2 = new LinkedList<SDGNode>();
				 HashSet<SDGNode> marked = new HashSet<SDGNode>();

				 w1.add(startNode);
				 marked.add(startNode);

				 while (!w1.isEmpty()) {
					 SDGNode next = w1.poll();

					 for (SDGEdge edge : icfg.outgoingEdgesOfUnsafe(next)) {
						 if (edge.getKind() == SDGEdge.Kind.FORK
								 || (edge.getKind() == SDGEdge.Kind.RETURN && !edge.getTarget().isInThread(thread))) {
							 // don't leave the thread
							 continue;

						 } else {
							 SDGNode reached = edge.getTarget();
							 assert reached.isInThread(thread); // otherwise, we would've left the thread

							 // don't cross thread region borders
							 assert (reached.customData == START) == startNodes.contains(reached); 
							 if (reached.customData == START) continue;

							 if (marked.add(reached)) {
								 // 2-phase slicing
								 if (edge.getKind() == SDGEdge.Kind.CALL) {
									 w2.addFirst(reached);

								 } else {
									 w1.addFirst(reached);
								 }
							 }
						 }
					 }
				 }

				 while (!w2.isEmpty()) {
					 SDGNode next = w2.poll();

					 for (SDGEdge edge : icfg.outgoingEdgesOfUnsafe(next)) {
						 if (edge.getKind() != SDGEdge.Kind.FORK
								 && edge.getKind() != SDGEdge.Kind.RETURN) {
							 // don't leave the thread, don't leave procedures
							 SDGNode reached = edge.getTarget();
							 assert reached.isInThread(thread); // otherwise, we would've left the thread

							 // don't cross thread region borders
							 assert (reached.customData == START) == startNodes.contains(reached);
							 if (reached.customData == START) continue;

							 if (!marked.contains(reached)) {
								 marked.add(reached);
								 w2.addFirst(reached);
							 }
						 }
					 }
				 }

				 // marked contains the nodes of the thread region
				 ThreadRegion tr = new ThreadRegion(id, startNode, thread, info.isDynamic(thread), new ArraySet<>(marked));
				 result.add(tr);

				 for (SDGNode n : marked) {
					 mappy.put(n, tr);
				 }
				 id++;
			 }
			 
			 assert !map.contains(thread);
			 map.put(thread, new ArrayMap<>(mappy));
			 
			 assert result.size() == startNodes.size();
			 return result;
		 }
		 
		 
		 
		 private class RegionsGlobalWorkListElement {
			 final SDGNode next;
			 final SDGNode lastStartNode;
			 final boolean inInitialStartNodesRegion;
			 RegionsGlobalWorkListElement(SDGNode next, SDGNode lastStartNode, boolean inInitialStartNodesRegion) {
				 this.next = next;
				 this.lastStartNode = lastStartNode;
				 this.inInitialStartNodesRegion = inInitialStartNodesRegion;
			 }
		 }
		 /**
		  * Computes the thread regions of the given thread. For each given start node, a context-sensitive, intra-thread
		  * forward slice in the control-flow graph is performed. All nodes belonging to such a slice form a thread region.
		  * @param startNodes start nodes to compute thread regions from
		  * @param thread thread for which the regions are to be determined
		  * @return all thread regions belonging to the given thread
		  */
		 private List<GlobalThreadRegion> computeRegionsGlobal(HashSet<SDGNode> startNodes) {
			 List<GlobalThreadRegion> result = new ArrayList<>(startNodes.size());
			 
			 
			 final Color START = new Color();

			 for (SDGNode startNode : startNodes) {
				 startNode.customData = START;
			 }

			 for (SDGNode startNode : startNodes) {
				 //final Map<SDGNode, Set<SDGNode>> globalDirectSuccessorStartNodes = new HashMap<>();
				 //globalDirectSuccessorStartNodesForThread.put(startNode, globalDirectSuccessorStartNodes); 
				 LinkedList<RegionsGlobalWorkListElement> w1 = new LinkedList<>();
				 LinkedList<RegionsGlobalWorkListElement> w2 = new LinkedList<>();
				 HashSet<SDGNode> inRegion = new HashSet<>();
				 HashSet<Pair<SDGNode, SDGNode>> behindRegion = new HashSet<>();

				 w1.add(new RegionsGlobalWorkListElement(startNode, startNode, true));
				 inRegion.add(startNode);
				 
				 final Set<Integer> threadsOfStartNode = new ModifiableArraySet<>(Integer.class);
				 for (int thread : startNode.getThreadNumbers()) {
					 threadsOfStartNode.add(thread);
				 }

				 while (!w1.isEmpty()) {
					 final RegionsGlobalWorkListElement element = w1.poll();
					 final SDGNode next = element.next;
					 final boolean nextInRegion = element.inInitialStartNodesRegion;
					 final SDGNode lastStartNode = element.lastStartNode;

					 for (SDGEdge edge : icfg.outgoingEdgesOfUnsafe(next)) {
						 final SDGNode reached = edge.getTarget();

						 assert (edge.getKind() != SDGEdge.Kind.FORK || reached.customData == START);
						 
						 // don't leave threads
						 if (edge.getKind() == SDGEdge.Kind.RETURN) {
							 final int[] threadNumberReached = reached.getThreadNumbers();
							 boolean containsAny = false;
							 for (int i = 0; i < threadNumberReached.length; i++) {
								 if (threadsOfStartNode.contains(threadNumberReached[i])) {
									 containsAny = true;
									 break;
								 }
							 }
							 if (!containsAny) continue;
						 }

						 // handle thread region borders
						 assert (reached.customData == START) == startNodes.contains(reached); 
						 if (reached.customData == START) {
							 globalDirectSuccessorStartNodes.compute(lastStartNode, (k, successors) -> {
								 if (successors == null) {
									 successors = new HashSet<>();
								 }
								 successors.add(reached);
								 
								 return successors;
							 });

//							 final Pair<SDGNode, SDGNode> reachedWithLastStartNode = Pair.pair(reached, reached);
//							 if (!inRegion.contains(reached) && behindRegion.add(reachedWithLastStartNode)) {
//								 // 2-phase slicing
//								 if (edge.getKind() == SDGEdge.Kind.CALL) {
//									 w2.addFirst(new RegionsGlobalWorkListElement(reached, reached, false));
//
//								 } else {
//									 w1.addFirst(new RegionsGlobalWorkListElement(reached, reached, false));
//								 }
//							 }
						 } else {
							 if (nextInRegion) {
								 if (inRegion.add(reached)) {
									 if (edge.getKind() == SDGEdge.Kind.CALL) {
										 w2.addFirst(new RegionsGlobalWorkListElement(reached, lastStartNode, nextInRegion));
		
									 } else {
										 w1.addFirst(new RegionsGlobalWorkListElement(reached, lastStartNode, nextInRegion));
									 }
								 }
							 } else {
//								 final Pair<SDGNode, SDGNode> reachedWithLastStartNode = Pair.pair(reached, lastStartNode);
//								 if (behindRegion.add(reachedWithLastStartNode)) {
//									 if (edge.getKind() == SDGEdge.Kind.CALL) {
//										 w2.addFirst(new RegionsGlobalWorkListElement(reached, lastStartNode, nextInRegion));
//		
//									 } else {
//										 w1.addFirst(new RegionsGlobalWorkListElement(reached, lastStartNode, nextInRegion));
//									 }
//								 }
							 }
						 }
					 }
				 }

				 while (!w2.isEmpty()) {
					 final RegionsGlobalWorkListElement element = w2.poll();
					 final SDGNode next = element.next;
					 final boolean nextInRegion = element.inInitialStartNodesRegion;
					 final SDGNode lastStartNode = element.lastStartNode;

					 for (SDGEdge edge : icfg.outgoingEdgesOfUnsafe(next)) {
						 if (edge.getKind() == SDGEdge.Kind.RETURN) continue;
						 SDGNode reached = edge.getTarget();
						 
						 assert (edge.getKind() != SDGEdge.Kind.FORK || reached.customData == START);

						 // handle thread region borders
						 assert (reached.customData == START) == startNodes.contains(reached);
						 if (reached.customData == START) {
							 globalDirectSuccessorStartNodes.compute(lastStartNode, (k, successors) -> {
								 if (successors == null) {
									 successors = new HashSet<>();
								 }
								 successors.add(reached);
								 
								 return successors;
							 });
							 
//							 final Pair<SDGNode, SDGNode> reachedWithLastStartNode = Pair.pair(reached, reached);
//							 if (!inRegion.contains(reached) && behindRegion.add(reachedWithLastStartNode)) {
//								 w2.addFirst(new RegionsGlobalWorkListElement(reached, reached, false));
//							 }
						 } else {
							 if (nextInRegion) {
								 if (inRegion.add(reached)) {
									 w2.addFirst(new RegionsGlobalWorkListElement(reached, lastStartNode, nextInRegion));
								 }
							 } else {
//								 final Pair<SDGNode, SDGNode> reachedWithLastStartNode = Pair.pair(reached, lastStartNode);
//								 if (behindRegion.add(reachedWithLastStartNode)) {
//									 w2.addFirst(new RegionsGlobalWorkListElement(reached, lastStartNode, nextInRegion));
//								 }
							 }
						 }
					 }
				 }

				 // marked contains the nodes of the thread region
				 GlobalThreadRegion tr = new GlobalThreadRegion(startNode, new ArraySet<>(inRegion));
				 result.add(tr);

				 for (SDGNode n : inRegion) {
					 assert !globalMap.containsKey(n);
					 globalMap.put(n, tr);
				 }
			 }
			 
			 assert result.size() == startNodes.size();
			 return result;
		 }
	 }



	 /* DEBUG */
	 public static void main(String[] args) throws Exception {
		 String str = "/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/";
		 str += "conc.TimeTravel.pdg";
		 //    	str += "conc.Testi.pdg";
		 //    	str += "conc.ac.AlarmClock.pdg";


		 SDG g = SDG.readFrom(str);
		 //      ThreadRegionsNew tr = ThreadRegionsNew.allThreadsParallel(g);
		 ThreadRegions tr = ThreadRegions.createPreciseThreadRegions(g);
		 System.out.println(tr);
	 }
}
