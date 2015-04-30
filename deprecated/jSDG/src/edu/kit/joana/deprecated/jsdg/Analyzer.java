/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.WalaException;

import edu.kit.joana.deprecated.jsdg.SDGFactory.Config;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.util.Debug;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Log.LogLevel;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.SDGVerifier;
import edu.kit.joana.wala.util.VerboseProgressMonitor;
import edu.kit.joana.wala.util.WatchDog;

/**
 * Helper class to run SDG creation from shell.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class Analyzer {

	public static Config cfg;

	public static void main(String args[]) throws WalaException, IOException,
	IllegalArgumentException, CancelException, PDGFormatException, InvalidClassFileException {
		WatchDog watchdog = null;
		String logFile = null;

		// try catch block added due to problems with eclipse on mac osx
		// (exceptions got lost - program simply terminated)
		try {
			final IProgressMonitor progress = edu.kit.joana.deprecated.jsdg.wala.NullProgressMonitor.INSTANCE;

			String file = null;
			Integer timeout = 60; // stop per default after 1 hour
			int runs = 1;
			boolean lazy = false;
			boolean verify = true;

			if (args != null && args.length > 0) {
				for (int i = 0; i < args.length; i++) {
					if (args[i].equals("-cfg")) {
						if (args.length > i + 1) {
							file = args[i+1];
							i++;
						} else {
							System.err.println("No config file provided as argument.");
						}
					} else if (args[i].equals("-timeout")) {
						if (args.length > i + 1) {
							try {
								timeout = Integer.parseInt(args[i+1]);
								i++;
							} catch (NumberFormatException nf) {
								System.err.println("Timeout value is no number - timeout not enabled: " + nf.getMessage());
								timeout = null;
							}
						} else {
							timeout = null;
							System.err.println("No timeout value provided - timeout not enabled.");
						}
					} else if (args[i].equals("-runs")) {
						if (args.length > i + 1) {
							try {
								runs = Integer.parseInt(args[i+1]);
								i++;
								if (runs < 1) {
									runs = 1;
								} else {
									System.out.println("number of requested runs: " + runs);
								}
							} catch (NumberFormatException nf) {
								System.err.println("Number of runs value is no number - defaulting to single run: " + nf.getMessage());
								runs = 1;
							}
						} else {
							runs = 1;
							System.err.println("No value for number of runs provided - defaulting to 1.");
						}
					} else if (args[i].equals("-no-verify")) {
						verify = false;
					} else if (args[i].equals("-verify")) {
						verify = true;
					} else if (args[i].equals("-lazy")) {
						lazy = true;
					} else if (args[i].equals("-help")) {
						System.out.println("Usage: progname [-cfg <configfile>] [-timeout <minutes>] [-lazy] [-verify | -no-verify] [-help]");
						return;
					}
				}
			}

			if (file != null) {
				System.out.print("using config from file '" + file + "'");
				cfg = SDGFactory.Config.readFrom(new FileInputStream(file));
				System.out.println(" saving SDG to '" + cfg.outputSDGfile + "'");
			} else {
				System.out.println("Usage: progname [-cfg <configfile>] [-timeout <minutes>] [-lazy] [-help]");
				System.out.println("No configuration file selected - aborting...");
				return;
			}
			//System.out.println(cfg.toString());
			//System.out.println(Debug.getSettings());
			Log.noLogging();
			logFile = cfg.logFile;
			cfg.logFile = null;

			if (lazy) {
				File f = new File(cfg.outputSDGfile);
				if (f.exists()) {
					System.out.println("SDG file already exists - nothing to do for me...");
					if (verify) {
						SDG sdg = SDG.readFrom(cfg.outputSDGfile);
						SDGVerifier.verify(sdg, !cfg.useWalaSdg, cfg.addControlFlow);
					}
					return;
				}
			}

			if (timeout != null) {
				System.out.println("TIMEOUT set to " + timeout + " minutes.");

				final long timeoutInMs = ((long) timeout) * 60L * 1000L;
				// give program 1 minute to exit when progress.cancel() was triggered
				final long timeToCleanup = 60L * 60L * 1000L;

				watchdog = new WatchDog(progress, timeoutInMs, timeToCleanup);
				watchdog.start();
			}

			final Activator act = Activator.getDefault();
//			act.getFactory().runExtractImmutables(cfg, progress);

			long duration = 0;;
			edu.kit.joana.ifc.sdg.graph.SDG sdg = null;
			for (int i = 0; i < runs; i++) {
				final long startTime = System.currentTimeMillis();
				sdg = act.getFactory().getJoanaSDG(cfg, progress);
				final long endTime = System.currentTimeMillis();
				if (i == 0 || endTime - startTime < duration) {
					duration = endTime - startTime;
				}
			}

			if (watchdog != null && watchdog.isAlive()) {
				// tell watchdog to stop
				System.out.println("Shutting down watchdog.");
				watchdog.done();
				watchdog.interrupt();
			}

			// do not interrupt writing to file. we've come so far - we wont stop now.
			if (cfg.outputSDGfile != null) {
				final IProgressMonitor progress2 = edu.kit.joana.deprecated.jsdg.wala.NullProgressMonitor.INSTANCE;
				progress2.beginTask("Saving SDG to " + cfg.outputSDGfile, -1);
				BufferedOutputStream bOut = new BufferedOutputStream(
						new FileOutputStream(cfg.outputSDGfile));
				SDGSerializer.toPDGFormat(sdg, bOut);
				progress2.done();
			}

			if (verify) {
				SDGVerifier.verify(sdg, !cfg.useWalaSdg, cfg.addControlFlow);
			}

			cfg.logFile = logFile;
			final PrintStream out = new PrintStream(cfg.logFile);
			out.println(sdg.vertexSet().size() + " nodes and " + sdg.edgeSet().size() + " edges in " + duration + " ms");
			out.println("using config file: " + file);
			out.println(cfg.toString());
			out.flush();
			out.close();
			System.out.println(sdg.vertexSet().size() + " nodes and " + sdg.edgeSet().size() + " edges in " + duration
					+ " ms logged to '" + cfg.logFile + "'");
		} catch (Exception e) {
			if (cfg.logFile == null) {
				cfg.logFile = logFile;
			}
			if (cfg.logFile != null) {
				final PrintStream out = new PrintStream(cfg.logFile);
				out.println("ERROR");
				e.printStackTrace(out);
				out.flush();
				out.close();
			}
			e.printStackTrace();
		} finally {
			if (watchdog != null && watchdog.isAlive()) {
				// tell watchdog to stop
				System.out.println("Shutting down watchdog.");
				watchdog.done();
				watchdog.interrupt();
			}

			//Date date = new Date();
			//Log.info("Stopped Analysis at " + date);
		}
	}

}
