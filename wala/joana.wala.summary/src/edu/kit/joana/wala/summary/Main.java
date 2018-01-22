/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CallGraph;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCallGraph;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;
import edu.kit.joana.wala.summary.EntryPointCache.LoadEntryPointException;
import edu.kit.joana.wala.summary.EntryPointCache.StoreEntryPointException;
import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class Main {

	public static String MAIN_DIR =
//		"/Users/juergengraf/tmp/sdgs/";
		"/ben/grafj/tmp/sdgs/";

	public static String ENTRYPOINT_SUMMARY_FILE_SUFFIX = ".sum";
	public static String SUBGRAPH_PDG_FILE_SUFFIX = ".mojo.pdg";

	private Main() {}

	private static long cumulatedWorkPackageSetUpTime;

	public static void main(String[] args) throws IOException, LoadEntryPointException, StoreEntryPointException, CancelException {
		if (args.length != 1) {
			printUsage();
			return;
		}

		cumulatedWorkPackageSetUpTime = 0;
		final long timeStart = System.currentTimeMillis();

		final String sdgFileName = args[0].trim();
		info("Reading SDG from file \"" + sdgFileName + "\"...");
		SDG sdg = SDG.readFrom(sdgFileName);

		printTotalSumEdges(sdg, sdg.getName());

		final long timeReadIn = System.currentTimeMillis();

		info("Extracting call graph...");
		CallGraph cg = GraphUtil.buildCallGraph(sdg);

		info("Folding strongly connected components...");

		final FoldedCallGraph fcg = GraphFolder.foldCallGraph(cg);
		Set<SDGNode> reachable = ForwardReachablilitySlicer.slice(fcg, fcg.getRoot());
		final int total = reachable.size();
		info("\t" + total + " SCCs found");
		int folded = 0;
		for (SDGNode node : reachable) {
			if (node.getKind() == SDGNode.Kind.FOLDED && fcg.getFoldedNodesOf(node).size() > 1) {
				folded++;
			}
		}
		info("\t" + folded + " non-trivial SCCs");

		// Maps entrynode id -> summary information
		EntryPointCache cache = EntryPointCache.create(MAIN_DIR + sdg.getName() + "-cache" + File.separator);

		info("\n-----------------\n");

		runPerSCCAnalysis(sdg, reachable, cache, fcg, cg, timeStart, timeReadIn);

		info("\n-----------------\n");

//		runPerSCCAnalysis(sdg, reachable, cache, fcg, cg, timeStart, timeReadIn);

//		info("\n-----------------\n");
//
//		runPerSCCAnalysis(sdg, reachable, cache, fcg, cg, timeStart, timeReadIn);
//
//		info("\n-----------------\n");
//
//		runPerSCCAnalysis(sdg, reachable, cache, fcg, cg, timeStart, timeReadIn);
//
//		info("Saving cached summary information per entrypoint...");
//		cache.writeToDisk();
//		info("done.");

//		info("\n-----------------\n");
//
//		{
//			adjustWholeSDGWithSummaries(sdg, cg, cache);
//			final String filename = MAIN_DIR + sdg.getName() + SUBGRAPH_PDG_FILE_SUFFIX;
//			debug("\twriting pdg to file " + filename);
//			FileOutputStream fOs = null;
//			try {
//				fOs = new FileOutputStream(filename);
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}
//			final BufferedOutputStream bOs = new BufferedOutputStream(fOs);
//			SDGSerializer.toPDGFormat(sdg, bOs);
//		}

		{
//			info("Reading SDG from file \"" + sdgFileName + "\"...");
//			SDG sdg2 = SDG.readFrom(sdgFileName);
//			info("Comparing to whole graph algo (please stand by)...");
//			runWholeGraphSummary(sdg2);

//			final String filename = MAIN_DIR + sdg2.getName() + ".summary.pdg";
//			debug("\twriting pdg to file " + filename);
//			FileOutputStream fOs = null;
//			try {
//				fOs = new FileOutputStream(filename);
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}
//			final BufferedOutputStream bOs = new BufferedOutputStream(fOs);
//			SDGSerializer.toPDGFormat(sdg2, bOs);
		}
	}

	@SuppressWarnings("unused")
	private static void runPerSCCAnalysis(SDG sdg, Set<SDGNode> reachable,
			EntryPointCache cache, FoldedCallGraph origFoldedCG, CallGraph cg, final long timeStart,
			final long timeReadIn) throws FileNotFoundException, LoadEntryPointException, StoreEntryPointException, CancelException {
		IProgressMonitor progress = NullProgressMonitor.INSTANCE;
		FoldedCallGraph fcg = GraphFolder.foldCallGraph(cg);
		Set<SDGNode> leafs = GraphUtil.findLeafs(fcg);
		leafs.retainAll(reachable);

		info("Starting with " + leafs.size() + " leafs.");
//		for (SDGNode node : leafs) {
//			info("\t" + node.getLabel());
//		}
//		StrongConnectivityInspector<SDGNode, SDGEdge> scc = new StrongConnectivityInspector<SDGNode, SDGEdge>(cg);
//		List<Set<SDGNode>> sccs = scc.stronglyConnectedSets();
//		info("\t" + sccs.size() + " found.");
		LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
		worklist.addAll(leafs);
		int done = 0;

		final long timePreparations = System.currentTimeMillis();
		long cumulatedPureSummaryTime = 0;
		long cumulatedTotalSumEdges = 0;

		while (!worklist.isEmpty()) {
			SDGNode current = worklist.removeFirst();
			done++;

			// search nodes that are potential candidates for new leafs if the current node is removed.
			Set<SDGNode> recheck = new HashSet<SDGNode>();
			for (SDGEdge edge : fcg.incomingEdgesOf(current)) {
				SDGNode toCheck = fcg.getEdgeSource(edge);
				if (!toCheck.equals(current)) {
					recheck.add(toCheck);
				}
			}

			// create work package
			WorkPackage<SDG> pack = createWorkPackage(sdg, origFoldedCG, cg, current, cache);

			// do the work
			long timeSumStart = System.currentTimeMillis();
//			if (current.getId() == -3) {
//				info("Before comp...");
//				printTotalSumEdges(pack.getGraph(), current.getLabel() + ": " + current.getId() + " # " + pack.getName());
//
//				SDG packSdg = (SDG) pack.getGraph();
//				SDGNode root = packSdg.getRoot();
//				if (root != null) {
//					info("root is " + root.getId() + ":" + root.getLabel());
//				} else {
//					info("root is null");
//				}
//			}
			final int sumEdges = SummaryComputation.compute(pack, progress);
//			if (current.getId() == -3) {
//				info("After comp...");
//				printTotalSumEdges(pack.getGraph(), current.getLabel() + ": " + current.getId() + " # " + pack.getName());
//				info("\n\n");
//			}
			cumulatedTotalSumEdges += sumEdges;
			final long sumTime = System.currentTimeMillis() - timeSumStart;

			//writePackToFile(pack);


			// save results of work
			for (WorkPackage.EntryPoint ep : pack.getEntryPoints()) {
				cache.put(ep);

//				String fileName = MAIN_DIR + ep.getEntryId() + ENTRYPOINT_SUMMARY_FILE_SUFFIX;
//				PrintWriter pw = new PrintWriter(fileName);
//				WorkPackage.EntryPoint.writeOut(pw, ep);
//
//				BufferedInputStream bIn = new BufferedInputStream(new FileInputStream(fileName));
//				try {
//					WorkPackage.EntryPoint ep2 = WorkPackage.EntryPoint.readIn(bIn);
//// write out a second time... to check for consistency
//					String fileName2 = MAIN_DIR + ep.getEntryId() + ENTRYPOINT_SUMMARY_FILE_SUFFIX + ".2";
//					PrintWriter pw2 = new PrintWriter(fileName2);
//					WorkPackage.EntryPoint.writeOut(pw2, ep2);
//				} catch (ParseException e) {
//					e.printStackTrace();
//				}
			}

			// see if some work packages depended on the current one and add them to the worklist, if all dependencies
			// are resolved.
			GraphUtil.removeLeaf(fcg, current);

			int newLeafs = 0;
			for (SDGNode check : recheck) {
				if (GraphUtil.isLeaf(fcg, check) && reachable.contains(check)) {
					newLeafs++;

					worklist.addLast(check);
				}
			}

			// print info
//			info(done + "/" + total + ": working " + current.getKind() + "|" + current.getId() + " " + current.getLabel() + ": " + newLeafs + " new leafs - "
//					+ worklist.size() + " worklist.");
//			info("\t->" + pack.toString());
			debug("\t" + sumEdges + " summary edges computed in " + sumTime + "ms");
			cumulatedPureSummaryTime += sumTime;
		}

		final long timeTotal = System.currentTimeMillis();

		info("Merging summaries into sdg.");

		// add computed summaries to sdg.
		for (SDGNode node : sdg.vertexSet()) {
			if (node.getKind() == SDGNode.Kind.ENTRY) {
				Collection<SDGNode> callers = sdg.getCallers(node);

				if (callers.size() > 0) {
					EntryPoint ep = cache.getEntryPoint(node.getId());

					debug("\tadjusting " + callers.size() + " calls to " + node.getLabel() + " " + ep);


					for (SDGNode callNode : sdg.getCallers(node)) {
						if (callNode.getProc() == 0) {
							// skip propagation of summaries to root
							continue;
						}

						adjustCallsite(sdg, callNode, node, ep, sdg);
					}
				}
			}
		}

		final long timeMerging = System.currentTimeMillis() - timeTotal;

		info("SDG read in: \t\t" + (timeReadIn - timeStart) + "ms");
		info("Datastructure set up: \t" + (timePreparations - timeReadIn) + "ms");
		info("Summary computation: \t" + (timeTotal - timePreparations)
				+ "ms (pure computation time: " + cumulatedPureSummaryTime + "ms)"
				+ "(package setup time: " + cumulatedWorkPackageSetUpTime + "ms)");
		info("Total time: \t\t" + (timeTotal - timeStart) + "ms");
		info("Time for merging summary edges: " + timeMerging + "ms");
		printTotalSumEdges(sdg, sdg.getName());
	}

	private static void printTotalSumEdges(DirectedGraph<SDGNode, SDGEdge> graph, String title) {
		long countSum = 0;
		for (SDGEdge edge : graph.edgeSet()) {
			if (edge.getKind() == SDGEdge.Kind.SUMMARY) {
				countSum++;
			}
		}

		info("SDG(" + title + ") Total number of summary edges: " + countSum);
	}

	@SuppressWarnings("unused")
	private static void writePackToFile(WorkPackage<SDG> pack) {
		final String filename = MAIN_DIR + pack.getName() + SUBGRAPH_PDG_FILE_SUFFIX;
		debug("\twriting pdg to file " + filename);
		FileOutputStream fOs = null;
		try {
			fOs = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		final BufferedOutputStream bOs = new BufferedOutputStream(fOs);
		SDGSerializer.toPDGFormat((SDG) pack.getGraph(), bOs);
	}

	@SuppressWarnings("unused")
	private static void runWholeGraphSummary(SDG sdg) throws CancelException {
		info("Running summary computation on whole graph...");

		final IProgressMonitor progress = NullProgressMonitor.INSTANCE;
		final long timeStart = System.currentTimeMillis();

		cumulatedWorkPackageSetUpTime = 0;
		long cumulatedPureSummaryTime = 0;

		final WorkPackage<SDG> pack = createWholeGraphPackage(sdg);

		// do the work
		long timeSumStart = System.currentTimeMillis();
		final int sumEdges = SummaryComputation.compute(pack, progress);
		final long sumTime = System.currentTimeMillis() - timeSumStart;

		debug("\t" + sumEdges + " summary edges computed in " + sumTime + "ms");
		cumulatedPureSummaryTime += sumTime;

		final long timeTotal = System.currentTimeMillis();

		info("Summary computation: \t" + (timeTotal - timeSumStart)
				+ "ms (pure computation time: " + cumulatedPureSummaryTime + "ms)"
				+ "(package setup time: " + cumulatedWorkPackageSetUpTime + "ms)");
		info("Total time: \t\t" + (timeTotal - timeStart) + "ms");
		printTotalSumEdges(sdg, sdg.getName());
	}

	private static WorkPackage<SDG> createWholeGraphPackage(SDG sdg) {
		final SDGNode root = sdg.getRoot();

		if (root.getKind() != SDGNode.Kind.ENTRY) {
			throw new IllegalStateException("Root node has to be entry node");
		}

		WorkPackage.EntryPoint ep = GraphUtil.extractEntryPoint(sdg, root);
		Set<WorkPackage.EntryPoint> entryPoints = new HashSet<WorkPackage.EntryPoint>();
		entryPoints.add(ep);
		WorkPackage<SDG> pack = WorkPackage.create(sdg, entryPoints, sdg.getName());

		return pack;
	}

	private static WorkPackage<SDG> createWorkPackage(SDG sdg, FoldedCallGraph fcg, CallGraph cg, SDGNode current, EntryPointCache cache) throws LoadEntryPointException {
		// 1. Create EntryPoints
		// 2. Create stripped subgraphs with summary edges of previous computations included
		// 3. Augment subgraph with summary edges of previously computed work packages.

		WorkPackage<SDG> pack;

		final long timeStart = System.currentTimeMillis();

		debug("create workpackage for " + current.getKind() + "|" + current.getId() + " " + current.getLabel());

		SDG stripped;

		if (current.getKind() != SDGNode.Kind.FOLDED) {
			assert sdg.containsVertex(current) : "Not part of sdg: " + current.getKind() + "|" + current.getId() + " " + current.getLabel();

			Set<WorkPackage.EntryPoint> entryPoints = new HashSet<WorkPackage.EntryPoint>();
			WorkPackage.EntryPoint ep = GraphUtil.extractEntryPoint(sdg, current);
//			info("\t" + current.getKind() + "|" + current.getId() + " " + current.getLabel() + ": " + ep);
			entryPoints.add(ep);

			stripped = GraphUtil.stripGraph(sdg, current);
			debug("\t->subgraph contains " + stripped.vertexSet().size() + " nodes and " + stripped.edgeSet().size() + " edges.");
			final String name = current.getLabel() + "@" + current.getId();
			pack = WorkPackage.create(stripped, entryPoints, name);
		} else {
			assert current.getKind() == SDGNode.Kind.ENTRY
				: "Entry Node expected. Found: " + current.getKind() + "|" + current.getId() + " " + current.getLabel();
//			info("\tworking on folded node " + current.getKind() + "|" + current.getId() + " " + current.getLabel());

			List<SDGNode> entries = fcg.getFoldedNodesOf(current);
			Set<WorkPackage.EntryPoint> entryPoints = new HashSet<WorkPackage.EntryPoint>();

			String name = "";
			for (SDGNode entry : entries) {
				// this may include nodes that are never called outside the scc
				WorkPackage.EntryPoint ep = GraphUtil.extractEntryPoint(sdg, entry);
//				info("\t\t" + entry.getKind() + "|" + entry.getId() + " " + entry.getLabel() + ": " + ep);
				entryPoints.add(ep);
				name += entry.getLabel() + "@" + entry.getId() + "-";
			}
			name = name.substring(0, name.length() - 1);

			stripped = GraphUtil.stripGraph(sdg, entries);
			debug("\t->subgraph contains " + stripped.vertexSet().size() + " nodes and " + stripped.edgeSet().size() + " edges.");
			pack = WorkPackage.create(stripped, entryPoints, name);
		}

		stripped.setName(pack.getName());

		int newSumEdges = adjustSubgraphWithSummaries(cg, fcg, cache, current, stripped);
		debug("\t->added " + newSumEdges + " dependencies through adjustments.");
//		if (current.getId() == -3) {
//			info(newSumEdges + " added in adjustment.");
//			for (SDGEdge edge : fcg.outgoingEdgesOf(current)) {
//				if (edge.getKind() == SDGEdge.Kind.CALL && !fcg.getFoldedNodesOf(current).contains(edge.getTarget())) {
//					EntryPoint ep = cache.getEntryPoint(edge.getTarget().getId());
//					info("\toutside target: " + edge.getTarget().getLabel() + " => " + ep);
//				}
//			}
//		}

		final long timeEnd = System.currentTimeMillis();

		debug("\tpackage set up took " + (timeEnd - timeStart) + "ms");

		cumulatedWorkPackageSetUpTime += (timeEnd - timeStart);

		return pack;
	}

	private static int adjustSubgraphWithSummaries(CallGraph cg, FoldedCallGraph fcg, EntryPointCache cache,
			SDGNode current, SDG toAdjust) {
		List<SDGNode> callers;
		if (current.getKind() == SDGNode.Kind.FOLDED) {
			callers = fcg.getFoldedNodesOf(current);
		} else {
			callers = new LinkedList<SDGNode>();
			callers.add(current);
		}

		int newSumEdges = 0;

		for (SDGNode entry : toAdjust.vertexSet()) {
			if (entry.getKind() != SDGNode.Kind.ENTRY) {
				continue;
			}

			if (!callers.contains(entry)) {
				EntryPoint ep = null;
				try {
					ep = cache.getEntryPoint(entry.getId());
				} catch (LoadEntryPointException e) {
					e.printStackTrace();
				}

				debug("\t\tadjusting all callsites of " + entry.getId() + "|" + entry.getLabel() + " with " + ep);

				if (ep == null) {
					error("No entrypoint information for " + entry.getId() + "|" + entry.getLabel());
					throw new IllegalStateException();
				}

				newSumEdges += adjustAllCallsites(entry, ep, toAdjust);
			}
		}

//		for (SDGNode caller : callers) {
//			for (SDGEdge callEdge : cg.outgoingEdgesOf(caller)) {
//				SDGNode callee = cg.getEdgeTarget(callEdge);
//				if (!callers.contains(callee)) {
//
//					for (SDGNode callNode : sdg.getCallers(callee)) {
//						if (!toAdjust.containsVertex(callNode)) {
//							continue;
//						}
//
//						EntryPoint ep = null;
//						try {
//							ep = cache.getEntryPoint(callee.getId());
//						} catch (LoadEntryPointException e) {
//							e.printStackTrace();
//						}
//
//						debug("\t\tadjusting callsite of " + callNode.getId() + "|" + callNode.getLabel() + " with " + ep);
//
//						if (ep == null) {
//							error("No entrypoint information for " + callee.getId() + "|" + callee.getLabel());
//							throw new IllegalStateException();
//						}
//
//						newSumEdges += adjustCallsite(sdg, callNode, callee, ep, toAdjust);
//					}
//				}
//			}
//		}

		return newSumEdges;
	}

	@SuppressWarnings("unused")
	private static void adjustWholeSDGWithSummaries(SDG sdg, CallGraph cg, EntryPointCache cache) {
		for (SDGNode caller : cg.vertexSet()) {
			assert caller.getKind() == SDGNode.Kind.ENTRY;

			if (caller == sdg.getRoot()) {
				continue;
			}

			for (SDGEdge edge : cg.outgoingEdgesOf(caller)) {
				final SDGNode callee = edge.getTarget();
				assert callee.getKind() == SDGNode.Kind.ENTRY;

				for (SDGNode callNode : sdg.getCallers(callee)) {
					assert callee.getKind() == SDGNode.Kind.CALL;

					EntryPoint ep = null;
					try {
						ep = cache.getEntryPoint(callee.getId());
					} catch (LoadEntryPointException e) {
						e.printStackTrace();
					}

					debug("\t\tadjusting callsite of " + callNode.getId() + "|" + callNode.getLabel() + " with " + ep);

					if (ep == null) {
						error("No entrypoint information for " + callee.getId() + "|" + callee.getLabel());
						throw new IllegalStateException();
					}

					adjustCallsite(sdg, callNode, callee, ep, sdg);
				}
			}
		}
	}

	private static int adjustAllCallsites(SDGNode calleeEntry, EntryPoint ep, SDG toAdjust) {
		assert calleeEntry.getKind() == SDGNode.Kind.ENTRY;
		assert calleeEntry.getId() == ep.getEntryId();

		int newEdges = 0;

		for (TIntIterator itFin = ep.iterateFormalIns(); itFin.hasNext();) {
			final int fInId = itFin.next();
			final SDGNode formalIn = toAdjust.getNode(fInId);

			assert (formalIn.getKind() == SDGNode.Kind.FORMAL_IN);

			TIntList influenced = ep.getInfluencedFormOuts(fInId);
			if (influenced == null) {
				continue;
			}

			for (TIntIterator itFout = influenced.iterator(); itFout.hasNext();) {
				final int fOutId = itFout.next();
				final SDGNode formalOut = toAdjust.getNode(fOutId);

				assert (formalOut.getKind() == SDGNode.Kind.FORMAL_OUT);

				// will lead to direct summary edges at all callsites.
				SDGEdge ddEdge = new SDGEdge(formalIn, formalOut, SDGEdge.Kind.DATA_DEP);
				toAdjust.addEdge(ddEdge);
				newEdges++;
			}
		}

		return newEdges;
	}

	private static int adjustCallsite(SDG sdg, SDGNode callNode, SDGNode callee, EntryPoint ep, SDG toAdjust) {
		assert callNode.getKind() == SDGNode.Kind.CALL;
		assert callee.getKind() == SDGNode.Kind.ENTRY;

		int newEdges = 0;

		for (TIntIterator itFin = ep.iterateFormalIns(); itFin.hasNext();) {
			final int fInId = itFin.next();
			final SDGNode formalIn = sdg.getNode(fInId);

			assert (formalIn.getKind() == SDGNode.Kind.FORMAL_IN);

			final SDGNode actualIn = sdg.getActualIn(callNode, formalIn);

			if (actualIn == null) {
				debug("WARN: No matching actIn found. For node " + formalIn.getId() + " of call " + callNode.getLabel() + " - SKIPPING");
				continue;
//				throw new IllegalStateException("No matching actIn found. For node " + formalIn.getId());
			}

			TIntList influenced = ep.getInfluencedFormOuts(fInId);
			if (influenced == null) {
				continue;
			}

			for (TIntIterator itFout = influenced.iterator(); itFout.hasNext();) {
				final int fOutId = itFout.next();
				final SDGNode formalOut = sdg.getNode(fOutId);

				assert (formalOut.getKind() == SDGNode.Kind.FORMAL_OUT);

				final SDGNode actualOut = sdg.getActualOut(callNode, formalOut);

				if (actualOut == null) {
					debug("WARN: No matching actualOut found.");
					continue;
//					throw new IllegalStateException("No matching actualOut found.");
				}

				SDGEdge sumEdge = new SDGEdge(actualIn, actualOut, SDGEdge.Kind.SUMMARY);
				toAdjust.addEdge(actualIn, actualOut, sumEdge);
				newEdges++;
			}
		}

		return newEdges;
	}

	private static void info(String str) {
		System.out.println(str);
	}

	private static void debug(String str) {
		System.out.println(str);
	}

	private static void error(String str) {
		System.err.println(str);
	}

	private static void printUsage() {
		System.out.println(
			  "Parallel Summary Edge Computation - Juergen Graf <graf@kit.edu>\n\n"
			+ "Usage:\n"
			+ "\tjava -jar sdg-summary.jar <sdg>\n\n"
			+ "\t<sdg>\tFile containing SDG without summary edges.\n\n"
			+ "Output:\n"
			+ "\t<sdg>.sum\t SDG with summary edges.\n\n"
			+ "Example:\n"
			+ "\tjava -jar sdg-summary.jar MainEmulator.pdg"
		);
	}
}
