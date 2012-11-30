/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*****************************************************************************
 *                                                                           *
 *   Tuple of SDG Nodes                                                      *
 *                                                                           *
 *   This file is part of the chopper package of the sdg library.            *
 *   The used chopping algorithms are desribed in Jens Krinke's PhD thesis   *
 *   "Advanced Slicing of Sequential and Concurrent Programs".               *
 *                                                                           *
 *   authors:                                                                *
 *   Bernd Nuernberger, nuerberg@fmi.uni-passau.de                           *
 *                                                                           *
 *****************************************************************************/

package edu.kit.joana.ifc.sdg.graph;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * This class represents an immutable tuple of SDG nodes.<br>
 *
 * <code>hashCode()</code> and <code>equals()</code> are implemented to
 * ensure the set properties when added into a set.
 *
 * @author Bernd Nuernberger
 */
public class SDGNodeTuple {
    /** The first component of the tuple. */
    private SDGNode firstNode;
    /** The second component of the tuple. */
    private SDGNode secondNode;

    /**
     * Constructs a tuple given two nodes.
     * @param firstNode The  node for the first component.
     * @param secondNode The node for the second component.
     */
    public SDGNodeTuple(SDGNode firstNode, SDGNode secondNode) {
        this.firstNode = firstNode;
        this.secondNode = secondNode;
    }

    /**
     * Compares this tuple to another tuple.
     * @param o The tuple to compare this tuple to.
     * @return <code>true</code> if the tuples are equal,
     *         <code>false</code> if the given tuple does not match this tuple
     */
    public boolean equals(Object o) {
        if (!(o instanceof SDGNodeTuple)) {
        	return false;

        } else {
        	SDGNodeTuple t = (SDGNodeTuple) o;
            return (firstNode == t.firstNode && secondNode == t.secondNode);
        }
    }

    /**
     * Returns the first component of this tuple.
     * @return Node in the first component.
     */
    public SDGNode getFirstNode() {
        return firstNode;
    }

    /**
     * Returns the second component of this tuple.
     * @return Node in the second component.
     */
    public SDGNode getSecondNode() {
        return secondNode;
    }

    /**
     * Setter for the first component of this tuple.
     * @param n  The new firstNode.
     */
    public void setFirstNode(SDGNode n) {
        firstNode = n;
    }

    /**
     * Setter for the second component of this tuple.
     * @param n  The new secondNode.
     */
    public void setSecondNode(SDGNode n) {
        secondNode = n;
    }

    /**
     * Returns a <code>String</code> representation of this tuple.
     * @return String of format (m, n) representing this tuple.
     */
    public String toString() {
        return "("+firstNode+", "+secondNode+")";
    }

    /**
     * Returns the hashcode if this tuple. The lower 16 bits of the hashcode
     * contain the id of the first component node and the higher 16 bits
     * contain the id of the second node. If two tuples are equals, its
     * hashcodes will also match. Visa versa that must not be true if any
     * contained node has an id greater than 65535.
     * @return The hashcode of this tuple.
     */
    public int hashCode() {
        return firstNode.getId() | (secondNode.getId() << 16);
    }
}
