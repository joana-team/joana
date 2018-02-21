/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.util;

import java.util.HashSet;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


public final class SDGUtils {

	private SDGUtils() {}

	public static SDG callGraph(SDG sdg) {
		SDG callGraph;
		String name = sdg.getName();
		if (name == null) {
			callGraph = new SDG("<unnamed>");
		} else {
			// get class name from a String like "my.lib.Class.foo(lib.A, int)"
			// -> "my.lib.Class"
			// search last '.' before '('
			final int posBR = name.indexOf('(');
			int posPT = 0;
			for (int curr = 0; curr >= 0 && curr < posBR; ) {
				posPT = curr;
				curr = name.indexOf('.', curr + 1);
			}
			name = name.substring(0, posPT);
			callGraph = new SDG(name);
		}
		int size = 1;

		// now collect entry nodes and sort them in order of their proc-IDs
		LinkedList<SDGNode> unsorted = new LinkedList<SDGNode>();

		for (SDGNode currentVertex : sdg.vertexSet()) {
			if (currentVertex.getKind() == SDGNode.Kind.ENTRY) {
				unsorted.add(currentVertex);
				if (currentVertex.getProc() > size) {
					size = currentVertex.getProc();
				}
			}
		}

		SDGNode[] entries = new SDGNode[size+1];

		// now we iterate the graph to filter out all entry nodes
		for (SDGNode currentVertex : unsorted) {
			int proc = currentVertex.getProc();
			entries[proc] = currentVertex;
			callGraph.addVertex(currentVertex);
		}

		// iterate the graph again and filter out all call nodes
		for (SDGNode currentVertex : sdg.vertexSet()) {
			if (currentVertex.getKind() == SDGNode.Kind.CALL) {
				// iterate incident edges
				for (SDGEdge e : sdg.outgoingEdgesOf(currentVertex)) {
					SDGNode targetVertex = e.getTarget();
					// if an edge leads to an entry node add it to the
					// filtered graph, with its source not the call node
					// but the entry node of the call node's method
					if (targetVertex.getKind() == SDGNode.Kind.ENTRY) {
						int proc = currentVertex.getProc();
						callGraph.addEdge( SDGEdge.Kind.CALL.newEdge(entries[proc], targetVertex));
					}
				}
			}
		}
		
		// We expect sdg to have a root that is an ENTRY node
		if (sdg.getRoot() == null) {
			callGraph.setRoot(sdg.guessRoot());
		} else {
			callGraph.setRoot(sdg.getRoot());
		}

		return callGraph;
	}

	/* knoten im call graph ausblenden */
	/**
	 * Parses the file to construct an SDG graph instance. This graph is
	 * iterated and ENTRY nodes and their relations are filtered out. Finally
	 * the graph and the filtered graph are included in the GraphViewerModel
	 * instance.
	 *
	 * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.command.ExecutableCommand#execute()
	 */
	public static SDG truncatedCallGraph(SDG callGraph, String regexp) {
		SDG truncatedCallGraph = new SDG(callGraph.getName());
		SDGNode entry = callGraph.getRoot();
		truncatedCallGraph.setRoot(entry);
		LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
		HashSet<SDGNode> marked = new HashSet<SDGNode>();

		truncatedCallGraph.addVertex(entry);
		marked.add(entry);
		wl.add(entry);

		while(!wl.isEmpty()) {
			SDGNode next = wl.poll();

			for (SDGEdge e : callGraph.outgoingEdgesOf(next)) {
				SDGNode target = e.getTarget();

				if (target.getLabel().matches(regexp)) continue;

				if (marked.add(target)) {
					wl.add(target);
					truncatedCallGraph.addVertex(target);
				}

				truncatedCallGraph.addEdge(e);
			}
		}

		return truncatedCallGraph;
	}
}
