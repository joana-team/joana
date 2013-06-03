/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.sdg;

import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;

/**
 * Utility class for various methods operating on reduced CFGs.
 * @author Martin Mohr
 */
public final class ReducedCFGUtil {
	
	
	private ReducedCFGUtil() {}

	/**
	 * Prepares the given reduced CFG for being viewed in the graph viewer. Basically,
	 * for each control-flow edge, a respective help edge is added. Note, that this
	 * has only been tested for reduced CFGs resulting from {@link ReducedCFGBuilder#extractReducedCFG(SDG)}.
	 * @param redICFG the reduced CFG to be prepared
	 */
	public static void prepareForViewer(CFG redICFG) {
		List<SDGEdge> toAdd = new LinkedList<SDGEdge>();
		for (SDGEdge e : redICFG.edgeSet()) {
			if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
				toAdd.add(new SDGEdge(e.getSource(), e.getTarget(), SDGEdge.Kind.HELP));
			}
		}
		redICFG.addAllEdges(toAdd);
		
	}
	
}
