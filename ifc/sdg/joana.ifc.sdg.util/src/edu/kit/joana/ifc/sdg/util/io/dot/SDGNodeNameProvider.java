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

public class SDGNodeNameProvider implements VertexNameProvider<SDGNode> {

	@Override
	public String getVertexName(SDGNode arg0) {
		String label = (arg0.getBytecodeName() != null)?arg0.getBytecodeName():arg0.getLabel();
		return "[" + arg0.getId() + "] " + " " + arg0.getKind() + " " + label;
	}

}
