/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.io.dot;

import org.jgrapht.ext.EdgeNameProvider;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;


public class SDGEdgeNameProvider implements EdgeNameProvider<SDGEdge> {

	@Override
	public String getEdgeName(SDGEdge arg0) {
		return arg0.getKind().toString();
	}


}
