/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;


/** An interface for ContextManagers.
 *
 * @author giffhorn
 */
public interface ContextManager<C extends Context> {

	/** Retrieves all contexts of the given node.
	 *
	 * @param node  The given node.
	 * @return      A set of contexts.
	 */
	Collection<C> getAllContextsOf(SDGNode node);

	/** Retrieves all contexts of the given node inside the given thread.
	 *
	 * @param node    The given node.
	 * @param thread  The given thread.
	 * @return        A set of contexts.
	 */
	Collection<C> getContextsOf(SDGNode node, int thread);

	/** Traverses intra-procedurally from `oldContext' to `reachedNode',
	 * builds the according Context of `reachedNode' and returns it.
	 *
	 * @param reachedNode  The reached node.
	 * @param oldContext   The Context from which reachedNode is reached.
	 * @return             The Context of reachedNode.
	 */
    C level(SDGNode reachedNode, C oldContext);

    /** Enters a called procedure at call site `call site', coming from `oldContext'
     * and going to `reachedNode'. Returns the resulting Context of `reachedNode'.
	 *
     * @param reachedNode  The reached node.
     * @param callSite     The call site at which the traversal takes place.
     * @param oldContext   The Context from which reachedNode is reached.
     * @return             The Context of reachedNode.
     */
    C descend(SDGNode reachedNode, SDGNodeTuple callSite, C oldContext);

    /** Leaves a procedure towards the calling procedure specified by call site `callSite'.
     * Traverses from `oldContext' to `reachedNode' and returns the resulting Context of `reachedNode'
     *
     * @param reachedNode  The reached node.
     * @param callSite     The call site at which the traversal takes place.
     * @param oldContext   The Context from which reachedNode is reached.
     * @return             The Context of reachedNode.
     */
    C[] ascend(SDGNode reachedNode, SDGNodeTuple callSite, C oldContext);

    /** Unmaps a fold node to any of its folded nodes.
     * If the given node is not a fold node, it is returned itself.
     *
     * @param node  The fold node.
     * @return      One of its folded nodes
     */
    SDGNode unmap(SDGNode node);
}
