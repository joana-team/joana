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

import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;


public class PathSlicerFactory {

	public PathSlicer createPathSlicer(String sdgFile, CFG graph) {
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
			e.printStackTrace();
			throw new IllegalArgumentException("Could not create VariableMapReader!");
		}
		return new PathSlicer(rdr, graph);
	}

	public static boolean checkSDG(String sdgFile) {
		File f = new File(sdgFile + "_def");
		if (!f.exists()) {
			return false;
		}
		f = new File(sdgFile + "_use");
		if (!f.exists()) {
			return false;
		}
		f = new File(sdgFile + "_mod");
		if (!f.exists()) {
			return false;
		}
		f = new File(sdgFile + "_ref");
		if (!f.exists()) {
			return false;
		}
		f = new File(sdgFile + "_pto");
		if (!f.exists()) {
			return false;
		}
		return true;
	}
}
