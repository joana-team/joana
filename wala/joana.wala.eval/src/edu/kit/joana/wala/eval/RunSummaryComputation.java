/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.io.FileUtil;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.eval.util.SummaryEdgeDriver;
import edu.kit.joana.wala.eval.util.SummaryEdgeDriver.Result;
import edu.kit.joana.wala.util.WatchDog;

/**
 * Performs summary edge computation on each SDG found in the given directory.
 * 
 * @author Juergen Graf <juergen.graf@kit.edu>
 */
public class RunSummaryComputation {

	private static final String SDG_REGEX = ".*\\.pdg";
	
	private static long NO_MULTIPLE_RUNS_THRESHOLD = 1000000; 
	
	private enum Variant { OLD, NEW, DELETE };
	
	public static class Task {
		String filename;
		String logname;
		long startTime;
		long endTime;
		int numOfSumEdges;
		
		public long duration() {
			return endTime - startTime;
		}
	}
	
	public static void main(String[] args) {
		Variant v = Variant.NEW;
		boolean recursive = false;
		boolean lazy = false;
		int runs = 1;
		int timeout = -1;
		List<File> filelist = new LinkedList<File>();
		
		
		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-variant")) {
					if (args.length > i + 1) {
						final String variant = args[i+1];
						v = Variant.valueOf(variant.toUpperCase());
						i++;
					} else {
						error("No computation variant option provided.");
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
							error("Number of runs value is no number - defaulting to single run: " + nf.getMessage());
							runs = 1;
						}
					} else {
						runs = 1;
						error("No value for number of runs provided - defaulting to 1.");
					}
				} else if (args[i].equals("-timeout")) {
					if (args.length > i + 1) {
						try {
							timeout = Integer.parseInt(args[i+1]);
							i++;
						} catch (NumberFormatException nf) {
							error("Timeout value is no number - timeout not enabled: " + nf.getMessage());
							timeout = -1;
						}
					} else {
						timeout = -1;
						error("No timeout value provided - timeout not enabled.");
					}
				} else if (args[i].equals("-recursive")) {
					recursive = true;
				} else if (args[i].equals("-lazy")) {
					lazy = true;
				} else if (args[i].equals("-help")) {
					println("Usage: progname [-variant [new|old|delete]] [-runs <numberofruns>] [-recursive] [-help] <files or dir>");
					return;
				} else {
					// must be a file or directory name
					final String filename = args[i];
					
					final File f = new File(filename);
					if (!f.exists()) {
						error("File does not exist: '" + filename + "' - skipping");
					} else if (!f.canRead()) {
						error("File is not readable: '" + filename + "' - skipping");
					} else {
						filelist.add(f);
					}
					
					i++;
				}
			}
		}
		
		if (timeout > 0) {
			println("timeout set to " + timeout + " minutes.");
		}
		
		final List<Task> tasks = buildTaskList(filelist, recursive, lazy, v);
		for (final Task t : tasks) {
			work(t, v, runs, timeout);
		}
	}
	
	private static void work(final Task t, final Variant v, final int runs, final int timeout) {
		PrintStream log = null;
		if (v != Variant.DELETE) {
			try {
				log = new PrintStream(t.logname);
			} catch (FileNotFoundException e1) {
				error(e1);
			}
		}

		final IProgressMonitor progress = new NullProgressMonitor();
		final WatchDog watchdog;
		
		if (timeout > 0) {
			final long timeoutInMs = ((long) timeout) * 60L * 1000L;
			// give program 1 minute to exit when progress.cancel() was triggered
			final long timeToCleanup = 60L * 60L * 1000L;

			watchdog = new WatchDog(progress, timeoutInMs, timeToCleanup);
			watchdog.start();
		} else {
			watchdog = null;
		}
		
		print("working on '" + t.filename + "' with " + v.name() + "... ");

		SummaryEdgeDriver su = null;
		final SummaryEdgeDriver delete = SummaryEdgeDriver.getDeleteVariant();
		
		switch (v) {
		case NEW:
			su = SummaryEdgeDriver.getNewVariant();
			break;
		case OLD:
			su = SummaryEdgeDriver.getOldVariant();
			break;
		case DELETE:
			su = delete;
			break;
		}

		boolean error = false;
		boolean timeoutOccured = false;
		try {
			final SDG sdg = SDG.readFromAndUseLessHeap(t.filename);
			
			for (int i = 0; i < runs; i++) {
				if (v != Variant.DELETE) {
					delete.compute(sdg, progress);
				} else if (i > 0) {
					// only run once for delete variant
					break;
				}
				
				final Result r = su.compute(sdg, progress);
	
				if (i == 0 || t.duration() > r.duration()) {
					t.startTime = r.startTime;
					t.endTime = r.endTime;
					t.numOfSumEdges = r.numSumEdges;
				}
				
				if (t.duration() > NO_MULTIPLE_RUNS_THRESHOLD) {
					break;
				}
			}
			
			// write results back
			SDGSerializer.toPDGFormat(sdg, new FileOutputStream(t.filename));
		} catch (IOException e) {
			error(e);
			error = true;
		} catch (CancelException e) {
			if (watchdog != null && progress.isCanceled()) {
				// expected cancel
				timeoutOccured = true;
			} else {
				error(e);
			}
			error = true;
		} catch (NullPointerException e) {
			error(e);
			error = true;
		} finally {
			if (watchdog != null && watchdog.isAlive()) {
				// tell watchdog to stop
				watchdog.done();
				watchdog.interrupt();
			}
		}
		
		if (!error) {
			println(t.numOfSumEdges + " edges in " + t.duration() + " ms");
		} else {
			println(timeoutOccured ? "TIMEOUT ERROR" : "ERROR");
		}
		
		if (v != Variant.DELETE && log != null) {
			if (!error) {
				log.println(t.numOfSumEdges + " edges in " + t.duration() + " ms for " + t.filename);
			} else {
				log.println(timeoutOccured ? "TIMEOUT ERROR" : "ERROR");
			}
			log.flush();
			log.close();
		}
	}
	
	private static String logFile(final String pdgFile, final Variant v) {
		switch (v) {
		case DELETE:
			return pdgFile + "-sumdel.log";
		case NEW:
			return pdgFile + "-sumnew.log";
		case OLD:
			return pdgFile + "-sumold.log";
		}
		
		throw new IllegalArgumentException("unkown variant: " + v);
	}
	
	private static boolean nonEmptyFileExists(final String filename) {
		final File lf = new File(filename);

		if (lf.exists() && lf.isFile() && lf.length() > 0) {
			try {
				final BufferedReader br = new BufferedReader(new FileReader(lf));
				final String line = br.readLine();
				br.close();
				return !line.contains("ERROR");
			} catch (IOException exc) {}
		}
		
		return false;
	}
	
	private static boolean needsToBeComputed(final boolean lazy, final String sdgFile, final String logFile) {
		if (!lazy) {
			return true;
		}
		
		if (nonEmptyFileExists(logFile)) {
			final File sdg = new File(sdgFile);
			final File log = new File(logFile);
			try {
				final Map<String,Object> sdgAttr = Files.readAttributes(sdg.toPath(), "lastModifiedTime");
				final Map<String,Object> logAttr = Files.readAttributes(log.toPath(), "lastModifiedTime");
				if (sdgAttr.containsKey("lastModifiedTime") && logAttr.containsKey("lastModifiedTime")) {
					final FileTime ftSdg = (FileTime) sdgAttr.get("lastModifiedTime");
					final FileTime ftLog = (FileTime) logAttr.get("lastModifiedTime");
					if (ftSdg.compareTo(ftLog) <= 0) {
						// sdg before after log file -> no rerun needed
						System.out.println("skipping " + sdg.getName() + " as it is older or same date then summary log.");
						return false;
					}
				}
			} catch (IOException e) {
				error(e);
			}
		}
		
		return true;
	}
	
	private static List<Task> buildTaskList(final List<File> filelist, final boolean recursive, final boolean lazy,
			final Variant v) {
		final List<Task> tasks = new LinkedList<Task>();
		
		for (final File f : filelist) {
			if (f.isDirectory()) {
				final Collection<File> result = FileUtil.listFiles(f.getAbsolutePath(), SDG_REGEX, recursive);
				for (final File found : result) {
					if (found.canRead()) {
						final Task t = new Task();
						t.filename = found.getAbsolutePath();
						t.logname = logFile(t.filename, v);
						if (needsToBeComputed(lazy, t.filename, t.logname)) {
							tasks.add(t);
						}
					} else {
						error("File is not readable: '" + found.getAbsolutePath() + "' -skipping");
					}
				}
			} else {
				final Task t = new Task();
				t.filename = f.getAbsolutePath();
				t.logname = logFile(t.filename, v);
				if (needsToBeComputed(lazy, t.filename, t.logname)) {
					tasks.add(t);
				}
			}
		}
		
		return tasks;
	}

	public static void print(final String str) {
		System.out.print(str);
	}

	public static void println(final String str) {
		System.out.println(str);
	}
	
	public static void error(final String str) {
		System.err.println(str);
	}

	public static void error(final Throwable t) {
		t.printStackTrace();
	}

}
