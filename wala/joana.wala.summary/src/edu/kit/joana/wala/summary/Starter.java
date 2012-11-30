/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary;

import java.io.IOException;

import edu.kit.joana.wala.summary.jobber.SumCompManagerWithSeperatePackager;
import edu.kit.joana.wala.summary.jobber.SumCompWorker;
import edu.kit.joana.wala.util.jobber.server.JobberServer;


/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class Starter {

	public static void main(String[] args) throws IOException {
		Runtime rt = Runtime.getRuntime();
		info("Heap size: " + (rt.maxMemory() / (1024 * 1024))  + "M, Free mem: " + (rt.freeMemory() / (1024 * 1024)) + "M");

		if (args.length < 1) {
			printUsage();
			return;
		}
		final String mode = args[0].toLowerCase().trim();

		if ("manager".equals(mode)) {
			JobberServer server = null;

			if (args.length < 3) {
				printUsage();
				return;
			}

			if (args.length > 3) {
				int port = JobberServer.PORT;
				try {
					port = Integer.parseInt(args[3]);
				} catch (NumberFormatException exc) {
					error("Could not parse port number: '" + args[3] + "' - using default port.");
				}

				server = JobberServer.create(port);
			} else {
				server = JobberServer.create();
			}

			info("Starting jobber server at port " + server.getPort());

			server.start();

			// wait 2 secs for the server to settle
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {}


			final String sdgFile = args[1];
			final String cacheDir = args[2];
			info("Starting manager for sdg " + sdgFile + " cache dir: " + cacheDir);

			SumCompManagerWithSeperatePackager manager =
				new SumCompManagerWithSeperatePackager("localhost", server.getPort(), sdgFile, cacheDir);

			manager.start();
		} else if ("worker".equals(mode)) {
			if (args.length < 3) {
				printUsage();
				return;
			}

			final String cacheDir = args[1];
			final String serverIp = args[2];

			SumCompWorker worker;

			if (args.length > 3) {
				int port = JobberServer.PORT;
				try {
					port = Integer.parseInt(args[3]);
				} catch (NumberFormatException exc) {
					error("Could not parse port number: '" + args[3] + "' - using default port.");
				}

				worker = new SumCompWorker(serverIp, port, cacheDir);
			} else {
				worker = new SumCompWorker(serverIp, JobberServer.PORT, cacheDir);
			}

			info("Spawning worker at " + cacheDir + " talking to " + serverIp + ":" + worker.getServerPort());

			worker.start();
		} else {
			printUsage();
			return;
		}
	}

	private static void info(String msg) {
		System.out.println(msg);
	}

	private static void error(String msg) {
		System.err.println(msg);
	}

	private static void printUsage() {
		System.out.println("Usage: java -jar jsdg-summary.jar [manager|worker] <specific_args>\n");
		System.out.println(
				  "The parallel summary computation needs 1 (combined) server and manager instance\n"
				+ "that holds the job list and issues new jobs to the list.\n"
				+ "Then there is a bunch (1-many) of worker instances that poll jobs from the\n"
				+ "server and do the actual work.\n"
				+ "\nStart Manager and Server:\n"
				+ "java -jar jsdg-summary.jar manager <sdg> <cachedir> [<port>]\n"
				+ "\t<sdg> filename of the sdg that should be used for summary computation.\n"
				+ "\t<cachedir> path to cachefiles that will hold per method summaries. This\n"
				+ "\t           is used for communication with the workers. So it must be \n"
				+ "\t           accessible to all worker and manager instances.\n"
				+ "\t<port> optional port of the server - when not running on default port.\n"
				+ "\t       default port is: " + JobberServer.PORT + "\n"
				+ "\nStart Worker:\n"
				+ "java -jar jsdg-summary.jar worker <cachedir> <serverip> [<port>]\n"
				+ "\t<cachedir> path to cachefiles that will hold per method summaries. This\n"
				+ "\t           is used for communication with the workers. So it must be \n"
				+ "\t           accessible to all worker and manager instances.\n"
				+ "\t<serverip> ip adress or name of the server.\n"
				+ "\t<port> optional port of the server - when not running on default port.\n"
		);
	}

}
