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

/**
 * Manages the work packages. Chooses which ones are ready for computation.
 *
 * @author grafj
 *
 */
public class SumCompManagerWithSeperatePackager extends ManagerClient {

	public static final int POLL_FOR_JOBS_FINISHED_MS = 100;

	private final String sdgFile;
	private final String cacheDir;


	public SumCompManagerWithSeperatePackager(String serverIp, int port, String sdgFile, String cacheDir) {
		super(serverIp, port);
		this.sdgFile = sdgFile;
		this.cacheDir = (cacheDir.length() > 0 && !cacheDir.endsWith(File.separator)
				? cacheDir + File.separator : cacheDir);
	}

	public static void main(String[] args) {
		if (args.length == 3) {
			SumCompManagerWithSeperatePackager scm = new SumCompManagerWithSeperatePackager(args[0], JobberServer.PORT, args[1], args[2]);
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

		WorkPackageBuilder wbBuilder = new WorkPackageBuilder(sdg, cg, cacheDir);
		wbBuilder.start();

		info("Folding strongly connected components...");

		//final FoldedCallGraph fcg = GraphFolder.foldCallGraph(cg);
		final FoldedCallGraph changingCallGraph = GraphFolder.foldCallGraph(cg);
		Set<SDGNode> reachable = ForwardReachablilitySlicer.slice(changingCallGraph, changingCallGraph.getRoot());

		info("\t" + reachable.size() + " SCCs found");
		int folded = 0;
		for (SDGNode node : reachable) {
			if (node.getKind() == SDGNode.Kind.FOLDED && changingCallGraph.getFoldedNodesOf(node).size() > 1) {
				folded++;
			}
		}
		info("\t" + folded + " non-trivial SCCs");

		LinkedList<JobMessage> worklist = new LinkedList<JobMessage>();
		Set<SDGNode> dependenciesResolved = GraphUtil.findLeafs(changingCallGraph);

		Set<SDGNode> totalWork = new HashSet<SDGNode>(reachable);
		SDGNode root = changingCallGraph.getRoot();
		if (root.getId() == 1) {
			// remove artificial root node
			totalWork.remove(root);
		} else {
			throw new IllegalStateException("No artificial root node with id 1 in call graph.");
		}

		List<JobMessage> currentlyQueuedOrRunning = new LinkedList<JobMessage>();

		while (!totalWork.isEmpty()) {
			List<JobMessage> newJobs = wbBuilder.getReadyJobs(dependenciesResolved);
			worklist.addAll(newJobs);

			if (!worklist.isEmpty()) {
				for (JobMessage job : worklist) {
					sendJob(job);
					currentlyQueuedOrRunning.add(job);
				}

				worklist.clear();
			}


			try {
				sleep(POLL_FOR_JOBS_FINISHED_MS);
			} catch (InterruptedException e) {}

			// look for finished jobs
			List<JobMessage> jobsDone = new LinkedList<JobMessage>();
			List<JobMessage> jobsFailed = new LinkedList<JobMessage>();

			JobState[] states = checkStatus(currentlyQueuedOrRunning);

			int index = 0;
			for (JobMessage msg : currentlyQueuedOrRunning) {
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
			currentlyQueuedOrRunning.removeAll(jobsDone);
			for (JobMessage done : jobsDone) {
				totalWork.remove(done.getNode());
			}

			Set<SDGNode> newResolvedDependencies = checkForNewLeafNodes(changingCallGraph, reachable, jobsDone);
			dependenciesResolved.addAll(newResolvedDependencies);
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

				SDGEdge sumEdge = new SDGEdge(actualIn, actualOut, SDGEdge.Kind.SUMMARY);
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
