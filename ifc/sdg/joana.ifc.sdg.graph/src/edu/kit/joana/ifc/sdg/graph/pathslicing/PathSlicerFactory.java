/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.pathslicing;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;


public final class PathSlicerFactory {

	private PathSlicerFactory() {}

	private static final String[] suffixes = {"_def", "_use", "_mod", "_ref", "_pto"};

	public static PathSlicer createPathSlicer(String sdgFile, CFG graph) {
		if (!checkSDG(sdgFile)) {
			throw new IllegalArgumentException("SDG files not correct!");
		}
		if (graph.getRoot() == null) {
			throw new IllegalArgumentException("CFG needs a root!");
		}
		VariableMapReader rdr;
		try {
			rdr = new VariableMapReader(sdgFile);
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not create VariableMapReader!", e);
		}
		return new PathSlicer(rdr, graph);
	}

	public static boolean checkSDG(String sdgFile) {
		return Arrays.stream(suffixes).allMatch(s -> new File(sdgFile + s).exists());
	}
}
