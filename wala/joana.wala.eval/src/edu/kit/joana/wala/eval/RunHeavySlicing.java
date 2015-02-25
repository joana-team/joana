/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.util.io.FileUtil;

import edu.kit.joana.wala.eval.util.HeavySlicer;
import edu.kit.joana.wala.eval.util.HeavySlicer.Task;

/**
 * Runs summary slices on any valid criteria in the given SDG and computes precision in percent of nodes on average in
 * a slice
 * 
 * @author Juergen Graf <juergen.graf@kit.edu>
 */
public class RunHeavySlicing {

	private static final String SDG_REGEX = ".*\\.pdg";
	
	private static final NumberFormat NF = DecimalFormat.getInstance();
	static {
		NF.setMaximumFractionDigits(2);
		NF.setMaximumFractionDigits(2);
	}
	
	public static void main(String[] args) {
		final HeavySlicer hs = new HeavySlicer(System.out);
		boolean recursive = false;
		List<File> filelist = new LinkedList<File>();
		
		
		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-recursive")) {
					recursive = true;
				} else if (args[i].equals("-help")) {
					hs.println("Usage: progname [-variant [new|old|delete]] [-runs <numberofruns>] [-recursive] [-help] <files or dir>");
					return;
				} else {
					// must be a file or directory name
					final String filename = args[i];
					
					final File f = new File(filename);
					if (!f.exists()) {
						hs.error("File does not exist: '" + filename + "' - skipping");
					} else if (!f.canRead()) {
						hs.error("File is not readable: '" + filename + "' -skipping");
					} else {
						filelist.add(f);
					}
					
					i++;
				}
			}
		}

		final List<Task> tasks = buildTaskList(filelist, recursive, hs);
		for (final Task t : tasks) {
			hs.work(t);
		}
	}
	
	private static List<Task> buildTaskList(final List<File> filelist, final boolean recursive, final HeavySlicer hs) {
		final List<Task> tasks = new LinkedList<Task>();
		
		for (final File f : filelist) {
			if (f.isDirectory()) {
				final Collection<File> result = FileUtil.listFiles(f.getAbsolutePath(), SDG_REGEX, recursive);
				for (final File found : result) {
					if (found.canRead()) {
						final Task t = hs.createTask(found);
						if (t != null) {
							tasks.add(t);
						}
					} else {
						hs.error("File is not readable: '" + found.getAbsolutePath() + "' -skipping");
					}
				}
			} else {
				final Task t = hs.createTask(f);
				if (t != null) {
					tasks.add(t);
				}
			}
		}
		
		return tasks;
	}

}
