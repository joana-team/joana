/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * IDComparator.java
 *
 * Created on 10. September 2005, 11:09
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;

import java.util.Comparator;

/**
 * Compares the IDs of two nodes.
 * @author Siegfried Weber
 */
public class IDComparator implements Comparator<Node> {

    /**
     * Compares the IDs of the two given nodes.
     * Returns a negativ number if the ID of <CODE>o1</CODE>is lower.
     * Returns a positiv number if the ID of <CODE>o2</CODE>is lower.
     * Returns 0 if the two nodes have the same ID.
     * @param o1 a node
     * @param o2 a node
     * @return the comparison
     */
    public int compare(Node o1, Node o2) {
        return NodeConstants.getID(o1) - NodeConstants.getID(o2);
    }
}
