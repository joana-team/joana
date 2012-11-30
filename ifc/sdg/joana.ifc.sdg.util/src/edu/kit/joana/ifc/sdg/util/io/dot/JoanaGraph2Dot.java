/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.io.dot;
import java.io.FileWriter;
import java.io.IOException;


import org.jgrapht.ext.DOTExporter;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.util.sdg.ReducedCFGBuilder;


public final class JoanaGraph2Dot {

	private static final SDGNodeIdProvider nodeIdProvider = new SDGNodeIdProvider();
	private static final SDGNodeNameProvider nodeNameProvider = new SDGNodeNameProvider();
	private static final SDGEdgeNameProvider edgeNameProvider = new SDGEdgeNameProvider();

	private JoanaGraph2Dot() {}

	public static final void dotifySDG(SDG sdg, String fileName) throws IOException {
		dotifyJoanaGraph(sdg, fileName);
	}

	public static final void dotifyCFG(SDG sdg, String fileName) throws IOException {
		dotifyJoanaGraph(ICFGBuilder.extractICFG(sdg), fileName);
	}

	public static final void dotifyReducedCFG(SDG sdg, String fileName) throws IOException {
		dotifyJoanaGraph(ReducedCFGBuilder.extractReducedCFG(sdg), fileName);
	}

	public static final void dotifyJoanaGraph(JoanaGraph joanaGraph, String fileName) throws IOException {
		FileWriter writer = new FileWriter(fileName);
		DOTExporter<SDGNode, SDGEdge> exporter = new DOTExporter<SDGNode, SDGEdge>(nodeIdProvider, nodeNameProvider, edgeNameProvider);
		exporter.export(writer, joanaGraph);
	}

}
