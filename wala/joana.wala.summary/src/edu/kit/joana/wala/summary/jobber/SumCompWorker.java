/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary.jobber;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.util.CancelException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.wala.summary.EntryPointCache;
import edu.kit.joana.wala.summary.EntryPointCache.LoadEntryPointException;
import edu.kit.joana.wala.summary.EntryPointCache.StoreEntryPointException;
import edu.kit.joana.wala.summary.GraphUtil;
import edu.kit.joana.wala.summary.NullProgressMonitor;
import edu.kit.joana.wala.summary.SummaryComputation;
import edu.kit.joana.wala.summary.WorkPackage;
import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import edu.kit.joana.wala.summary.jobber.JobMessage.JobMessageFormatException;
import edu.kit.joana.wala.util.jobber.Job;
import edu.kit.joana.wala.util.jobber.JobState;
import edu.kit.joana.wala.util.jobber.client.WorkerClient;
import edu.kit.joana.wala.util.jobber.io.RespMessage;
import edu.kit.joana.wala.util.jobber.server.JobberServer;
import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;


public class SumCompWorker extends WorkerClient {

	public static final String JOB_TYPE = "sumcomp";
	private final EntryPointCache cache;
	private final String pathToCacheFiles;

	public SumCompWorker(String serverIp, int port, String pathToCacheFiles) {
		super(serverIp, port, JOB_TYPE);
		if (pathToCacheFiles.length() > 0 && !pathToCacheFiles.endsWith(File.separator)) {
			pathToCacheFiles += File.separator;
		}
		this.pathToCacheFiles = pathToCacheFiles;
		this.cache = EntryPointCache.create(pathToCacheFiles);
	}

	public static void main(String[] args) {
		if (args.length == 2) {
			SumCompWorker worker = new SumCompWorker(args[0], JobberServer.PORT, args[1]);
			worker.start();
		} else {
			System.out.println("Usage: java -jar sumcomp.jar edu.kit.joana.wala.summary.jobber.SumCompWorker <server adress> <cache dir>");
		}
	}

	@Override
	public void displayError(String msg) {
		System.err.println(msg);
	}

	@Override
	public void displayError(Throwable t) {
		System.err.println(t.getMessage());
		t.printStackTrace(System.err);
	}

	@Override
	public void displayError(RespMessage msg) {
		System.err.println(msg.toString());
	}

	@Override
	public JobState work(Job job) {
		log("Accepted job: " + job);

		try {
			JobMessage msg = JobMessage.fromCharBuffer(job.getData());

			log("Loading subgraph from file " + msg.getSubgraphFile());

			// 1. load subgraph from file
			SDG subgraph = SDG.readFrom(pathToCacheFiles + msg.getSubgraphFile());

			log("Adjusting subgraph with summary info.");

			// 2. adjust graph with summaryinfo of exitpoints
			GraphUtil.adjustSubgraphWithSummaries(subgraph, msg.getExitPoints(), cache);

			log("Creating entrypoints.");

			// 3. create entrypoints
			Set<EntryPoint> entryPoints = createEntryPoints(subgraph, msg.getEntries());

			log("Creating workpackages.");

			// 4. create workpackage
			WorkPackage wp = WorkPackage.create(subgraph, entryPoints, msg.getSubgraphFile());

			log("Running summary computation on " + wp);

			// 5. run summary computation on workpackage
			SummaryComputation.compute(wp, NullProgressMonitor.INSTANCE);

			log("Writing summary info to cache.");

			// 6. save summary info of entrypoints in cache (and files)
			for (EntryPoint ep : entryPoints) {
				cache.put(ep);
			}
		} catch (CancelException e) {
			displayError(e);
			return JobState.FAILED;
		} catch (JobMessageFormatException e) {
			displayError(e);
			return JobState.FAILED;
		} catch (IOException e) {
			displayError(e);
			return JobState.FAILED;
		} catch (LoadEntryPointException e) {
			displayError(e);
			return JobState.FAILED;
		} catch (StoreEntryPointException e) {
			displayError(e);
			return JobState.FAILED;
		}

		log("Job done: " + job);

		return JobState.DONE;
	}

	private static Set<EntryPoint> createEntryPoints(SDG subgraph, TIntCollection entries) {
		Set<EntryPoint> entryPoints = new HashSet<EntryPoint>();

		TIntIterator it = entries.iterator();
		while (it.hasNext()) {
			int entryId = it.next();

			SDGNode entry = subgraph.getNode(entryId);

			TIntCollection formalIns = new TIntArrayList();
			for (SDGNode fIn : subgraph.getFormalInsOfProcedure(entry)) {
				formalIns.add(fIn.getId());
			}

			TIntCollection formalOuts = new TIntArrayList();
			for (SDGNode fOut : subgraph.getFormalOutsOfProcedure(entry)) {
				formalOuts.add(fOut.getId());
			}

			EntryPoint ep = new EntryPoint(entryId, formalIns, formalOuts);
			entryPoints.add(ep);
		}

		return entryPoints;
	}

	private void log(Object o) {
		System.out.println("LOG(" + getId() + "): " + o);
	}

}
