/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jgrapht.traverse.BreadthFirstIterator;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;

public class LineNrSlicer {

	public static class Line implements Comparable<Line> {
		public final String filename;
		public final int line;

		public Line(String filename, int line) {
			this.filename = filename;
			this.line = line;
		}

		public String toString() {
			return filename + ":" + line;
		}

		public String getRealLine(String base) throws IOException {
			String real = "<not found>";

			BufferedReader bIn = new BufferedReader(new FileReader(base + filename));
			for (int i = 1; i < line && bIn.ready(); i++) {
				bIn.readLine();
			}

			if (bIn.ready()) {
				real = bIn.readLine();
			}
			bIn.close();

			return real;
		}

		public boolean equals(Object obj) {
			if (obj instanceof Line) {
				Line l = (Line) obj;
				return filename.equals(l.filename) && line == l.line;
			}
			return false;
		}

		public int hashCode() {
			return filename.hashCode() * line;
		}

		public int compareTo(Line l) {
			int cmp = filename.compareTo(l.filename);
			if (cmp == 0) {
				cmp = line - l.line;
			}

			return cmp;
		}
	}

	private SDG sdg;
	private Slicer slicerBack;
	private Slicer slicerForw;
	private final boolean extendedStats;
	
	private Map<Line,Set<SDGNode>> line2nodes;
	private SortedSet<Line> lines;

	public LineNrSlicer(final SDG sdg) {
		this(sdg, false);
	}
	
	public LineNrSlicer(final SDG sdg, final boolean extendedStats) {
		this.sdg = sdg;
		this.extendedStats = extendedStats;
		this.slicerBack = new SummarySlicerBackward(sdg);
		this.slicerForw = new SummarySlicerBackward(sdg);
		createLine2Nodes();
	}

	public SortedSet<Line> getLines() {
		return Collections.unmodifiableSortedSet(lines);
	}
	
//	private void fixupSDG() {
//		for (final SDGNode n : sdg.vertexSet()) {
//			if (n.kind == SDGNode.Kind.CALL) {
//				
//			}
//		}
//	}
	
	private void createLine2Nodes() {
		line2nodes = new HashMap<>();
		lines = new TreeSet<Line>();

		final BreadthFirstIterator<SDGNode, SDGEdge> it = new BreadthFirstIterator<SDGNode, SDGEdge>(sdg);
		while (it.hasNext()) {
			final SDGNode node = it.next();
			if (isRelevantLine(node)) {
				Line line = new Line(node.getSource(), node.getSr());
				lines.add(line);
				Set<SDGNode> nodes = line2nodes.get(line);
				if (nodes == null) {
					nodes = new HashSet<>();
					line2nodes.put(line, nodes);
				}
				nodes.add(node);
			}
		}

//		// enrich lines2nodes with missing actInOuts
//		for (final Line l : lines) {
//			final Set<SDGNode> nodes = line2nodes.get(l);
//			if (nodes != null) {
//				final Set<SDGNode> toAdd = new HashSet<>();
//				for (final SDGNode n : nodes) {
//					if (n.getKind() == Kind.CALL) {
//						// collect act-in-out nodes
//						addActInOutSuccs(n, toAdd);
//					}
//				}
//				nodes.addAll(toAdd);
//			}
//		}
	}
	
	public Set<Line> sliceForward(Line line) {
		if (sdg == null) {
			throw new IllegalStateException("Run readIn first to load sdg from file.");
		}

		Set<Line> lines = new HashSet<Line>();
		lines.add(line);
		return sliceForward(lines);
	}

	public Set<Line> sliceForward(Set<Line> line) {
		if (sdg == null) {
			throw new IllegalStateException("Run readIn first to load sdg from file.");
		}

		Set<SDGNode> crit = getNodesForLine(line);
		Collection<SDGNode> result = slicerForw.slice(crit);

		return getLinesForNodes(result);
	}

	public Set<Line> sliceBackward(Line line) {
		if (sdg == null) {
			throw new IllegalStateException("Run readIn first to load sdg from file.");
		}

		Set<Line> lines = new HashSet<Line>();
		lines.add(line);
		return sliceBackward(lines);
	}

	public Set<Line> sliceBackward(Set<Line> line) {
		if (sdg == null) {
			throw new IllegalStateException("Run readIn first to load sdg from file.");
		}

		Set<SDGNode> crit = new HashSet<>();
		for (Line l : line) {
			Set<SDGNode> nodes = line2nodes.get(l);
			if (nodes != null) {
				crit.addAll(nodes);
			}
		}
		Collection<SDGNode> result = slicerBack.slice(crit);

		return getLinesForNodes(result);
	}

	private Set<Line> getLinesForNodes(Collection<SDGNode> nodes) {
		TreeSet<Line> lines = new TreeSet<Line>();

		for (SDGNode node : nodes) {
			if (isRelevantLine(node)) {
				Line line = new Line(node.getSource(), node.getSr());
				lines.add(line);
			}
		}

		return lines;
	}

	private Set<SDGNode> getNodesForLine(Set<Line> lines) {
		BreadthFirstIterator<SDGNode, SDGEdge> it =
			new BreadthFirstIterator<SDGNode, SDGEdge>(sdg);
		Set<SDGNode> nodes = new HashSet<SDGNode>();

		while (it.hasNext()) {
			SDGNode node = it.next();
			for (Line line : lines) {
				if (line.filename.equals(node.getSource()) && line.line == node.getSr() && isRelevantLine(node)) {
//					System.out.println("Added " + node.getKind() + ": " + node.getLabel());
					nodes.add(node);
				}
			}
		}

		if (nodes == null || nodes.isEmpty()) {
			throw new IllegalStateException("Lines not found in graph: " + lines);
		}

//		Set<SDGNode> actInOut = new HashSet<SDGNode>();
//		for (SDGNode node : nodes) {
//			if (node.getKind() == Kind.CALL) {
//				// collect act-in-out nodes
//				addActInOutSuccs(node, actInOut);
//			}
//		}
//
//		for (SDGNode node : actInOut) {
//			nodes.add(node);
//		}

		return nodes;
	}

	@SuppressWarnings("unused")
	private final void addActInOutSuccs(SDGNode call, Set<SDGNode> actInOut) {
		actInOut.add(call);
		Set<SDGEdge> out = sdg.outgoingEdgesOf(call);
		for (SDGEdge edge : out) {
			SDGNode node = edge.getTarget();
			if (node.getKind() == Kind.ACTUAL_IN || node.getKind() == Kind.ACTUAL_OUT) {
				if (!actInOut.contains(node)) {
					addActInOutSuccs(node, actInOut);
				}
			}
		}
	}

	public static class Result {
		public int numberOfLines;
		public long avgLinesPerSlice;
		public double percentInSlice;
		public SortedSet<LineInSlice> perLineResults; // only present if LineNrSlicer started with extendedStats enabled
		
		public String toString() {
			return avgLinesPerSlice + " of " + numberOfLines + " in average slice. (" + percentInSlice + "%)";
		}
		
		public void printPerLineResult(final PrintStream out) {
			if (perLineResults == null) {
				out.println("no per line results available.");
			} else {
				out.println("slicing stats per line:");
				for (final LineInSlice lis : perLineResults) {
					out.println(lis);
				}
			}
		}
	}
	
	public static class LineInSlice implements Comparable<LineInSlice> {
		public int count;
		public final Line line;
		
		public LineInSlice(final Line line) {
			this.line = line;
		}
		
		public int hashCode() {
			return line.hashCode() * 4177;
		}
		
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			
			if (o instanceof LineInSlice) {
				LineInSlice lis = (LineInSlice) o;
				return line.equals(lis.line);
			}
			
			return false;
		}
		
		public int compareTo(LineInSlice other) {
			int diff = count - other.count;
			
			if (diff != 0) {
				return diff;
			}
			
			return line.compareTo(other.line);
		}
		
		public String toString() {
			return count + "\t" + line;
		}
			
	}
	
	private static boolean isExcluded(final String str) {
		return str.contains("java/lang") || str.contains("java/io") || str.contains("java/util")
				|| str.contains("com/ibm/wala") || str.contains("sun/reflect") || str.contains("java/security")
				|| str.contains("sun/") ;
	}
	
	private static boolean isRelevantLine(final SDGNode node) {
		return node.getKind() == SDGNode.Kind.EXPRESSION && !node.getLabel().startsWith("CONST") && !node.getLabel().contains("= catch ")
				&& node.getSource() != null && node.getSr() > 0 && !isExcluded(node.getSource());
	}
	
	public Result heavySliceBackw() {
		final Result r = new Result();
		final Map<Line,LineInSlice> line2lis = (extendedStats ? new HashMap<Line, LineInSlice>() : null);

		if (extendedStats) {
			for (final Line l : lines) {
				LineInSlice lis = new LineInSlice(l);
				line2lis.put(l, lis);
			}
		}
		
		r.numberOfLines = lines.size();
		//System.out.println(lines.size() + " different sourcecode lines found.");
		long sliceTotal = 0;
		final LinkedList<Line> worklist = new LinkedList<>();
		worklist.addAll(lines);
		Collections.shuffle(worklist);
		for (final Line l : worklist) {
			final Set<Line> slice = sliceBackward(l);
			if (extendedStats) {
				for (Line sliceline : slice) {
					LineInSlice lis = line2lis.get(sliceline);
					lis.count++;
				}
			}
			sliceTotal += slice.size();
			//System.out.println(l + ": " + size);
		}

//		System.out.println("Total lines in all slices: " + sliceTotal);
//		System.out.println("Total num of slices: " + lines.size());
		r.avgLinesPerSlice = sliceTotal / r.numberOfLines;
		r.percentInSlice = (100.0 * r.avgLinesPerSlice) / ((double) r.numberOfLines);
		if (extendedStats) {
			final SortedSet<LineInSlice> sortedLines = new TreeSet<>();
			sortedLines.addAll(line2lis.values());
			r.perLineResults = sortedLines;
		}
//		System.out.println("Avg. lines per slice: " + linesPerSlice + "(" + percent + "%)");
		return r;
	}

	public static String getClassName(String file) {
		String result;

		int indexOfSlash = file.lastIndexOf('/');
		if (indexOfSlash > 0) {
			result = file.substring(indexOfSlash + 1);
		} else {
			result = file;
		}

		result = result.substring(0, result.lastIndexOf('.'));

		return result;
	}

}
