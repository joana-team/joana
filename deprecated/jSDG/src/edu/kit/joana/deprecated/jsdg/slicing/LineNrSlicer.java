/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.slicing;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;


import org.jgrapht.traverse.BreadthFirstIterator;

import edu.kit.joana.deprecated.jsdg.SDGFactory.Config;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerForward;

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

	private final String pdgFile;
	private SDG sdg;
	private Slicer slicerBack;
	private Slicer slicerForw;

	public LineNrSlicer(String pdgFile) {
		this.pdgFile = pdgFile;
	}

	public void readIn() throws IOException {
		sdg = SDG.readFrom(pdgFile);
		slicerBack = new SummarySlicerBackward(sdg);
		slicerForw = new SummarySlicerForward(sdg);
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

		Set<SDGNode> crit = getNodesForLine(line);
		Collection<SDGNode> result = slicerBack.slice(crit);

		return getLinesForNodes(result);
	}

	private Set<Line> getLinesForNodes(Collection<SDGNode> nodes) {
		TreeSet<Line> lines = new TreeSet<Line>();

		for (SDGNode node : nodes) {
			if (node.getSource() != null) {
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
				if (line.filename.equals(node.getSource()) && line.line == node.getSr()) {
//					System.out.println("Added " + node.getKind() + ": " + node.getLabel());
					nodes.add(node);
				}
			}
		}

		if (nodes == null || nodes.isEmpty()) {
			throw new IllegalStateException("Lines not found in graph: " + lines);
		}

		Set<SDGNode> actInOut = new HashSet<SDGNode>();
		for (SDGNode node : nodes) {
			if (node.getKind() == Kind.CALL) {
				// collect act-in-out nodes
				addActInOutSuccs(node, actInOut);
			}
		}

		for (SDGNode node : actInOut) {
			nodes.add(node);
		}

		return nodes;
	}

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

	private static final Map<Line, Integer> inSlice = new HashMap<Line, Integer>();

	private static void countLine(Line node) {
		Integer num = inSlice.get(node);
		if (num == null) {
			num = new Integer(0);
		}
		num++;
		inSlice.put(node, num);
	}

	private static void countLines(Set<Line> lines) {
		for (Line line : lines) {
			countLine(line);
		}
	}

	public void heavySliceBackw() {
		if (sdg == null) {
			throw new IllegalStateException("Run readIn first to load sdg from file.");
		}

		Set<Line> lines = new TreeSet<Line>();
		BreadthFirstIterator<SDGNode, SDGEdge> it =
			new BreadthFirstIterator<SDGNode, SDGEdge>(sdg);
		while (it.hasNext()) {
			SDGNode node = it.next();
			if (node.getSource() != null && node.getSr() >= 0) {
				Line line = new Line(node.getSource(), node.getSr());
				lines.add(line);
			}
		}

		System.out.println(lines.size() + " different sourcecode lines found.");
		long sliceTotal = 0;
		for (Line l : lines) {
			Set<Line> slice = sliceBackward(l);
			countLines(slice);
			int size = slice.size();
			sliceTotal += size;
			System.out.println(l + ": " + size);
		}

		System.out.println("Total lines in all slices: " + sliceTotal);
		System.out.println("Total num of slices: " + lines.size());
		double linesPerSlice = (double) ((double) sliceTotal / (double) lines.size());
		double percent = (double) ((double) linesPerSlice / (double) lines.size()) * 100.0;
		System.out.println("Avg. lines per slice: " + linesPerSlice + "(" + percent + "%)");
	}

	public static void writeStatistics(String outDir, String sdgFile) throws FileNotFoundException {
		String sliceNodesFile = outDir + getClassName(sdgFile) + ".sliceLines.csv";

		SortedSet<Map.Entry<Line, Integer>> sorted =
			new TreeSet<Map.Entry<Line,Integer>>(new Comparator<Map.Entry<Line,Integer>>() {

				public int compare(Entry<Line, Integer> o1, Entry<Line, Integer> o2){
//					return o1.getValue().compareTo(o2.getValue());
					int comp = o2.getValue().compareTo(o1.getValue());
					if (comp == 0) {
						return ((o1.hashCode() - o2.hashCode()) > 0 ? 1 : -1);
					} else {
						return comp;
					}
				}
			}
		);

		for (Map.Entry<Line, Integer> entry : inSlice.entrySet()) {
			sorted.add(entry);
		}

		PrintWriter pw = new PrintWriter(sliceNodesFile);
		int count = 0;
		pw.println("No.,Count,Id,Source,Line");
		for (Map.Entry<Line, Integer> entry : sorted) {
			Line line = entry.getKey();
			pw.println((count + 1) + "," + entry.getValue() +
				"," + line.toString() + "," + line.filename + "," + line.line);
			count++;
		}
		pw.flush();
		pw.close();

		System.out.println("Node statistics written to " + sliceNodesFile);
	}

	/**
	 * @param args
	 * @throws ANTLRException
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String sdgFile = null;
		String outDir = "";
		String configFile = null;

		for (int i = 1; i < args.length; i++) {
			String arg = args[i].toLowerCase();

			if ("-sdg".equals(arg)) {
				if (i+1 < args.length) {
					i++;
					sdgFile = args[i];
				} else {
					System.out.println("Missing sdg file name.");
				}
			} else if ("-out".equals(arg)) {
				if (i+1 < args.length) {
					i++;
					outDir = args[i];
				} else {
					System.out.println("Missing output directory name.");
				}
			} else if ("-cfg".equals(arg)) {
				if (i+1 < args.length) {
					i++;
					configFile = args[i];
				} else {
					System.out.println("Missing config file name.");
				}
			}
		}

		if (configFile != null) {
			System.out.println("Reading configuration from " + configFile);
			FileInputStream fIn = new FileInputStream(configFile);
			Config cfg = Config.readFrom(fIn);
			sdgFile = cfg.outputSDGfile;
			outDir = cfg.outputDir;
		}

		if (sdgFile != null && outDir != null) {
			if (outDir.length() > 0 && !outDir.endsWith("/")) {
				outDir += "/";
			}

			LineNrSlicer slicer = new LineNrSlicer(sdgFile);
			slicer.readIn();
			slicer.heavySliceBackw();

			writeStatistics(outDir, sdgFile);
		} else {
			System.out.println("No sdg file specified. Use -sdg option to specify the sdg file.");
		}
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
