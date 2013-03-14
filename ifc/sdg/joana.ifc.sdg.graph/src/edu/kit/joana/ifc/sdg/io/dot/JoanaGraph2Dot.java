/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.io.dot;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;

import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.io.IOFactory;

/**
 * Convenience class which provides the possibility to export JoanaGraphs to the dot format
 * @author Martin Mohr
 */
public final class JoanaGraph2Dot {
	
	/** prevent instantiation of this utility class */
	private JoanaGraph2Dot() {}

	/**
	 * Dotifies the given graph using the given name providers and writes the result to the given output stream.
	 * @param joanaGraph graph to export to dot format
	 * @param vIdProvider used to generate ids for each vertex in the given graph
	 * @param vNameProvider used to generate names for each vertex in the given graph
	 * @param edgeNameProvider used to generate names for each edge in the given graph
	 * @param out output stream to write the dotified version of the given graph to
	 */
	public static final <V,E> void writeDotToOutputStream(AbstractJoanaGraph<V,E> joanaGraph, OutputStream out) {
		Writer writer = IOFactory.createUTF8OutputStreamWriter(out);
		DOTExporter<V, E> exporter = new DOTExporter<V, E>(vIdProvider, vNameProvider, edgeNameProvider);
		exporter.export(writer, joanaGraph);
	}
	
	/**
	 * Convenience method for usage of {@link #writeDotToOutputStream(AbstractJoanaGraph, VertexNameProvider, VertexNameProvider, EdgeNameProvider, OutputStream) writeDotToOutputStream()} 
	 * for cases in which the graph is to be exported to a file. Relieves users from the burden to create a FileOutputStream for your filename.
	 * @param joanaGraph graph to export to dot format
	 * @param vIdProvider used to generate ids for each vertex in the given graph
	 * @param vNameProvider used to generate names for each vertex in the given graph
	 * @param edgeNameProvider used to generate names for each edge in the given graph
	 * @param fileName name of the file in which the graph is to be stored
	 * @throws IOException if the file exists but is a directory rather than a regular file, does not exist but cannot be created, or cannot be opened for any other reason
	 */
	public static final <V,E> void writeDotToFile(AbstractJoanaGraph<V,E> joanaGraph, VertexNameProvider<V> vIdProvider, VertexNameProvider<V> vNameProvider, EdgeNameProvider<E> edgeNameProvider, String fileName) throws IOException {
		writeDotToOutputStream(joanaGraph, vIdProvider, vNameProvider, edgeNameProvider, new FileOutputStream(fileName));
	}

}