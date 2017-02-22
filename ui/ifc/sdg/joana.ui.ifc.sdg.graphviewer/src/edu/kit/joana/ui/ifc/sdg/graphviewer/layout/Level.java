/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Level.java
 *
 * Created on 7. September 2005, 12:42
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


/**
 * This class represents a level (or layer).
 * @author Siegfried Weber
 */
public class Level implements Iterable<Node> {

    /**
     * the level number
     */
    private final int level;
    /**
     * the nodes in this level
     */
    private List<Node> nodes;

    /**
     * Creates a new instance of Level
     * @param level the level number
     */
    public Level(int level) {
        this.level = level;
        nodes = new ArrayList<Node>();
    }

    /**
     * Adds a node to this level.
     * @param node a node
     */
    public void add(Node node) {
        node.setLevel(level);
        node.setIndex(nodes.size());
        nodes.add(node);
    }

    /**
     * Moves a node in the level.
     * @param oldIndex the old position
     * @param newIndex the new position
     */
    public void changeIndex(int oldIndex, int newIndex) {
        Node n = nodes.remove(oldIndex);
        nodes.add(newIndex, n);
        int lower = oldIndex;
        int upper = newIndex;
        if(oldIndex > newIndex) {
            lower = newIndex;
            upper = oldIndex;
        }
        for(int i = lower; i <= upper; i++)
            nodes.get(i).setIndex(i);
    }

    /**
     * Returns the number of nodes in this level.
     * @return the number of nodes
     */
    public int size() {
        return nodes.size();
    }

    /**
     * Returns the node at the specified position.
     * @param index a position
     * @return the node at the specified position
     */
    public Node get(int index) {
        return nodes.get(index);
    }

    /**
     * Returns all nodes in this level.
     * @return all nodes in this level
     */
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * Returns an iterator for the nodes in this level.
     * @return an iterator
     */
    public Iterator<Node> iterator() {
        return nodes.iterator();
    }

    /**
     * Sorts the nodes in this level with the specified comparator.
     * @param comparator a comparator for nodes
     */
    public void sort(Comparator<Node> comparator) {
        Node[] nodeArray = nodes.toArray(new Node[nodes.size()]);
        Arrays.sort(nodeArray, comparator);
        nodes = Arrays.asList(nodeArray);

        int index = 0;
        for(Node node : nodes)
            node.setIndex(index++);
    }
}
