/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 27.10.2005
 * @author Kai Brueckner
 * University of Passau
 */
package edu.kit.joana.ui.ifc.sdg.viewer.model;

import java.io.IOException;


import org.eclipse.jdt.core.ICompilationUnit;

import edu.kit.joana.ifc.sdg.graph.SDG;

/** A utility class for computing SDGs.
 *
 */
public class GraphFactory {

	/** Loads a graph from harddrive and creates a Graph object.
	 *
	 * @param file      The Java source file for the Graph.
	 * @param filename  The filename of the SDG to load.
	 * @return          The created Graph object. Can return null if the
	 * 					SDG file is corrupt.
	 * @throws IOException
	 */
	public static Graph loadGraph(ICompilationUnit file, String filename) throws IOException {
		// Parse the SDG file.
		SDG sdg = SDG.readFrom(filename);

		return new Graph(sdg, file, filename);
	}
}

