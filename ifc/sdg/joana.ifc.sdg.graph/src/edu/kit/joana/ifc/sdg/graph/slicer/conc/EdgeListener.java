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
 * This interface adds the ability to do additional things for each edge encountered during a slice. The purpose is,
 * for example, to collect edges of a certain kind encountered during a slice.
 * @author Martin Mohr
 *
 */
public interface EdgeListener {

	/**
	 * This method is called before slicing begins.
	 */
	public void init();

	/**
	 * This method is called for each edge which has been encountered during a slice.
	 * @param e edge which has been encountered during a slice.
	 */
	public void edgeEncountered(SDGEdge e);
}
