package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.util.Triple;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DotGrapher {

		private static String outPath;
		static {
			outPath = System.getProperty("user.dir");
		}

		public static void configureDest(String path) {
			outPath = path;
		}

		public static void exportDotGraph(DotGraph g) {
			String graph = buildGraph(getNodes(g), getEdges(g), g.getName());

			// create file
			File f = new File(outPath + "/" + g.getName() + ".dot");
			try {
				f.createNewFile();
				FileWriter w = new FileWriter(f);
				w.write(graph);
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	private static List<Pair<Integer, String>> getNodes(DotGraph g) {
		List<Pair<Integer, String>> nodes = new ArrayList<>();
		g.getNodes().forEach(n -> nodes.add(Pair.make(n.getId(), n.getLabel())));
		return nodes;
	}

	private static List<Triple<Integer, Integer, Boolean>> getEdges(DotGraph g) {
		List<Triple<Integer, Integer, Boolean>> edges = new ArrayList<>();
		for (DotNode node : g.getNodes()) {
			node.getSuccs().forEach(s -> edges.add(Triple.triple(node.getId(), s.getId(), node.isExceptionEdge(s))));
		}
		return edges;
	}

	private static String buildGraph(List<Pair<Integer, String>> nodes, List<Triple<Integer, Integer, Boolean>> edges,
			String name) {
		StringBuilder sb = new StringBuilder(String.format("digraph %s {\n", name));

		for (Pair<Integer, String> node : nodes) {
			sb.append(node.fst).append(" [shape=box, label=\"").append(node.snd).append("\"];\n");
		}

		for (Triple<Integer, Integer, Boolean> edge : edges) {
			sb.append(edge.getLeft()).append(" -> ").append(edge.getMiddle())
					.append((edge.getRight() ? " [style=dashed, color=grey]" : "")).append(";\n");
		}
		sb.append("}");
		return sb.toString();
	}
}
