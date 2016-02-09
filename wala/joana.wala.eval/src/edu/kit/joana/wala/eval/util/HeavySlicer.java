/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.wala.eval.util.LineNrSlicer.Line;
import edu.kit.joana.wala.eval.util.LineNrSlicer.Result;

/**
 * Runs summary slices on any valid criteria in the given SDG and computes precision in percent of nodes on average in
 * a slice
 * 
 * @author Juergen Graf <juergen.graf@kit.edu>
 */
public class HeavySlicer {

	private static final NumberFormat NF = DecimalFormat.getInstance();
	static {
		NF.setMaximumFractionDigits(2);
		NF.setMaximumFractionDigits(2);
	}

	private final PrintStream out;
	
	public static class Task {
		String filename;
		String logname;
		boolean extendedStats = false;
		int numOfLines;
		int numOfRelevantNodes;
		int numOfTotalNodes;
		long avgRelevantNodesInSlice;
		long avgTotalNodesInSlice;
		long avgLinesInSlice;
		boolean summaryEdgesFound;

		public String percentTotalStr() {
			return NF.format(percentTotal()) + "%";
		}
		
		public String percentRelevantStr() {
			return NF.format(percentRelevant()) + "%";
		}
		
		public String percentLinesStr() {
			return NF.format(percentLines()) + "%";
		}
		
		public String allPercentsStr() {
			return percentLinesStr() + " (" + avgLinesInSlice + " of " + numOfLines + ")";
		}
		
		public double percentTotal() {
			if (numOfTotalNodes == 0) return 1;
			
			return (100.0 * avgTotalNodesInSlice) / ((double) numOfTotalNodes);
		}
		
		public double percentLines() {
			if (numOfLines == 0) return 1;
			
			return (100.0 * avgLinesInSlice) / ((double) numOfLines);
		}
		
		public double percentRelevant() {
			if (numOfRelevantNodes == 0) return 1;

			return (100.0 * avgRelevantNodesInSlice) / ((double) numOfRelevantNodes);
		}
		
		public String toString() {
			return (summaryEdgesFound ? "" : "no summary edges found\n")
			+	"nodes   : " + percentTotalStr() + " (" + avgTotalNodesInSlice + " of " + numOfTotalNodes + ")\n"
			+	"relevant: " + percentRelevantStr() + " (" + avgRelevantNodesInSlice + " of " + numOfRelevantNodes + ")\n"
			+	"lines   : " + percentLinesStr() + " (" + avgLinesInSlice + " of " + numOfLines + ")";
		}
	}
	
	public HeavySlicer(final PrintStream out) {
		this.out = out;
	}
	
	public static void main(String argv[]) {
		final HeavySlicer hs = new HeavySlicer(System.out);
		final String[] todo = new String[] {
//				"C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JRE14_JavaGrandeCrypt_PtsType_Graph-noopt.pdg",
//				"C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JRE14_JavaGrandeCrypt_PtsInst_Graph-noopt.pdg",
//				"C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JRE14_JavaGrandeCrypt_PtsObj_Graph-noopt.pdg",
//				"C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JC_Purse_PtsInst_Graph_StdNoOpt.pdg",
//				"C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JRE14_JavaGrandeSparseMatmult_PtsType_Graph.pdg",
				
//				"C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\wala-0-cfa\\jc-Purse.pdg",
//				"C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\tree-0-cfa\\jc-Purse.pdg",
				"C:\\Users\\Juergen\\git\\joana\\example\\output\\test_JC_Purse_PtsType_Graph_Std.pdg",
				
//				"C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\tree-0-cfa\\jre14-jg-Crypt.pdg",
//				"C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\tree-0-1-cfa\\jre14-jg-Crypt.pdg",
//				"C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\tree-objsens\\jre14-jg-Crypt.pdg",
//				"C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\wala-0-cfa\\jre14-jg-Crypt.pdg",
//				"C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\wala-0-1-cfa\\jre14-jg-Crypt.pdg",
//				"C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\wala-objsens\\jre14-jg-Crypt.pdg",
			//	"C:\\Users\\Juergen\\git\\joana\\deprecated\\jSDG\\out\\tree-0-1-cfa\\jc-Purse.pdg",
		};

//		final Task t1 = hs.createTask(todo[0]);
//		final Task t2 = hs.createTask(todo[1]);
//		hs.compare(t1, t2, new Line("crypt/IDEATest.java", 208));
		
		for (final String sdg : todo) {
			final Task tsk = hs.createTask(sdg);
			//tsk.extendedStats = true;
			hs.work(tsk);
		}
//		
	}
	
	private static boolean hasSummaryEdges(final SDG sdg) {
		for (final SDGEdge e : sdg.edgeSet()) {
			switch (e.getKind()) {
			case SUMMARY:
			case SUMMARY_DATA:
			case SUMMARY_NO_ALIAS:
				return true;
			default:
			}
		}
		return false;
	}

	public void compare(final Task t1, final Task t2, final Line line) {
		println("comparing on '" + t1.filename + "' and '" + t2.filename + "'");
		println("on line " + line);

		try {
			final LineNrSlicer lns1 = getSlicer(t1);
			final LineNrSlicer lns2 = getSlicer(t2);
			
			final Set<Line> slice1 = lns1.sliceBackward(line);
			final Set<Line> slice2 = lns2.sliceBackward(line);
			
			final SortedSet<Line> notInSlice1 = new TreeSet<Line>();
			notInSlice1.addAll(slice2);
			notInSlice1.removeAll(slice1);
			final SortedSet<Line> notInSlice2 = new TreeSet<Line>();
			notInSlice2.addAll(slice1);
			notInSlice2.removeAll(slice2);
			
			if (!notInSlice2.isEmpty()) {
				println(line + " has " + notInSlice2.size() + " additional lines in '" + t1.filename + "'");
				printLines(notInSlice2);
			}

			if (!notInSlice1.isEmpty()) {
				println(line + " has " + notInSlice1.size() + " additional lines in '" + t2.filename + "'");
				printLines(notInSlice1);
			}
		} catch (IOException e) {
			error(e);
		} catch (NullPointerException e) {
			error(e);
		}
	}
	
	public void compare(final Task t1, final Task t2) {
		println("comparing on '" + t1.filename + "' and '" + t2.filename + "'");

		try {
			final LineNrSlicer lns1 = getSlicer(t1);
			final LineNrSlicer lns2 = getSlicer(t2);
			
			final SortedSet<Line> lines1 = lns1.getLines();
			final SortedSet<Line> lines2 = lns2.getLines();
			
			final SortedSet<Line> notIn1 = new TreeSet<Line>();
			notIn1.addAll(lines2);
			notIn1.removeAll(lines1);
			final SortedSet<Line> notIn2 = new TreeSet<Line>();
			notIn2.addAll(lines1);
			notIn2.removeAll(lines2);
			final SortedSet<Line> inBoth = new TreeSet<Line>();
			inBoth.addAll(lines1);
			inBoth.removeAll(notIn2);
			
			println(inBoth.size() + " similar lines.");
			println(notIn1.size() + " lines not in '" + t1.filename + "':");
			printLines(notIn1);
			println(notIn2.size() + " lines not in '" + t2.filename + "':");
			printLines(notIn2);
			
			println("start slicing...");
			
			for (final Line l : inBoth) {
				final Set<Line> slice1 = lns1.sliceBackward(l);
				final Set<Line> slice2 = lns2.sliceBackward(l);
				
				final SortedSet<Line> notInSlice1 = new TreeSet<Line>();
				notInSlice1.addAll(slice2);
				notInSlice1.removeAll(slice1);
				final SortedSet<Line> notInSlice2 = new TreeSet<Line>();
				notInSlice2.addAll(slice1);
				notInSlice2.removeAll(slice2);
				
				if (!notInSlice2.isEmpty()) {
					println(l + " has " + notInSlice2.size() + " additional lines in '" + t1.filename + "'");
				}

				if (!notInSlice1.isEmpty()) {
					println(l + " has " + notInSlice1.size() + " additional lines in '" + t2.filename + "'");
				}
			}
		} catch (IOException e) {
			error(e);
		} catch (NullPointerException e) {
			error(e);
		}
	}

	private void printLines(final Collection<Line> lines) {
		for (final Line l : lines) {
			println(l.toString());
		}
	}
	
	private LineNrSlicer getSlicer(final Task t) throws IOException {
		final SDG sdg = SDG.readFromAndUseLessHeap(t.filename);

		t.summaryEdgesFound = hasSummaryEdges(sdg);
		if (!t.summaryEdgesFound) {
			print(" _no_summary_edges_found_ ");
		}
		
		final LineNrSlicer lns = new LineNrSlicer(sdg, t.extendedStats);
		
		return lns;
	}
	
	public synchronized boolean work(final Task t) {
		PrintStream log = null;
		try {
			log = new PrintStream(t.logname);
		} catch (FileNotFoundException e1) {
			error(e1);
			return false;
		}
		print("working on '" + t.filename + "' ... ");

		boolean error = false;
		try {
			final SDG sdg = SDG.readFromAndUseLessHeap(t.filename);

//			long allSliceNodes = 0;
//			long allRelevantSliceNodes = 0;
			t.summaryEdgesFound = hasSummaryEdges(sdg);
			if (!t.summaryEdgesFound) {
				print(" _no_summary_edges_found_ ");
			}
			
			final LineNrSlicer lns = new LineNrSlicer(sdg, t.extendedStats);
			final Result r = lns.heavySliceBackw();
			t.numOfLines = r.numberOfLines;
			t.avgLinesInSlice = r.avgLinesPerSlice;
			if (t.extendedStats) {
				r.printPerLineResult(out);
			}
			
//			final SummarySlicerBackward ssb = new SummarySlicerBackward(sdg);
//			for (final SDGNode n : sdg.vertexSet()) {
//				t.numOfTotalNodes++;
//				if (isCounting(n)) {
//					t.numOfRelevantNodes++;
//					
//					final Collection<SDGNode> slice = ssb.slice(n);
//					allSliceNodes += slice.size();
//					for (final SDGNode sn : slice) {
//						if (isCounting(sn)) {
//							allRelevantSliceNodes++;
//						}
//					}
//				}
//			}
//			
//			t.avgRelevantNodesInSlice = allRelevantSliceNodes / t.numOfRelevantNodes;
//			t.avgTotalNodesInSlice = allSliceNodes / t.numOfRelevantNodes;
		} catch (IOException e) {
			error(e);
			error = true;
		} catch (NullPointerException e) {
			error(e);
			error = true;
		}
		
		if (!error) {
			println(t.allPercentsStr());
		} else {
			println("ERROR");
		}
		
		if (log != null) {
			if (!error) {
				log.println(t.percentLinesStr());
				log.println(t.toString());
			} else {
				log.println("ERROR");
			}
			log.flush();
			log.close();
		}
		
		return !error;
	}
	
	private static boolean isCounting(final SDGNode node) {
		switch (node.getKind()) {
		case CALL:
		case EXPRESSION:
		case NORMAL:
		case PREDICATE:
			// do edu.kit.joana.deprecated.jsdg.slicing
			final String source = node.getSource();
			return (source != null); // && node.getBytecodeIndex() >= 0;
		default:
			// ignore rest of nodes;
			return false;
		}
	}

	public Task createTask(final String fileName) {
		final File f = new File(fileName);
		
		if (!f.exists() || !f.canRead()) {
			error("file not found or not readable '" + f.getAbsolutePath() + "'");
			return null;
		}

		return createTask(f);
	}
	
	public Task createTask(final File f) {
		if (!f.canRead() || f.isDirectory()) {
			error("cannot read file '" + f.getAbsolutePath() + "'");
			return null;
		}

		final Task t = new Task();
		t.filename = f.getAbsolutePath();
		t.logname = defaultLogFileName(t.filename);
		
		return t;
	}
	
	public static String defaultLogFileName(final String sdgFile) {
		return sdgFile + "-heavyslicing.log";
	}
	
	public void print(final String str) {
		out.print(str);
	}

	public void println(final String str) {
		out.println(str);
	}
	
	public void error(final String str) {
		out.println(str);
	}

	public void error(final Throwable t) {
		t.printStackTrace(out);
	}

}
