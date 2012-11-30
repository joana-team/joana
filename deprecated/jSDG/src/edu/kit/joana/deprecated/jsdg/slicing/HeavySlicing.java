/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.slicing;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jgrapht.traverse.BreadthFirstIterator;

import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.IncrementalSummaryBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;

public class HeavySlicing {

	private static boolean isCounting(SDGNode node) {
		switch (node.getKind()) {
		case CALL:
		case EXPRESSION:
		case NORMAL:
		case PREDICATE:
			// do edu.kit.joana.deprecated.jsdg.slicing
			String source = node.getSource();
			return (source != null); // && node.getBytecodeIndex() >= 0;
		default:
			// ignore rest of nodes;
			return false;
		}
	}


	private static final Map<SDGNode, Integer> inSlice = new HashMap<SDGNode, Integer>();

	private static void countNode(SDGNode node) {
		Integer num = inSlice.get(node);
		if (num == null) {
			num = new Integer(0);
		}
		num++;
		inSlice.put(node, num);
	}

	private static int countNodes(Collection<SDGNode> slice) {
		int result = 0;

		for (SDGNode node : slice) {
			if (isCounting(node)) {
				countNode(node);
				result++;
			}
		}

		return result;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
//		String sdgFile = "../Barcode/jSDG/output/j2me.Barcode.pdg";
//		String sdgFile = "../benchmarks/jSDG/output/JGFBarrierBench.pts_graph.pdg";
		String sdgFile = "/Users/jgf/Documents/workspaces/CryptoJavaIFC/de.uni.trier.infsec.protocols.simplevoting.Setup-main.ifc.pdg";
//		String sdgFile = null;
//		String sdgFile = "../benchmarks/jSDG/output/JGFForkJoinBench.pts_graph.pdg";
//		String sdgFile = "../benchmarks/jSDG/output/JGFCryptBenchSizeA.pts_graph.pdg";
//		String sdgFile = "../benchmarks/jSDG/output/JGFCryptBenchSizeA.jsdg.pdg";
//		String sdgFile = "../javaSDG/JGFBarrierBench.pdg";
//		String sdgFile = "../javaSDG/def.JGFCryptBenchSizeA.pdg";
		String outDir = "";

		for (int i = 1; i < args.length; i++) {
			if ("-sdg".equals(args[i].toLowerCase())) {
				if (i+1 < args.length) {
					i++;
					sdgFile = args[i];
				} else {
					System.out.println("Missing sdg file name.");
				}
			} else if ("-out".equals(args[i].toLowerCase())) {
				if (i+1 < args.length) {
					i++;
					outDir = args[i];
				} else {
					System.out.println("Missing output directory name.");
				}
			}
		}

		if (sdgFile != null && outDir != null) {
			if (outDir.length() > 0 && !outDir.endsWith("/")) {
				outDir += "/";
			}

			heavySlice(sdgFile, outDir);
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

	private static final boolean VERBOSE = false;

	public static void heavySliceIncremental(String sdgFile, String outDir) throws IOException {
		String logFile = outDir + getClassName(sdgFile) + ".IncrHeavySlicing.log";
		String sliceNodesFile = outDir + getClassName(sdgFile) + ".IncrSliceNodes.csv";
		Log.setLogFile(logFile);
		System.out.println("Setting logfile to " + logFile);

		System.out.println("Loading SDG from " + sdgFile);
		Log.info("Loading SDG from " + sdgFile);

		SDG sdg = SDG.readFrom(sdgFile);
		IncrementalSummaryBackward slicerBack = new IncrementalSummaryBackward(sdg);
		int numberOfCountingNodes = 0;
		long totalBackSlicedNodes = 0;

		for (Iterator<SDGNode> it = new BreadthFirstIterator<SDGNode, SDGEdge>(sdg);it.hasNext();) {
			SDGNode node = it.next();
			if (isCounting(node)) {
				numberOfCountingNodes++;
				if (VERBOSE) {
					System.out.print("Backward edu.kit.joana.deprecated.jsdg.slicing " + node.getLabel());
					Log.info("Backward edu.kit.joana.deprecated.jsdg.slicing " + node.getLabel());
				}
				Set<SDGNode> criterion = new HashSet<SDGNode>();
				criterion.add(node);
				Collection<SDGNode> sliceBack = slicerBack.slice(criterion);
				int numBackNodes = countNodes(sliceBack);
				if (VERBOSE) {
					System.out.println(": " + numBackNodes + " nodes");
					Log.info("\t" + numBackNodes + " nodes");
				}
				totalBackSlicedNodes += numBackNodes;
			}
		}

		SortedSet<Map.Entry<SDGNode, Integer>> sorted =
			new TreeSet<Map.Entry<SDGNode,Integer>>(new Comparator<Map.Entry<SDGNode,Integer>>() {

				public int compare(Entry<SDGNode, Integer> o1, Entry<SDGNode, Integer> o2){
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

		for (Map.Entry<SDGNode, Integer> entry : inSlice.entrySet()) {
			sorted.add(entry);
		}

		PrintWriter pw = new PrintWriter(sliceNodesFile);
		int count = 0;
		pw.println("No.,Count,Id,Label,Source,Line");
		for (Map.Entry<SDGNode, Integer> entry : sorted) {
			pw.println((count + 1) + "," + entry.getValue() +
				"," + entry.getKey().getId() + ",\"" + entry.getKey().getLabel() +
				"\"," + entry.getKey().getSource() + "," + entry.getKey().getSr());
			count++;
		}
		pw.flush();
		pw.close();

		System.out.println("Total number of nodes: " + numberOfCountingNodes);
		System.out.println("Number of nodes in backward slices: " + totalBackSlicedNodes);
		System.out.println("Number of nodes in backward slices per slice: " + ((double)totalBackSlicedNodes) / ((double)numberOfCountingNodes));
		Log.info("Total number of nodes: " + numberOfCountingNodes);
		Log.info("Number of nodes in backward slices: " + totalBackSlicedNodes);
		Log.info("Number of nodes in backward slices per slice: " + ((double)totalBackSlicedNodes) / ((double)numberOfCountingNodes));
	}

	public static void heavySlice(String sdgFile, String outDir) throws IOException {
		String logFile = outDir + getClassName(sdgFile) + ".heavySlicing.log";
		String sliceNodesFile = outDir + getClassName(sdgFile) + ".sliceNodes.csv";
		Log.setLogFile(logFile);
		System.out.println("Setting logfile to " + logFile);

		System.out.println("Loading SDG from " + sdgFile);
		Log.info("Loading SDG from " + sdgFile);

		SDG sdg = SDG.readFrom(sdgFile);
		SummarySlicerBackward slicerBack = new SummarySlicerBackward(sdg);
//		SummarySlicerForward slicerForw = new SummarySlicerForward(sdg);

		int numberOfCountingNodes = 0;
		long totalBackSlicedNodes = 0;
		long totalForwSlicedNodes = 0;
		for (Iterator<SDGNode> it = new BreadthFirstIterator<SDGNode, SDGEdge>(sdg);it.hasNext();) {
			SDGNode node = it.next();
			if (isCounting(node)) {
				numberOfCountingNodes++;
				if (VERBOSE) {
					System.out.print("Backward edu.kit.joana.deprecated.jsdg.slicing " + node.getLabel());
					Log.info("Backward edu.kit.joana.deprecated.jsdg.slicing " + node.getLabel());
				}
				Set<SDGNode> criterion = new HashSet<SDGNode>();
				criterion.add(node);
				Collection<SDGNode> sliceBack = slicerBack.slice(criterion);
				int numBackNodes = countNodes(sliceBack);
				if (VERBOSE) {
					System.out.println(": " + numBackNodes + " nodes");
					Log.info("\t" + numBackNodes + " nodes");
				}
				totalBackSlicedNodes += numBackNodes;

//				if (VERBOSE) {
//					System.out.print("Forward edu.kit.joana.deprecated.jsdg.slicing " + node.getLabel());
//					Log.info("Forward edu.kit.joana.deprecated.jsdg.slicing " + node.getLabel());
//				}
//				Collection<SDGNode> sliceForw = slicerForw.slice(criterion);
//				int numForwNodes = countNodes(sliceForw);
//				if (VERBOSE) {
//					System.out.println(": " + numForwNodes + " nodes");
//					Log.info("\t" + numForwNodes + " nodes");
//				}
//				totalForwSlicedNodes += numForwNodes;
			}
		}

		SortedSet<Map.Entry<SDGNode, Integer>> sorted =
			new TreeSet<Map.Entry<SDGNode,Integer>>(new Comparator<Map.Entry<SDGNode,Integer>>() {

				public int compare(Entry<SDGNode, Integer> o1, Entry<SDGNode, Integer> o2){
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

		for (Map.Entry<SDGNode, Integer> entry : inSlice.entrySet()) {
			sorted.add(entry);
		}

		PrintWriter pw = new PrintWriter(sliceNodesFile);
		int count = 0;
		pw.println("No.,Count,Id,Label,Source,Line");
		for (Map.Entry<SDGNode, Integer> entry : sorted) {
			pw.println((count + 1) + "," + entry.getValue() +
				"," + entry.getKey().getId() + ",\"" + entry.getKey().getLabel() +
				"\"," + entry.getKey().getSource() + "," + entry.getKey().getSr());
			count++;
		}
		pw.flush();
		pw.close();

		System.out.println("Total number of nodes: " + numberOfCountingNodes);
		System.out.println("Number of nodes in backward slices: " + totalBackSlicedNodes);
//		System.out.println("Number of nodes in forward slices: " + totalForwSlicedNodes);
		System.out.println("Number of nodes in backward slices per slice: " + ((double)totalBackSlicedNodes) / ((double)numberOfCountingNodes));
//		System.out.println("Number of nodes in forward slices per slice: " + ((double)totalForwSlicedNodes) / ((double)numberOfCountingNodes));
		Log.info("Total number of nodes: " + numberOfCountingNodes);
		Log.info("Number of nodes in backward slices: " + totalBackSlicedNodes);
//		Log.info("Number of nodes in forward slices: " + totalForwSlicedNodes);
		Log.info("Number of nodes in backward slices per slice: " + ((double)totalBackSlicedNodes) / ((double)numberOfCountingNodes));
//		Log.info("Number of nodes in forward slices per slice: " + ((double)totalForwSlicedNodes) / ((double)numberOfCountingNodes));
	}

}
