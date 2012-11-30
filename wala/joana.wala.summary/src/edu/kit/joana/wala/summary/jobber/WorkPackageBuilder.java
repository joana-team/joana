/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary.jobber;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CallGraph;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCallGraph;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;
import edu.kit.joana.wala.summary.ForwardReachablilitySlicer;
import edu.kit.joana.wala.summary.GraphUtil;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Builds seperate workpackages from a single sdg.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class WorkPackageBuilder extends Thread {

	public static final int MAX_FILENAME_LENGTH = 127;
	public static final String SUBGRAPH_FILE_SUFFIX = ".pdg";

	private final SDG sdg;
	private final CallGraph cg;
	private final String cacheDir;

	private final List<JobMessage> ready = new LinkedList<JobMessage>();

	public WorkPackageBuilder(SDG sdg, CallGraph cg, String cacheDir) {
		super();
		this.sdg = sdg;
		this.cg = cg;
		this.cacheDir = cacheDir;
	}

	@Override
	public void run() {
		final FoldedCallGraph fcg = GraphFolder.foldCallGraph(cg);
		Set<SDGNode> reachable = ForwardReachablilitySlicer.slice(fcg, fcg.getRoot());

		Set<SDGNode> leafs = GraphUtil.findLeafs(fcg);
		LinkedList<SDGNode> localWorklist = new LinkedList<SDGNode>(leafs);

		info("started...");

		while (!localWorklist.isEmpty()) {
			SDGNode current = localWorklist.removeFirst();

			JobMessage job = createJob(sdg, fcg, cg, current);

			synchronized (ready) {
				ready.add(job);
			}

			addNewLeafNodesToList(fcg, reachable, job, localWorklist);
		}

		info("done.");
	}

	public List<JobMessage> getReadyJobs(Set<SDGNode> dependencyResolved) {
		List<JobMessage> newJobs = new LinkedList<JobMessage>();

		synchronized (ready) {
			for (JobMessage job : ready) {
				if (dependencyResolved.contains(job.getNode())) {
					newJobs.add(job);
				}
			}

			ready.removeAll(newJobs);
		}

		return newJobs;
	}

	private JobMessage createJob(SDG sdg, FoldedCallGraph fcg, CallGraph cg, SDGNode current) {
		info("Creating job for " + current.getId() + "|" + current.getLabel());

		SDG subgraph = createSubgraph(sdg, fcg, current);
		final String filename = makeFilename(subgraph);
		try {
			writeToFile(cacheDir + filename, subgraph);
		} catch (IOException e) {
			throw new IllegalStateException("Could not save subgraph '" + filename + "' in " + cacheDir, e);
		}

		JobMessage msg = new JobMessage(filename);

		TIntSet entries = findEntryIds(fcg, cg, current);
		TIntIterator it = entries.iterator();
		while (it.hasNext()) {
			int entryId = it.next();
			msg.addEntry(entryId);
		}

		TIntSet sccExits = findExitIds(fcg, cg, current);
		it = sccExits.iterator();
		while (it.hasNext()) {
			int exitId = it.next();
			msg.addExitPoint(exitId);
		}

		msg.setNode(current);
		msg.setSize(subgraph.vertexSet().size());

		info("Job ready: " + msg);

		return msg;
	}

	private static TIntSet findExitIds(FoldedCallGraph fcg, CallGraph cg, SDGNode current) {
		TIntSet exitPointIds = new TIntHashSet();

		if (current.getKind() == SDGNode.Kind.FOLDED) {
			List<SDGNode> foldedNodes = fcg.getFoldedNodesOf(current);
			for (SDGNode node : foldedNodes) {
				for (SDGEdge call : cg.outgoingEdgesOf(node)) {
					if (call.getKind() == SDGEdge.Kind.CALL && !foldedNodes.contains(call.getTarget())) {
						// outside call
						exitPointIds.add(call.getTarget().getId());
					}
				}
			}
		} else {
			for (SDGEdge call : cg.outgoingEdgesOf(current)) {
				if (call.getKind() == SDGEdge.Kind.CALL && !call.getTarget().equals(current)) {
					// outside call
					exitPointIds.add(call.getTarget().getId());
				}
			}
		}

		return exitPointIds;
	}

	private static TIntSet findEntryIds(FoldedCallGraph fcg, CallGraph cg, SDGNode current) {
		TIntSet entryIds = new TIntHashSet();

		if (current.getKind() == SDGNode.Kind.FOLDED) {
			List<SDGNode> foldedNodes = fcg.getFoldedNodesOf(current);
			for (SDGNode folded : foldedNodes) {
//				// get outside calls
//				boolean hasOutsideCalls = false;
//				for (SDGEdge edge : cg.incomingEdgesOf(folded)) {
//					if (edge.getKind() == SDGEdge.Kind.CALL && !foldedNodes.contains(edge.getSource())) {
//						hasOutsideCalls = true;
//						break;
//					}
//				}
//
//				if (hasOutsideCalls) {
//					entryIds.add(folded.getId());
//				}

				// add also internal calls to entries, as we need this info later on upon merging.
				entryIds.add(folded.getId());
			}
		} else {
			entryIds.add(current.getId());
		}

		return entryIds;
	}

	private void addNewLeafNodesToList(FoldedCallGraph changingCallGraph, Set<SDGNode> reachable, JobMessage done, Collection<SDGNode> toChange) {
		SDGNode node = done.getNode();

		// search nodes that are potential candidates for new leafs if the current node is removed.
		Set<SDGNode> recheck = new HashSet<SDGNode>();
		for (SDGEdge edge : changingCallGraph.incomingEdgesOf(node)) {
			SDGNode toCheck = changingCallGraph.getEdgeSource(edge);
			if (!toCheck.equals(node)) {
				recheck.add(toCheck);
			}
		}

		// see if some work packages depended on the current one and add them to the worklist, if all dependencies
		// are resolved.
		GraphUtil.removeLeaf(changingCallGraph, node);

		for (SDGNode check : recheck) {
			if (GraphUtil.isLeaf(changingCallGraph, check) && reachable.contains(check)) {
				toChange.add(check);
			}
		}
	}

	private static SDG createSubgraph(SDG sdg, FoldedCallGraph fcg, SDGNode node) {
		SDG stripped = null;
		String name = null;

		if (node.getKind() != SDGNode.Kind.FOLDED) {
			assert sdg.containsVertex(node) : "Not part of sdg: " + node.getKind() + "|" + node.getId() + " " + node.getLabel();
			assert node.getKind() == SDGNode.Kind.ENTRY
				: "Entry Node expected. Found: " + node.getKind() + "|" + node.getId() + " " + node.getLabel();


			name = node.getId() + "-" + node.getLabel();
			stripped = GraphUtil.stripGraph(sdg, node);
		} else {
			List<SDGNode> entries = fcg.getFoldedNodesOf(node);

			name = "";
			for (SDGNode entry : entries) {
				name += entry.getId() + "-" + entry.getLabel() + "_";
				if (name.length() > MAX_FILENAME_LENGTH + 1) {
					break;
				}
			}
			name = name.substring(0, name.length() - 1);

			stripped = GraphUtil.stripGraph(sdg, entries);
		}

		// limit long names to an arbitraty default
		// as no entrynode appears in the name of two different subgraphs, the names are
		// still unique as long as the id of the first node is completely contained.
		// This is the case as long as the id has less then 127 digits.
		if (name.length() > MAX_FILENAME_LENGTH) {
			name = name.substring(0, MAX_FILENAME_LENGTH) + "...";
		}
		stripped.setName("WP[" + name + "]");

		return stripped;
	}

	private static void writeToFile(String filename, SDG subgraph) throws IOException {
		final FileOutputStream fOs = new FileOutputStream(filename);
		final BufferedOutputStream bOs = new BufferedOutputStream(fOs);
		SDGSerializer.toPDGFormat(subgraph, bOs);
		bOs.close();
	}

	private static String makeFilename(SDG subgraph) {
		return subgraph.getName() + SUBGRAPH_FILE_SUFFIX;
	}

	// test builder
	public static void main(String[] args) {
		// 1. Load SDG from file
		info("Reading SDG from file \"" + TestJobber.SDG_FILE + "\"...");
		SDG sdg = null;
		try {
			sdg = SDG.readFrom(TestJobber.SDG_FILE);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot issue jobs when there is no valid SDG", e);
		}

		info("Extracting call graph...");
		CallGraph cg = GraphUtil.buildCallGraph(sdg);

		info("Starting WorkPackage builder...");
		WorkPackageBuilder wpBuilder = new WorkPackageBuilder(sdg, cg, TestJobber.CACHE_DIR);
		wpBuilder.start();
	}

	private static void info(String str) {
		System.out.println(str);
	}

}
