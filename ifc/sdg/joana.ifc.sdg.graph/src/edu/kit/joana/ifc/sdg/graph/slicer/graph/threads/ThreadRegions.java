/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.threads;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import gnu.trove.map.hash.TIntObjectHashMap;


/**
 * @author giffhorn
 *
 */
public class ThreadRegions implements Iterable<ThreadRegion> {

	/** the control flow graph used to determine the thread regions */
	private CFG icfg;

	/** the thread regions of the control flow graph */
	private List<ThreadRegion> regions;


	/** map thread -> (node of thread -> thread region of node) */
	private TIntObjectHashMap<TIntObjectHashMap<ThreadRegion>> map;

	protected ThreadRegions(List<ThreadRegion> regions, CFG icfg, TIntObjectHashMap<TIntObjectHashMap<ThreadRegion>> map) {
		this.regions = regions;
		this.icfg = icfg;
		this.map = map;
		verify();
	}

	private void verify() {
		for (ThreadRegion tr : regions) {
			tr.verify();
		}
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
		if (regions.get(id).getID() != id) throw new RuntimeException("Invalid Order");
		return regions.get(id);
	}

	public Collection<ThreadRegion> getAllThreadRegions(SDGNode node) {
		LinkedList<ThreadRegion> result = new LinkedList<ThreadRegion>();

		for (TIntObjectHashMap<ThreadRegion> subMap : map.valueCollection()) {
			ThreadRegion tr = subMap.get(node.getId());
			if (tr != null) {
				result.add(tr);
			}
		}

		return result;
	}

	/**
	 * Returns all thread regions belonging to the given thread.
	 * @param thread thread to determine thread regions of
	 * @return all thread regions belonging to the given thread
	 */
	public List<ThreadRegion> getThreadRegionSet(int thread) {
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

		assert map.containsKey(thread);
		assert map.get(thread).containsKey(node.getId());
		return map.get(thread).get(node.getId());
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
		return map.get(node.getNumber()).get(node.getNode().getId());
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
				 if (edge.getKind() != SDGEdge.Kind.CALL && edge.getKind() != SDGEdge.Kind.FORK) {
					 // don't traverse other region start nodes or already visited nodes
					 if (marked.add(edge.getTarget())) {
						 w1.addFirst(edge.getTarget());
					 }

				 } else if (marked.add(edge.getTarget())) {
					 w2.addFirst(edge.getTarget());
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
		 // build the ThreadRegions objects
		 LinkedList<ThreadRegion> regions = new LinkedList<ThreadRegion>();
		 TIntObjectHashMap<TIntObjectHashMap<ThreadRegion>> map= new TIntObjectHashMap<TIntObjectHashMap<ThreadRegion>>();

		 for (ThreadInstance ti : info) {
			 //    		System.out.println(ti);
			 ThreadRegion r = new ThreadRegion(ti.getId(), ti.getEntry(), ti.getId(), ti.isDynamic());
			 regions.addLast(r);
		 }

		 for (SDGNode n : icfg.vertexSet()) {
			 for (int t : n.getThreadNumbers()) {
				 TIntObjectHashMap<ThreadRegion> mappy = map.get(t);
				 if (mappy == null) {
					 mappy = new TIntObjectHashMap<ThreadRegion>();
					 map.put(t, mappy);
				 }
				 mappy.put(n.getId(), regions.get(t));
			 }
		 }

		 ThreadRegions tr = new ThreadRegions(regions, icfg, map);

		 return tr;
	 }

	 static class RegionBuilder {
		 private CFG icfg;
		 private ThreadsInformation info;
		 private LinkedList<ThreadRegion> regions;
		 private TIntObjectHashMap<TIntObjectHashMap<ThreadRegion>> map;
		 private int id;

		 private RegionBuilder(CFG icfg, ThreadsInformation info) {
			 this.icfg = icfg;
			 this.info = info;
			 regions = new LinkedList<ThreadRegion>();
			 map = new TIntObjectHashMap<TIntObjectHashMap<ThreadRegion>>();
			 id = 0;
		 }

		 public ThreadRegions computeRegions() {
			 for (int thread = 0; thread < info.getNumberOfThreads(); thread++) {
				 computeRegions(thread);
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

			 return new ThreadRegions(regions, icfg, map);
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
			 regions.addAll(computeRegions(startNodes, thread));
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
		 private HashSet<SDGNode> computeStartNodes(int thread) {
			 // initial start nodes
			 HashSet<SDGNode> init = new HashSet<SDGNode>();
			 init.add(info.getThreadEntry(thread));
			 Collection<SDGNode> forks = info.getAllForks();

			 for (SDGNode fork : forks) {
				 if (fork.isInThread(thread)) {
					 init.add(fork);
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
		  * Computes the thread regions of the given thread. For each given start node, a context-sensitive, intra-thread
		  * forward slice in the control-flow graph is performed. All nodes belonging to such a slice form a thread region.
		  * @param startNodes start nodes to compute thread regions from
		  * @param thread thread for which the regions are to be determined
		  * @return all thread regions belonging to the given thread
		  */
		 private LinkedList<ThreadRegion> computeRegions(HashSet<SDGNode> startNodes, int thread) {
			 LinkedList<ThreadRegion> result = new LinkedList<ThreadRegion>();

			 for (SDGNode startNode : startNodes) {
				 LinkedList<SDGNode> w1 = new LinkedList<SDGNode>();
				 LinkedList<SDGNode> w2 = new LinkedList<SDGNode>();
				 HashSet<SDGNode> marked = new HashSet<SDGNode>();

				 w1.add(startNode);
				 marked.add(startNode);

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
							 if (startNodes.contains(reached)) continue;

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
						 if (edge.getKind() != SDGEdge.Kind.FORK
								 && edge.getKind() != SDGEdge.Kind.RETURN) {
							 // don't leave the thread, don't leave procedures
							 SDGNode reached = edge.getTarget();
							 if (!reached.isInThread(thread)) throw new RuntimeException("Error at edge "+edge);

							 // don't cross thread region borders
							 if (startNodes.contains(reached)) continue;

							 if (!marked.contains(reached)) {
								 marked.add(reached);
								 w2.addFirst(reached);
							 }
						 }
					 }
				 }

				 // marked contains the nodes of the thread region
				 ThreadRegion tr = new ThreadRegion(id, startNode, thread, info.isDynamic(thread));
				 tr.setNodes(marked);
				 result.addLast(tr);

				 TIntObjectHashMap<ThreadRegion> mappy = map.get(thread);
				 if (mappy == null) {
					 mappy = new TIntObjectHashMap<ThreadRegion>();
					 map.put(thread, mappy);
				 }

				 for (SDGNode n : marked) {
					 mappy.put(n.getId(), tr);
				 }
				 id++;
			 }

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
