/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.barrier;

import java.util.Collection;
import java.util.Map;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


/** Interface for level slicers.
 * A level slicer counts the minimal number of edge traversals each visited node is away from the slicing criterion.
 */
public interface LevelSlicer {
	/** Returns foreach visited node its distance to the slicing criterion.
	 *
	 * @param c  The slicing criterion.
	 * @return   A map containing the slice, where each node is mapped to its distance to the the slicing criterion.
	 */
    Map<SDGNode,Integer> slice(Collection<SDGNode> n);

    Map<SDGNode, Integer> slice(SDGNode n);

	/** Traverses the graph only `maxSteps' wide from the slicing criterion.
	 * Returns foreach visited node its distance to the slicing criterion.
	 *
	 * @param c  The slicing criterion.
	 * @return   A map containing the slice, where each node is mapped to its distance to the the slicing criterion.
	 */
    Map<SDGNode, Integer> slice(Collection<SDGNode> n, int maxSteps);

    Map<SDGNode, Integer> slice(SDGNode n, int maxSteps);
}
