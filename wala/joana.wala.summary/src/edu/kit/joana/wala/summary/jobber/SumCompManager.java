/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary.jobber;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CallGraph;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCallGraph;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;
import edu.kit.joana.wala.summary.EntryPointCache;
import edu.kit.joana.wala.summary.EntryPointCache.LoadEntryPointException;
import edu.kit.joana.wala.summary.ForwardReachablilitySlicer;
import edu.kit.joana.wala.summary.GraphUtil;
import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import edu.kit.joana.wala.util.jobber.Job;
import edu.kit.joana.wala.util.jobber.JobState;
import edu.kit.joana.wala.util.jobber.client.ManagerClient;
import edu.kit.joana.wala.util.jobber.io.MessageParseException;
import edu.kit.joana.wala.util.jobber.io.RespMessage;
import edu.kit.joana.wala.util.jobber.server.JobberServer;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * Manages the work packages. Chooses which ones are ready for computation.
 *
 * @author grafj
 *
 */
public class SumCompManager extends ManagerClient {

	public static final int MAX_FILENAME_LENGTH = 127;
	public static final String SUBGRAPH_FILE_SUFFIX = ".pdg";
	public static final int POLL_FOR_JOBS_FINISHED_MS = 100;

	private final String sdgFile;
	private final String cacheDir;
	private final LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();

	public SumCompManager(String serverIp, int port, String sdgFile, String cacheDir) {
		super(serverIp, port);
		this.sdgFile = sdgFile;
		this.cacheDir = (cacheDir.length() > 0 && !cacheDir.endsWith(File.separator)
				? cacheDir + File.separator : cacheDir);
	}

	public static void main(String[] args) {
		if (args.length == 3) {
			SumCompManager scm = new SumCompManager(args[0], JobberServer.PORT, args[1], args[2]);
			scm.start();
		} else {
			System.out.println("Usage: java -jar sumcomp.jar edu.kit.joana.wala.summary.jobber.SumCompManager <server adress> <sdg> <cache dir>");
		}
	}

	@Override
	public void run() {
		// 1. Load SDG from file
		info("Reading SDG from file \"" + sdgFile + "\"...");
		SDG sdg = null;
		try {
			sdg = SDG.readFrom(sdgFile);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot issue jobs when there is no valid SDG", e);
		}

		info("Extracting call graph...");
		CallGraph cg = GraphUtil.buildCallGraph(sdg);

		info("Folding strongly connected components...");

		final FoldedCallGraph fcg = GraphFolder.foldCallGraph(cg);
		final FoldedCallGraph changingCallGraph = GraphFolder.foldCallGraph(cg);
		Set<SDGNode> reachable = ForwardReachablilitySlicer.slice(fcg, fcg.getRoot());

		info("\t" + reachable.size() + " SCCs found");
		int folded = 0;
		for (SDGNode node : reachable) {
			if (node.getKind() == SDGNode.Kind.FOLDED && fcg.getFoldedNodesOf(node).size() > 1) {
				folded++;
			}
		}
		info("\t" + folded + " non-trivial SCCs");

		Set<SDGNode> leafs = GraphUtil.findLeafs(fcg);
		worklist.addAll(leafs);

		Set<SDGNode> totalWork = new HashSet<SDGNode>(reachable);
		SDGNode root = fcg.getRoot();
		if (root.getId() == 1) {
			// remove artificial root node
			totalWork.remove(root);
		} else {
			throw new IllegalStateException("No artificial root node with id 1 in call graph.");
		}

		List<JobMessage> currentRunning = new LinkedList<JobMessage>();

		long lastTimePoll = System.currentTimeMillis();

		while (!totalWork.isEmpty()) {
			if (!worklist.isEmpty()) {
				SDGNode current = worklist.removeFirst();

				JobMessage job = createJob(sdg, fcg, cg, current);

				sendJob(job);

				currentRunning.add(job);
			} else {
				try {
					info("Waiting for " + currentRunning.size() + " jobs to finish. " + totalWork.size() + " still todo.");
					sleep(POLL_FOR_JOBS_FINISHED_MS);
				} catch (InterruptedException e) {}
			}

			if (POLL_FOR_JOBS_FINISHED_MS < (System.currentTimeMillis() - lastTimePoll)) {
				List<JobMessage> jobsDone = new LinkedList<JobMessage>();
				List<JobMessage> jobsFailed = new LinkedList<JobMessage>();

				JobState[] states = checkStatus(currentRunning);

				int index = 0;
				for (JobMessage msg : currentRunning) {
					JobState state = states[index];
					index++;

					switch (state) {
					case DONE:
						jobsDone.add(msg);
						break;
					case FAILED:
						jobsFailed.add(msg);
						break;
					default: // nothing to do here
					}
				}


				// 1. reissue failed jobs
				for (JobMessage failed : jobsFailed) {
					sendJob(failed);
				}

				// 2. remove ok jobs from list and create new jobs that are now leafs in the folded call graph
				currentRunning.removeAll(jobsDone);
				for (JobMessage done : jobsDone) {
					totalWork.remove(done.getNode());
				}
				Set<SDGNode> newJobs = checkForNewLeafNodes(changingCallGraph, reachable, jobsDone);
				for (SDGNode jobNode : newJobs) {
					worklist.addLast(jobNode);
				}
			}
		}

		info("All workers finished.");

		shutDownWorkers();

		info("Merging summaryedges into sdg...");
		// add computed summaries to sdg.
		try {
			mergeComputedSummariesToSDG(sdg, cacheDir, sdgFile);
		} catch (LoadEntryPointException e) {
			throw new IllegalStateException("Could not merge results into SDG.", e);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Could not save SDG to file " + sdgFile, e);
		}

		info("All done.");
	}

	private static void mergeComputedSummariesToSDG(SDG sdg, String cacheDir, String sdgFile) throws LoadEntryPointException, FileNotFoundException {
		EntryPointCache cache = EntryPointCache.create(cacheDir);

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

		final FileOutputStream fOs = new FileOutputStream(sdgFile);
		final BufferedOutputStream bOs = new BufferedOutputStream(fOs);
		SDGSerializer.toPDGFormat(sdg, bOs);
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

				SDGEdge sumEdge =  SDGEdge.Kind.SUMMARY.newEdge(actualIn, actualOut);
				toAdjust.addEdge(actualIn, actualOut, sumEdge);
				newEdges++;
			}
		}

		return newEdges;
	}

	/**
	 * @param msg
	 * @return
	 */
	private JobState[] checkStatus(List<JobMessage> jobs) {
		JobState[] state = null;
		try {
			int[] jobIds = new int[jobs.size()];

			int index = 0;
			for (JobMessage msg : jobs) {
				jobIds[index] = msg.getJobberId();
				index++;
			}

			state = checkStatus(jobIds);
		} catch (UnknownHostException e) {
			throw new IllegalStateException("Could check status of jobs", e);
		} catch (IOException e) {
			throw new IllegalStateException("Could check status of jobs", e);
		} catch (MessageParseException e) {
			throw new IllegalStateException("Could check status of jobs", e);
		}

		return state;
	}

	/**
	 * @param changingCallGraph
	 * @param jobsDone
	 * @return
	 */
	private Set<SDGNode> checkForNewLeafNodes(FoldedCallGraph changingCallGraph, Set<SDGNode> reachable,
			List<JobMessage> jobsDone) {
		Set<SDGNode> leafs = new HashSet<SDGNode>();

		for (JobMessage job : jobsDone) {
			addNewLeafNodesToList(changingCallGraph, reachable, job, leafs);
		}

		return leafs;
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

	private void shutDownWorkers() {
		try {
			shutDownWorkers(SumCompWorker.JOB_TYPE);
		} catch (UnknownHostException e1) {
			throw new IllegalStateException("Could not send shut down message.", e1);
		} catch (IOException e1) {
			throw new IllegalStateException("Could not send shut down message.", e1);
		} catch (MessageParseException e1) {
			throw new IllegalStateException("Could not send shut down message.", e1);
		}
	}

	/**
	 * @param failed
	 */
	private void sendJob(JobMessage job) {
		info("Sending " + job);

		try {
			Job jobberJob = sendJob(SumCompWorker.JOB_TYPE, job.getSubgraphFile(), "", JobMessage.toCharBuffer(job));
			job.setJobberId(jobberJob.getId());
		} catch (UnknownHostException e) {
			throw new IllegalStateException("Could not send job: " + job.getSubgraphFile(), e);
		} catch (IOException e) {
			throw new IllegalStateException("Could not send job: " + job.getSubgraphFile(), e);
		} catch (MessageParseException e) {
			throw new IllegalStateException("Could not send job: " + job.getSubgraphFile(), e);
		}
	}

	private JobMessage createJob(SDG sdg, FoldedCallGraph fcg, CallGraph cg, SDGNode current) {
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

	private static String makeFilename(SDG subgraph) {
		return subgraph.getName() + SUBGRAPH_FILE_SUFFIX;
	}

	private static void writeToFile(String filename, SDG subgraph) throws IOException {
		final FileOutputStream fOs = new FileOutputStream(filename);
		final BufferedOutputStream bOs = new BufferedOutputStream(fOs);
		SDGSerializer.toPDGFormat(subgraph, bOs);
		bOs.close();
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

	@Override
	public void displayError(String msg) {
		System.err.println(msg);
	}

	@Override
	public void displayError(RespMessage msg) {
		System.err.println(msg.toString());
	}

	private static void info(String str) {
		System.out.println(str);
	}


	private static void debug(String str) {
		System.out.println(str);
	}
}
