/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary.jobber;

import java.io.IOException;

import edu.kit.joana.wala.util.jobber.server.JobberServer;

/**
 * Tests distributed SummaryEdge computation.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class TestJobber {

	public static final String CACHE_DIR = "/Users/juergengraf/tmp/sdg/cache";
	public static final String SDG_FILE =
		"/Users/juergengraf/tmp/sdg/conc.TimeTravel.pdg";
//		"/Users/juergengraf/tmp/sdg/javacard.framework.JCMain.pdg";
//		"/Users/juergengraf/tmp/sdg/j2me.bExplore.pdg";
	public static final int NUMBER_OF_WORKERS = 8;

	public static void main(String[] argv) throws IOException {
		JobberServer server = JobberServer.create();
		server.start();

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {}

		SumCompManagerWithSeperatePackager manager =
			new SumCompManagerWithSeperatePackager("localhost", JobberServer.PORT, SDG_FILE, CACHE_DIR);
		manager.start();

		SumCompWorker[] workers = new SumCompWorker[NUMBER_OF_WORKERS];
		for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
			workers[i] = new SumCompWorker("localhost", JobberServer.PORT, CACHE_DIR);
			workers[i].start();

		}

		try {
			manager.join();
			for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
				workers[i].join();
			}
		} catch (InterruptedException e) {}

		System.out.println("TestJobber finished.");
	}

}
