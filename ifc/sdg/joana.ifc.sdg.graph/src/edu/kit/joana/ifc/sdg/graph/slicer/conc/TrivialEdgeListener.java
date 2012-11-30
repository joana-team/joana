/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;

/**
 * This is the trivial implementation of an {@link EdgeListener} edge listener, which simply does nothing.
 * @author Martin Mohr
 *
 */
public class TrivialEdgeListener implements EdgeListener {

	@Override
	public void init() {
		// do nothing
	}

	@Override
	public void edgeEncountered(SDGEdge e) {
		// do nothing
	}



}
