/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph;

import java.util.List;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * A VirtualNode represents a certain instance of a node in a graph.
 * It can be used for virtual code duplication e.g. in graphs for threaded
 * programs, where threads usually shall not share code.
 *
 * @author giffhorn
 * @version 1.0
 * @see SDGNode
 */
public class VirtualNode {
    /** The represented node. */
    private SDGNode node;

    /** An ID for differentiating between instances of 'node' - typically a thread or thread regions ID. */
    private int number;

    /**
     * Creates a new instance of VirtualNode
     *
     * @param node    The node.
     * @param number  An ID to differentiate between instances of 'node'.
     */
    public VirtualNode(SDGNode node, int number) {
        this.node = node;
        this.number = number;
    }

    /**
     * Returns the represented node.
     */
    public SDGNode getNode() {
        return node;
    }

    /**
     * Returns the ID.
     */
    public int getNumber() {
        return number;
    }

    /**
     * Compares this VirtualNode with a given one.
     * They are considered equal if their nodes and IDs are equal.
     *
     * @param vn  The VirtualNode to compare with.
     */
    public boolean equals(Object o) {
    	VirtualNode vn = (VirtualNode) o;
        return this.node == vn.getNode() && this.number == vn.getNumber();
    }

    /**
     * Returns a representation of this Virtual Node.
     *
     * @return A string of the form "(<node-ID>, <virtual number>)"
     */
    public String toString() {
        return ("("+node+", "+number+")");
    }

    /**
     * Generates a List of VirtualNodes for a given node and a set of IDs.
     *
     * @param node  The node.
     * @param nums  The set of IDs.
     * @return      A list of VirtualNodes, unsorted.
     */
    public static List<VirtualNode> virtualNodes(SDGNode node, int[] nums) {
        LinkedList<VirtualNode> list = new LinkedList<VirtualNode>();

        for (int n : nums) {
            VirtualNode vn = new VirtualNode(node, n);
            list.addFirst(vn);
        }

        return list;

    }

    public int hashCode() {
        return node.getId() | (number << 16);
    }
}
