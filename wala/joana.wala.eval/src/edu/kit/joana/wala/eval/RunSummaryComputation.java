/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.io.FileUtil;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.eval.util.SummaryEdgeDriver;
import edu.kit.joana.wala.eval.util.SummaryEdgeDriver.Result;

/**
 * Performs summary edge computation on each SDG found in the given directory.
 * 
 * @author Juergen Graf <juergen.graf@kit.edu>
 */
public class RunSummaryComputation {

	private static final String SDG_REGEX = ".*\\.pdg";
	
	private enum Variant { OLD, NEW, DELETE };
	
	public static class Task {
		String filename;
		String logname;
		long startTime;
		long endTime;
		int numOfSumEdges;
	}
	
	public static void main(String[] args) {
		Variant v = Variant.NEW;
		boolean recursive = false;
		int runs = 1;
		List<File> filelist = new LinkedList<File>();
		
		
		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-variant")) {
					if (args.length > i + 1) {
						final String variant = args[i+1];
						v = Variant.valueOf(variant.toUpperCase());
						i++;
					} else {
						System.err.println("No computation variant option provided.");
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
				} else if (args[i].equals("-recursive")) {
					recursive = false;
				} else if (args[i].equals("-help")) {
					System.out.println("Usage: progname [-variant [new|old|delete]] [-runs <numberofruns>] [-recursive] [-help] <files or dir>");
					return;
				} else {
					// must be a file or directory name
					final String filename = args[i];
					
					final File f = new File(filename);
					if (!f.exists()) {
						System.err.println("File does not exist: '" + filename + "' - skipping");
					} else if (!f.canRead()) {
						System.err.println("File is not readable: '" + filename + "' -skipping");
					} else {
						filelist.add(f);
					}
					
					i++;
				}
			}
		}

		final List<Task> tasks = buildTaskList(filelist, recursive);
		for (final Task t : tasks) {
			System.out.println("working on '" + t.filename + "'");
			work(t, v);
		}
	}
	
	private static void work(final Task t, final Variant v) {
		SummaryEdgeDriver su = null;
		
		switch (v) {
		case NEW:
			su = SummaryEdgeDriver.getNewVariant();
			break;
		case OLD:
			su = SummaryEdgeDriver.getOldVariant();
			break;
		case DELETE:
			su = null; //TODO: add a remove edges driver
			break;
		}
		
		
		try {
			final SDG sdg = SDG.readFrom(t.filename);
			final Result r = su.compute(sdg, NullProgressMonitor.INSTANCE);

			t.startTime = r.startTime;
			t.endTime = r.endTime;
			t.numOfSumEdges = r.numSumEdges;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}
	
	private static List<Task> buildTaskList(final List<File> filelist, final boolean recursive) {
		final List<Task> tasks = new LinkedList<Task>();
		
		for (final File f : filelist) {
			if (f.isDirectory()) {
				final Collection<File> result = FileUtil.listFiles(f.getAbsolutePath(), SDG_REGEX, recursive);
				for (final File found : result) {
					if (found.canRead()) {
						final Task t = new Task();
						t.filename = found.getAbsolutePath();
						t.logname = t.filename + "-summary.log";
						tasks.add(t);
					} else {
						System.err.println("File is not readable: '" + found.getAbsolutePath() + "' -skipping");
					}
				}
			} else {
				final Task t = new Task();
				t.filename = f.getAbsolutePath();
				t.logname = t.filename + "-summary.log";
				tasks.add(t);
			}
		}
		
		return tasks;
	}

}
