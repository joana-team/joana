/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * LongestPath.java
 *
 * Created on 29. August 2005, 17:53
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * This class implements the longest path algorithm.
 * @author Siegfried Weber
 */
public class LongestPath extends LevelingStrategy {

    /**
     * the PDG
     */
    private PDG graph;

    /**
     * estimated time
     */
    private static final int TIME = 113;

    /**
     * Returns the maximum time for this graph.
     * @param graph a PDG
     * @return the maximum time
     */
    @Override
	int getMaxTime(PDG graph) {
        return TIME;
    }

    /**
     * Starts leveling the graph.
     * @param graph a PDG
     */
    @Override
	public void level(PDG graph) {
        start();
        this.graph = graph;
        List<Node> roots = new LinkedList<Node>();
        roots.add(graph.getEntryNode());
        level(roots, 0);
        check("level");
        go(TIME);
        complete(graph);
    }

    /**
     * Sets recursively the roots of the "unleveled" graph on the specified
     * level.
     * @param roots the roots of the "unleveled" graph
     * @param level the current level
     */
    private void level(Collection<Node> roots, int level) {
        List<Node> newRoots = new LinkedList<Node>();
        for(Node root : roots) {
            graph.setLevel(root, level);
            Set<Node> children = graph.getChildren(root);
            for(Node child : children) {
                boolean isRoot = true;
                Set<Node> parents = graph.getParents(child);
                for(Node parent : parents)
                    if(!parent.hasLevel())
                        isRoot = false;
                if(isRoot)
                    newRoots.add(child);
            }
        }
        if(!newRoots.isEmpty())
            level(newRoots, level + 1);
    }
}
