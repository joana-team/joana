/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.io.dot;

import org.jgrapht.ext.VertexNameProvider;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * Helper class for dot export of JoanaGraphs, which generates labels of dot nodes from kinds and labels of SDG nodes.
 * @author Martin Mohr
 */
public final class SDGNodeNameProvider implements VertexNameProvider<SDGNode> {
	
	private static final SDGNodeNameProvider INSTANCE = new SDGNodeNameProvider();
	
	/** prevent instantiation */
	private SDGNodeNameProvider() {}
	
	@Override
	public String getVertexName(SDGNode arg0) {
		String label = (arg0.getLabel() == null)?arg0.getBytecodeName():arg0.getLabel();
		return "[" + arg0.getId() + "] " + " " + arg0.getKind() + " " + label;
	}
	
	/**
	 * Returns the only instance of this class.
	 * @return the only instance of this class
	 */
	public static final SDGNodeNameProvider getInstance() {
		return INSTANCE;
	}

}
