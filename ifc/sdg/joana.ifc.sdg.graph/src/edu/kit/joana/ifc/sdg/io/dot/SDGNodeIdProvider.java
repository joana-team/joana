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
 * Helper class for dot export of JoanaGraphs, which generates IDs of dot nodes from the IDs of SDG nodes.
 * @author Martin Mohr
 */
public final class SDGNodeIdProvider implements VertexNameProvider<SDGNode> {
	
	private static final SDGNodeIdProvider INSTANCE = new SDGNodeIdProvider();
	
	/** prevent instantiation */
	private SDGNodeIdProvider() {}
	
	@Override
	public String getVertexName(SDGNode arg0) {
		return arg0.getId() + "";
	}
	
	/**
	 * Returns the only instance of this class.
	 * @return the only instance of this class
	 */
	public static final SDGNodeIdProvider getInstance() {
		return INSTANCE;
	}

}
