/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary;

import edu.kit.joana.ifc.sdg.graph.BitVector;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.CallGraphBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.CallGraphSlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CallGraph;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.CallGraphBuilder;
import edu.kit.joana.wala.summary.GraphUtil.SummaryProperties;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;


import org.jgrapht.DirectedGraph;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

public class MainChangeTest {

	private final static int NUM_ADD_EDGES = 5;
	private final static int MIN_SUM_EDGES = 1000;

	private static long cumulatedWorkPackageSetUpTime;

	private static long cumulatedOriginal = 0;
	private static long cumulatedWholeGraph = 0;
	private static long cumulatedReachable = 0;
	private static long cumulatedFully = 0;
	private static long cumulatedRemember = 0;
	private static int numSDG = 0;
	private static int numTries = 0;

	private static final int RELOAD_SDG = 20;
	private static final int TRIES_PER_SDG = 10;

	private static TLongList originalTimes = new TLongArrayList();
	private static ArrayList<String> originalName = new ArrayList<String>();
	private static TLongList wholeTimes = new TLongArrayList();
	private static TLongList reachTimes = new TLongArrayList();
	private static TLongList fullyTimes = new TLongArrayList();
	private static TLongList rememberTimes = new TLongArrayList();

	private static boolean trimGraph = false;

	public static void main(String[] args) throws IOException, CancelException {
		if (args.length < 1) {
			printUsage();
			return;
		}

		for (String arg : args) {
			if (arg.trim().equals("-trim")) {
				trimGraph = true;
				System.out.println("Graphs are stripped from unused edges.");
				continue;
			}

			File f = new File(arg);
			if (!f.exists() || !f.canRead()) {
				error(arg + " is not a readable and existsing file.");
				continue;
			}

			if (f.isDirectory()) {
				// search for *.pdg
				String[] files = f.list(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.endsWith(".pdg") && (new File(dir + File.separator + name)).isFile();
					}
				});

				for (String file : files) {
					run(arg + (arg.isEmpty() || arg.endsWith(File.separator) ? "" : File.separator) + file);
				}
			} else if (f.isFile()) {
				run(arg);
			}
		}

		info(originalTimes.size() + " - " + wholeTimes.size() + " - " + reachTimes.size()
				+ " - " + fullyTimes.size() + " - " + rememberTimes.size());

		TLongList avgRemember = new TLongArrayList();
		TLongList avgFully = new TLongArrayList();
		TLongList avgReach = new TLongArrayList();
		TLongList avgOriginal = new TLongArrayList();
		TLongList avgWhole = new TLongArrayList();
		List<String> avgName = new ArrayList<String>();

		for (int curSDG = 0; curSDG < numSDG; curSDG++) {
			long sumReach = 0;
			long sumFully = 0;
			long sumWhole = 0;
			long sumOrig = 0;
			long sumRem = 0;
			String sdgName = null;

			for (int curLoad = 0; curLoad < RELOAD_SDG; curLoad++) {
				final int origIndex = (curSDG + 1) * curLoad;

				sdgName = originalName.get(origIndex);

				for (int curTrie = 0; curTrie < TRIES_PER_SDG; curTrie++) {
					final int trieIndex = (origIndex + 1) * curTrie;

					final long origTime = originalTimes.get(trieIndex);
					final long whole = wholeTimes.get(trieIndex);
					final long reach = reachTimes.get(trieIndex);
					final long fully = fullyTimes.get(trieIndex);
					final long remember = rememberTimes.get(trieIndex);
					sumOrig += origTime;
					sumWhole += whole;
					sumReach += reach;
					sumFully += fully;
					sumRem += remember;
				}

			}

			long avgFully1 = sumFully / (RELOAD_SDG * TRIES_PER_SDG);
			long avgRem1 = sumRem / (RELOAD_SDG * TRIES_PER_SDG);
			long avgReach1 = sumReach / (RELOAD_SDG * TRIES_PER_SDG);
			long avgWhole1 = sumWhole / (RELOAD_SDG * TRIES_PER_SDG);
			long avgOrig1 = sumOrig / (RELOAD_SDG * TRIES_PER_SDG);

			avgFully.add(avgFully1);
			avgRemember.add(avgRem1);
			avgReach.add(avgReach1);
			avgOriginal.add(avgOrig1);
			avgWhole.add(avgWhole1);
			avgName.add(sdgName);
		}

		info("---- SNIP CSV ----");
		info("Program,Raw summary computation,Standard with precomputed,With reachablility analysis,Fully connect opt,Remember visited");
		for (int i = 0; i < avgOriginal.size(); i++) {
			String name = avgName.get(i);
			long original = avgOriginal.get(i);
			long whole = avgWhole.get(i);
			long reach = avgReach.get(i);
			long fully = avgFully.get(i);
			long remember = avgRemember.get(i);
			info(name + "," + original + "," + whole + "," + reach + "," + fully + "," + remember);
		}
		info("==== SNAP CSV ====");

		info(numTries + " tests on " + numSDG + " graphs.");
		info(cumulatedOriginal + "ms - cumulated raw whole graph summaries.");
		info(cumulatedWholeGraph + "ms - cumulated whole graph summaries with precomputed info.");
		info(cumulatedReachable + "ms - cumulated reachable parts summaries.");
		info(cumulatedFully + "ms - cumulated reachable parts + fully connected opt summaries.");
		info(cumulatedRemember + "ms - cumulated reachable parts + fully connected opt summaries + remember reached.");
	}

	private static int removeSummaryEdges(SDG sdg) {
		List<SDGEdge> toRemove = new LinkedList<SDGEdge>();
		// remove all summary edges
		for (SDGEdge edge : sdg.edgeSet()) {
			if (edge.getKind() == SDGEdge.Kind.SUMMARY) {
				toRemove.add(edge);
			}
		}
		sdg.removeAllEdges(toRemove);

		return toRemove.size();
	}

	private static void run(final String sdgFileName) throws IOException, CancelException {
		cumulatedWorkPackageSetUpTime = 0;
		final long timeStart = System.currentTimeMillis();


		info("Reading SDG from file \"" + sdgFileName + "\"...");
		SDG origSDG = SDG.readFrom(sdgFileName);

		final long timeReadIn = System.currentTimeMillis();

		debug("SDG read in: \t\t" + (timeReadIn - timeStart) + "ms");

		final int numSumEdges = removeSummaryEdges(origSDG);

		if (numSumEdges < MIN_SUM_EDGES) {
			info("less then " + MIN_SUM_EDGES + " summary edges - skipping this sdg...");
			return;
		}

		numSDG++;

//		printTotalSumEdges(origSDG, origSDG.getName());

		if (trimGraph) {
			stripUnneccessaryEdges(origSDG);
		}

//		SDG strippedSDG = origSDG.clone();
//		stripUnneccessaryEdges(strippedSDG);

		System.out.print("Computing " + RELOAD_SDG  + " runs ");

		for (int tries = 0; tries < RELOAD_SDG; tries++) {
			System.out.print('.');

			SDG sdg = origSDG.clone();

			runWholeGraphSummary(sdg, null, null, null, false, false, false);

			for (int i = 0; i < TRIES_PER_SDG; i++) {
				debug("Adding 5 edges to SDG...");
				numTries++;

				final SummaryProperties p = GraphUtil.createSummaryProperties(sdg);
				debug("Found " + p.fullyConnectedActOuts + " fully connected actOuts of total " + p.totalNumActOuts + " nodes.");

				Set<SDGNode> changed = addSomeEdges(sdg);
	//			for (SDGNode reach : changed) {
	//				System.out.println("CHANGED:" + reach.getId() + "|" + reach.getKind() + ":" + reach.getLabel());
	//			}

				addCallGraphPreds(changed, sdg);
	//			for (SDGNode reach : changed) {
	//				System.out.println("INFLUENCE: " + reach.getId() + "|" + reach.getKind() + ":" + reach.getLabel());
	//			}
	//
				SDG tmpSDG = sdg.clone();
				removeSummaryEdges(tmpSDG);
				debug("Running on whole graph without precomputed information...");
				runWholeGraphSummary(tmpSDG, null, null, null, false, true, true);
				tmpSDG = sdg.clone();
				debug("Running on whole graph WITH precomputed summary edges...");
				runWholeGraphSummary(tmpSDG, null, null, null, false, false, true);
				debug("Running only on changed procs...");
				tmpSDG = sdg.clone();
				runWholeGraphSummary(tmpSDG, changed, null, null, false, false, true);
				debug("Running only on changed procs + shortcut fully connected params...");
				tmpSDG = sdg.clone();
				runWholeGraphSummary(tmpSDG, changed, p.fullyConnectedIds, p.out2in, false, false, true);
				debug("Running only on changed procs + shortcut fully connected params + remember visited params...");
				tmpSDG = sdg.clone();
				runWholeGraphSummary(tmpSDG, changed, p.fullyConnectedIds, p.out2in, true, false, true);
			}
		}

		System.out.println("done.");
	}

	private static void stripUnneccessaryEdges(SDG sdg) {
		List<SDGEdge> toRemove = new LinkedList<SDGEdge>();

		for (SDGEdge edge : sdg.edgeSet()) {
			SDGEdge.Kind kind = edge.getKind();
			if (!kind.isIntraSDGEdge() && kind != SDGEdge.Kind.SUMMARY
					&& kind != SDGEdge.Kind.CALL
					&& kind != SDGEdge.Kind.PARAMETER_IN
					&& kind != SDGEdge.Kind.PARAMETER_OUT) {
				toRemove.add(edge);
			}
		}

		sdg.removeAllEdges(toRemove);
		sdg.setName(sdg.getName() + "[stripped]");
	}



	private static void addCallGraphPreds(Set<SDGNode> entries, SDG sdg) {
		CallGraph cg = CallGraphBuilder.buildCallGraph(sdg);
		CallGraphSlicer back = new CallGraphBackward(cg);

		Set<SDGNode> callsTo = new HashSet<SDGNode>();
		for (SDGNode entry : entries) {
			callsTo.addAll(sdg.getCallers(entry));
		}

		Collection<SDGNode> reachable = back.slice(callsTo);
		Set<SDGNode> reachEntries = new HashSet<SDGNode>();

		for (SDGNode call : reachable) {
			reachEntries.addAll(sdg.getPossibleTargets(call));
			//TODO this is conservative. we may add methods that where not reachable
		}

		for (SDGNode reach : reachEntries) {
//			System.out.println(reach.getId() + "|" + reach.getKind() + ":" + reach.getLabel());
			entries.add(reach);
		}
	}

	private final static Random rand = new Random();

	/*
	 * Returns a set of  entrynodes of the procedures that have been changed.
	 */
	private static Set<SDGNode> addSomeEdges(SDG sdg) {
		ArrayList<SDGNode> entries = new ArrayList<SDGNode>();
		for (SDGNode node : sdg.vertexSet()) {
			if (node.getKind() == SDGNode.Kind.ENTRY) {
				entries.add(node);
			}
		}

		Set<SDGNode> changed = new HashSet<SDGNode>();

		changed.add(addEdgesToSingleProc(NUM_ADD_EDGES, entries, sdg));
//		changed.add(addEdgesToSingleProc(5, entries, sdg));
//		changed.add(addEdgesToSingleProc(5, entries, sdg));
//		changed.add(addEdgesToSingleProc(5, entries, sdg));

		return changed;
	}

	/*
	 * returns the entrynode of the procedure that has been changed.
	 */
	private static SDGNode addEdgesToSingleProc(final int numOfEdges, ArrayList<SDGNode> entries, SDG sdg) {
		SDGNode entry = null;

		LinkedList<SDGEdge> candidates = null;
		while (candidates == null || candidates.size() < numOfEdges) {
			final int proc = rand.nextInt(entries.size());
			entry = entries.get(proc);
			candidates = findCandidates(entry, sdg);
		}

		for (int i = 0; i < numOfEdges; i++) {
			final int edgeIndex = rand.nextInt(candidates.size());
			SDGEdge edge = candidates.remove(edgeIndex);
			sdg.addEdge(edge);
		}

		return entry;
	}

	private static LinkedList<SDGEdge> findCandidates(SDGNode entry, SDG sdg) {
		final List<SDGNode> procNodes = sdg.getNodesOfProcedure(entry);

		final LinkedList<SDGEdge> candidates = new LinkedList<SDGEdge>();

		for (SDGNode from : procNodes) {
			for (SDGNode to : procNodes) {
				if (from != to && nodesNotConnected(from, to, sdg)) {
					final SDGEdge cand = new SDGEdge(from, to, SDGEdge.Kind.DATA_DEP);
					candidates.add(cand);
				}
			}
		}

		return candidates;
	}

	private static boolean nodesNotConnected(SDGNode from, SDGNode to, SDG sdg) {
		for (SDGEdge edge : sdg.outgoingEdgesOf(from)) {
			if (edge.getTarget() == to && edge.getKind().isIntraSDGEdge()) {
				return false;
			}
		}

		return true;
	}

	private static void runWholeGraphSummary(SDG sdg, Set<SDGNode> changed, TIntSet fully,
			TIntObjectMap<List<SDGNode>> out2in, boolean rememberReached, boolean raw, boolean count) throws CancelException {
		debug("Running summary computation...");

		final IProgressMonitor progress = NullProgressMonitor.INSTANCE;
		final long timeStart = System.currentTimeMillis();

		cumulatedWorkPackageSetUpTime = 0;
		long cumulatedPureSummaryTime = 0;

		final WorkPackage pack = createWholeGraphPackage(sdg, changed, fully, out2in ,rememberReached);

		// do the work
		long timeSumStart = System.currentTimeMillis();
		final int sumEdges = SummaryComputation.compute(pack, progress);
		final long sumTime = System.currentTimeMillis() - timeSumStart;

		debug("\t" + sumEdges + " summary edges computed in " + sumTime + "ms");
		cumulatedPureSummaryTime += sumTime;

		final long timeTotal = System.currentTimeMillis();

		debug("Summary computation: \t" + (timeTotal - timeSumStart)
				+ "ms (pure computation time: " + cumulatedPureSummaryTime + "ms)"
				+ "(package setup time: " + cumulatedWorkPackageSetUpTime + "ms)");
		debug("Total time: \t\t" + (timeTotal - timeStart) + "ms");

		if (count) {
			if (raw) {
				originalTimes.add(cumulatedPureSummaryTime);
				cumulatedOriginal += cumulatedPureSummaryTime;
			} else if (changed == null) {
				wholeTimes.add(cumulatedPureSummaryTime);
				cumulatedWholeGraph += cumulatedPureSummaryTime;
			} else if (fully == null) {
				reachTimes.add(cumulatedPureSummaryTime);
				cumulatedReachable += cumulatedPureSummaryTime;
			} else if (!rememberReached) {
				fullyTimes.add(cumulatedPureSummaryTime);
				cumulatedFully += cumulatedPureSummaryTime;
			} else {
				rememberTimes.add(cumulatedPureSummaryTime);
				cumulatedRemember += cumulatedPureSummaryTime;
			}
		} else {
			originalName.add(sdg.getName());
		}

		printTotalSumEdges(sdg, sdg.getName());
	}

	private static WorkPackage createWholeGraphPackage(SDG sdg, final Set<SDGNode> changed, final TIntSet fully,
			TIntObjectMap<List<SDGNode>> out2in, boolean rememberReached) {
		final SDGNode root = sdg.getRoot();

		if (root.getKind() != SDGNode.Kind.ENTRY) {
			throw new IllegalStateException("Root node has to be entry node");
		}

		TIntSet relevant = null;
		if (changed != null) {
			relevant = new TIntHashSet();
			for (SDGNode node : changed) {
				relevant.add(node.getProc());
			}
		}

		if (rememberReached) {
			// prepare sdg
	    	int numRelevantNodes = 1;
	        for (SDGNode n : (Set<SDGNode>) sdg.vertexSet()) {
	            if (n.getKind() == SDGNode.Kind.FORMAL_OUT || n.getKind() == SDGNode.Kind.EXIT) {
	            	n.tmp = 0;

	            	if (relevant != null && !relevant.contains(n.getProc())) {
	            		continue;
	            	}

	            	if (fully.contains(n.getId())) {
	            		continue;
	            	}

	            	n.tmp = numRelevantNodes;
	            	numRelevantNodes++;
	            }
	        }

	        for (SDGNode n : sdg.vertexSet()) {
	        	if (n.kind == SDGNode.Kind.ACTUAL_IN
	        			&& (relevant == null || relevant.contains(n.getProc()))) {
	        		if (n.bv != null) {
	        			n.bv.clearAll();
	        		} else {
	        			n.bv = new BitVector(numRelevantNodes + 1);
	        		}
	        	}
	        }
		}

		WorkPackage.EntryPoint ep = GraphUtil.extractEntryPoint(sdg, root);
		Set<WorkPackage.EntryPoint> entryPoints = new HashSet<WorkPackage.EntryPoint>();
		entryPoints.add(ep);
		WorkPackage pack = WorkPackage.create(sdg, entryPoints, sdg.getName(), relevant, fully, out2in, rememberReached);

		return pack;
	}

	private static void printTotalSumEdges(DirectedGraph<SDGNode, SDGEdge> graph, String title) {
		long countSum = 0;
		for (SDGEdge edge : graph.edgeSet()) {
			if (edge.getKind() == SDGEdge.Kind.SUMMARY) {
				countSum++;
			}
		}

		debug("SDG(" + title + ") Total number of summary edges: " + countSum);
	}

	private static void info(String str) {
		System.out.println(str);
	}

	private static void debug(String str) {
//		System.out.println(str);
	}

	private static void error(String str) {
		System.err.println(str);
	}

	private static void printUsage() {
		System.out.println(
			  "Incremental Summary Edge Computation Test - Juergen Graf <graf@kit.edu>\n\n"
			+ "Usage:\n"
			+ "\tjava -jar sdg-summary.jar <sdg>\n\n"
			+ "\t<sdg>\tFile containing SDG without summary edges. Or a directory containing SDG files (*.pdg).\n\n"
			+ "Output:\n"
			+ "\tTextual output of evaluation result.\n\n"
			+ "Example:\n"
			+ "\tjava -jar sdg-summary.jar MainEmulator.pdg\n"
			+ "\tjava -Xmx1024M -jar sdg-summary.jar MainEmulator.pdg\n"
		);
	}
}
