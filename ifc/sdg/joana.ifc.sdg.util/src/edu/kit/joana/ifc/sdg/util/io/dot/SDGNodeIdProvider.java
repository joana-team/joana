/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.io.dot;

import org.jgrapht.ext.VertexNameProvider;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


public class SDGNodeIdProvider implements VertexNameProvider<SDGNode> {

	@Override
	public String getVertexName(SDGNode arg0) {
		return arg0.getId() + "";
	}

}
