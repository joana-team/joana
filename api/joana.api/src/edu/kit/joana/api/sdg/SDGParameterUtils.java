/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * Provides utility code used for parameters.
 * @author Martin Mohr
 */
class SDGParameterUtils {
	private SDGParameterUtils() {}

	/**
	 * Checks if the given inRoot or outRoot node can reach the given node using only {@link SDGEdge.Kind#PARAMETER_STRUCTURE} edges.
	 * This includes the case 'zero edges', i.e. this method also returns {@code true} if 'node' equals either 'inRoot' or 'outRoot'.
	 * The given node must not be {@code null} and either all three nodes must be formal nodes or all three nodes
	 * must be actual nodes. 
	 * @param node node to check
	 * @param inRoot possible root of a *-in parameter (may be {@code null})
	 * @param outRoot possible root of a *-out parameter (may be {@code null})
	 * @param sdg sdg in which to search
	 * @return whether inRoot or outRoot can reach the given node using only {@link SDGEdge.Kind#PARAMETER_STRUCTURE} edges
	 */
	static boolean psBackwardsReachable(SDGNode node, SDGNode inRoot, SDGNode outRoot, SDG sdg) {
		LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
		Set<SDGNode> visited = new HashSet<SDGNode>();
		worklist.add(node);
		while (!worklist.isEmpty()) {
			SDGNode next = worklist.poll();
			visited.add(next);
			if (next.equals(inRoot) || next.equals(inRoot)) {
				return true;
			} else {
				for (SDGEdge out : sdg.incomingEdgesOf(next)) {
					if (out.getKind() == SDGEdge.Kind.PARAMETER_STRUCTURE && !visited.contains(out.getSource())) {
						worklist.add(out.getSource());
					}
				}
			}
		}
		return false;
	}

}
