/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph;

import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


/** An interface for folded graphs.
 *
 * @see FoldedCFG
 * @see FoldedBipartiteCallGraph
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public interface Folded {

    /** Maps a given vertex to this graph.
     * Returns the vertex itself or a fold vertex that folds this vertex.
     */
    public SDGNode map(SDGNode node);

    /** Unmaps a fold vertex.
     * Delivers _one_ of the vertices the fold vertex folds.
     * Needed for mapping of fold vertices between different folded graphs.
     */
    public SDGNode unmap(SDGNode node);

    /** Returns true if the given node is folded.
     */
    public boolean isFolded(SDGNode node);

    /** Gets the fold node of a given folded node.
     * Should return the node itself if it is not a folded node.
     */
    public SDGNode getFoldNode(SDGNode node);

    /** Returns all vertices a given fold vertex folds.
     *
     * @param fold  The fold vertex.
     * @return  A list with all folded vertices.
     */
    public List<SDGNode> getFoldedNodesOf(SDGNode fold);
}
