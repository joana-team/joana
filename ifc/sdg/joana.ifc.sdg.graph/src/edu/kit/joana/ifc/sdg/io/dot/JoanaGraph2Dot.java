/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.io.dot;

import java.io.IOException;
import java.io.OutputStream;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;
import edu.kit.joana.util.graph.io.dot.AbstractJoanaGraph2Dot;

/**
 * Convenience class which provides the possibility to export JoanaGraphs to the dot format. Simplifies the usage of {@link edu.kit.joana.util.graph.io.dot.AbstractJoanaGraph2Dot} by supplying
 * standard vertex and edge name providers.
 * @author Martin Mohr
 * @see SDGNodeIdProvider
 * @see SDGNodeNameProvider
 * @see SDGEdgeNameProvider
 */
public final class JoanaGraph2Dot {
	
	/** prevent instantiation of this utility class */
	private JoanaGraph2Dot() {}

	/**
	 * Dotifies the given graph using standard name providers and writes the result to the given output stream.
	 * @param joanaGraph graph to export to dot format
	 * @param out output stream to write the dotified version of the given graph to
	 */
	public static final void writeDotToOutputStream(JoanaGraph joanaGraph, OutputStream out) {
		AbstractJoanaGraph2Dot.writeDotToOutputStream(joanaGraph, SDGNodeIdProvider.getInstance(), SDGNodeNameProvider.getInstance(), SDGEdgeNameProvider.getInstance(), out);
	}
	
	/**
	 * Dotifies the given graph using standard name providers and writes the result to the given file.
	 * @param joanaGraph graph to export to dot format
	 * @param fileName name of file to write dotified version of the given graph to
	 */
	public static final <V extends IntegerIdentifiable,E extends KnowsVertices<V>> void writeDotToFile(JoanaGraph joanaGraph, String fileName) throws IOException {
		AbstractJoanaGraph2Dot.writeDotToFile(joanaGraph, SDGNodeIdProvider.getInstance(), SDGNodeNameProvider.getInstance(), SDGEdgeNameProvider.getInstance(), fileName);
	}

}