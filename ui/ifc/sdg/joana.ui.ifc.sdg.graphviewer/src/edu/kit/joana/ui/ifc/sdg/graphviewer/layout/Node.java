/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Node.java
 *
 * Created on 7. September 2005, 12:44
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.jgraph.graph.Edge;
import org.jgraph.graph.GraphCell;
import org.jgraph.graph.Port;

/**
 * This class represents a vertex or a bend point.
 * @author Siegfried Weber
 */
public class Node {

    /**
     * the number of an undefined level or index
     */
    public static final int UNDEFINED = -1;

    /**
     * the level of this node
     */
    private int level;
    /**
     * the index in the level of this node
     */
    private int index;
    /**
     * true if it is a vertex
     */
    private boolean isVertex;
    /**
     * a list of control dependant neighbors in the upper level
     */
    private List<Node> upperCDNeighbors;
    /**
     * a list of data dependant neighbors in the upper level
     */
    private List<Node> upperDDNeighbors;
    /**
     * a list of control dependant neighbors in the lower level
     */
    private List<Node> lowerCDNeighbors;
    /**
     * a list of data dependant neighbors in the lower level
     */
    private List<Node> lowerDDNeighbors;
    /**
     * the x-coordinate of this node
     */
    private int xCoord;
    /**
     * the JGraph cell
     */
    private GraphCell cell;
    /**
     * the attributes of this node
     */
    private Map<String, Object> attributes;
    /**
     * true if this node is a bend point and the edge points down
     */
    private boolean pointsDown;

    /**
     * Creates a new instance of Node
     * @param cell a JGraph cell
     */
    public Node(GraphCell cell) {
        level = UNDEFINED;
        index = UNDEFINED;
        isVertex = !(cell instanceof Edge) && !(cell instanceof Port);
        upperCDNeighbors = new LinkedList<Node>();
        upperDDNeighbors = new LinkedList<Node>();
        lowerCDNeighbors = new LinkedList<Node>();
        lowerDDNeighbors = new LinkedList<Node>();
        xCoord = UNDEFINED;
        this.cell = cell;
        attributes = new Hashtable<String, Object>();
    }

    /**
     * Returns if the level of this node is defined.
     * @return true if the level of this node is defined
     */
    boolean hasLevel() {
        return level != UNDEFINED;
    }

    /**
     * Returns the level of this node.
     * @return the level of this node
     */
    int getLevel() {
        return level;
    }

    /**
     * Sets the level of this node.
     * @param level a level
     */
    void setLevel(int level) {
        this.level = level;
    }

    /**
     * Returns the index of this point in a level.
     * @return the index
     */
    int getIndex() {
        return index;
    }

    /**
     * Sets the index of this point in a level.
     * @param index a index
     */
    void setIndex(int index) {
        this.index = index;
    }

    /**
     * Returns whether this node is a vertex or a bend point.
     * @return true if this node is a vertex
     */
    boolean isVertex() {
        return isVertex;
    }

    /**
     * Returns the JGraph cell.
     * @return the JGraph cell
     */
    public GraphCell getCell() {
        return cell;
    }

    /**
     * Returns the x-coordinate of this node.
     * @return the x-coordinate
     */
    int getXCoord() {
        return xCoord;
    }

    /**
     * Sets the x-coordinate of this node.
     * @param x a x-coordinate
     */
    void setXCoord(int x) {
        xCoord = x;
    }

    /**
     * Sets the direction of the edge if this node is a bend point.
     * @param pointsDown true if the edge points down
     */
    void setPointsDown(boolean pointsDown) {
        this.pointsDown = pointsDown;
    }

    /**
     * Returns whether the edge of this bend point points down.
     * @return true if the edge points down
     */
    boolean pointsDown() {
        return pointsDown;
    }

    /**
     * Returns an attribute of this node.
     * @param key the key of the attribute
     * @return the value of the attribute
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Sets an attribute.
     * @param key the key of the attribute
     * @param value the value of the attribute
     */
    public void setAttribute(String key, Node value) {
        attributes.put(key, value);
    }

    public void setAttribute(String key, int value) {
    	try {
    		attributes.put(key, value);
    	} catch (NullPointerException n) {
    		System.out.println(attributes);
    		System.out.println(key);
    		System.out.println(value);
    	}
    }

    public void setAttribute(String key, float value) {
    	try {
    		attributes.put(key, value);
    	} catch (NullPointerException n) {
    		System.out.println(attributes);
    		System.out.println(key);
    		System.out.println(value);
    	}
    }

    /**
     * Returns the nodes in the specified list ordered by their indices.
     * @param neighbors a list of nodes
     * @return an ordered array of nodes
     */
    private Node[] getOrderedNeighbors(List<Node> neighbors) {
        TreeSet<Node> orderedSet = new TreeSet<Node>(new IndexComparator());
        orderedSet.addAll(neighbors);
        return orderedSet.toArray(new Node[orderedSet.size()]);
    }

    /**
     * Returns the upper neighbors of this node ordered by their index.
     * @return the upper neighbors of this node
     */
    Node[] getUpperNeighbors() {
        List<Node> upperNeighbors = new LinkedList<Node>(upperCDNeighbors);
        upperNeighbors.addAll(upperDDNeighbors);
        return getOrderedNeighbors(upperNeighbors);
    }

    /**
     * Returns whether this node has upper control dependant neighbors.
     * @return true if this node has upper control dependant neighbors
     */
    boolean hasUpperCDNeighbors() {
        return !upperCDNeighbors.isEmpty();
    }

    /**
     * Returns the upper control dependant neighbors of this node ordered by
     * their index.
     * @return the upper control dependant neighbors of this node
     */
    Node[] getUpperCDNeighbors() {
        return getOrderedNeighbors(upperCDNeighbors);
    }

    /**
     * Adds an upper neighbor of this node.
     * @param upperNeighbor an upper neighbor
     * @param controlDependent true if the neighbor is control dependant
     */
    void addUpperNeighbor(Node upperNeighbor, boolean controlDependent) {
        if(controlDependent) {
            upperCDNeighbors.add(upperNeighbor);
        }
        else {
            upperDDNeighbors.add(upperNeighbor);
        }
    }

    /**
     * Returns the lower neighbors of this node ordered by their index.
     * @return the lower neighbors of this node
     */
    Node[] getLowerNeighbors() {
        List<Node> lowerNeighbors = new LinkedList<Node>(lowerCDNeighbors);
        lowerNeighbors.addAll(lowerDDNeighbors);
        return getOrderedNeighbors(lowerNeighbors);
    }

    /**
     * Returns whether this node has lower control dependant neighbors.
     * @return true if this node has lower control dependant neighbors
     */
    boolean hasLowerCDNeighbors() {
        return !lowerCDNeighbors.isEmpty();
    }

    /**
     * Returns the lower control dependant neighbors of this node ordered by
     * their index.
     * @return the lower control dependant neighbors of this node
     */
    Node[] getLowerCDNeighbors() {
        return getOrderedNeighbors(lowerCDNeighbors);
    }

    /**
     * Adds a lower neighbor of this node.
     * @param lowerNeighbor a lower neighbor
     * @param controlDependent true if the neighbor is control dependant
     */
    void addLowerNeighbor(Node lowerNeighbor, boolean controlDependent) {
        if(controlDependent) {
            lowerCDNeighbors.add(lowerNeighbor);
        }
        else {
            lowerDDNeighbors.add(lowerNeighbor);
        }
    }

    /**
     * Returns a string representation of this node.
     * @return a string representing this node
     */
    @Override
	public String toString() {
        if(isVertex) {
            return cell.toString();
        }

        return "edge node";
    }

    /**
     * This class compares indices of nodes.
     */
    class IndexComparator implements Comparator<Node> {

        /**
         * Compares the indices of the given nodes.
         * @param o1 a node
         * @param o2 a node
         * @return the comparison of the indices
         */
        public int compare(Node o1, Node o2) {
            return o1.index - o2.index;
        }
    }
}
