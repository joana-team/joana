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

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
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
		int numOfLines;
		int numOfRelevantNodes;
		int numOfTotalNodes;
		long avgRelevantNodesInSlice;
		long avgTotalNodesInSlice;
		long avgLinesInSlice;

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
			return percentLinesStr() + " (" + percentRelevantStr() + " - " + percentTotalStr() + ")";
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
			return 
				"nodes   : " + percentTotalStr() + " (" + avgTotalNodesInSlice + " of " + numOfTotalNodes + ")\n"
			+	"relevant: " + percentRelevantStr() + " (" + avgRelevantNodesInSlice + " of " + numOfRelevantNodes + ")\n"
			+	"lines   : " + percentLinesStr() + " (" + avgLinesInSlice + " of " + numOfLines + ")";
		}
	}
	
	public HeavySlicer(final PrintStream out) {
		this.out = out;
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

			long allSliceNodes = 0;
			long allRelevantSliceNodes = 0;
			
			final LineNrSlicer lns = new LineNrSlicer(sdg);
			final Result r = lns.heavySliceBackw();
			t.numOfLines = r.numberOfLines;
			t.avgLinesInSlice = r.avgLinesPerSlice;
			
			final SummarySlicerBackward ssb = new SummarySlicerBackward(sdg);
			for (final SDGNode n : sdg.vertexSet()) {
				t.numOfTotalNodes++;
				if (isCounting(n)) {
					t.numOfRelevantNodes++;
					
					final Collection<SDGNode> slice = ssb.slice(n);
					allSliceNodes += slice.size();
					for (final SDGNode sn : slice) {
						if (isCounting(sn)) {
							allRelevantSliceNodes++;
						}
					}
				}
			}
			
			t.avgRelevantNodesInSlice = allRelevantSliceNodes / t.numOfRelevantNodes;
			t.avgTotalNodesInSlice = allSliceNodes / t.numOfRelevantNodes;
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
		t.logname = t.filename + "-heavyslicing.log";
		
		return t;
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
