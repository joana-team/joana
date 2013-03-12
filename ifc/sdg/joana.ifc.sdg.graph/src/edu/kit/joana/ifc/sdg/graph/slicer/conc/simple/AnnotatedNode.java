/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.graph.slicer.conc.simple;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;

/**
 * An AnnotatedNode is a node assigned with its thread and the current execution states
 * of the program's thread instances.
 *
 * -- Created on March 20, 2006
 *
 * @author  Dennis Giffhorn
 */
public class AnnotatedNode {
    /**
     * The node.
     */
    private VirtualNode node;

    /**
     * The thread states according to the node.
     */
    private States states;
    /**
     * Creates a new instance of AnnotatedNode
     *
     * @param n  The node.
     * @param s  The thread states according to the node.
     * @param t  The node's thread.
     */
    public AnnotatedNode(SDGNode n, States s, int t) {
        node = new VirtualNode(n, t);
        states = s;
    }

    public VirtualNode getVirtual() {
        return node;
    }

    /**
     * Returns the node.
     */
    public SDGNode getNode() {
        return node.getNode();
    }

    /**
     * Returns the thread states.
     */
    public States getStates() {
        return states;
    }

    /**
     * Returns the thread of the AnnotatedNode.
     */
    public int getThread() {
        return node.getNumber();
    }

    /**
     * Returns a clone of the thread states.
     */
    public States cloneStates() {
        return states.clone();
    }

    /**
     * A textual representation of this AnnotatedNode.
     */
    public String toString() {
        String str = "";

        str += "node: "+node+"\n";
        str += "states: \n"+states+"\n";

        return str;
    }

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o == null) {
			return false;
		} else if (!(o instanceof AnnotatedNode)) {
			return false;
		} else {
			AnnotatedNode a = (AnnotatedNode) o;
			return a.node.equals(node) && a.states.equals(states);
		}
	}

    public int hashCode() {
    	int hc = 31*node.hashCode() + states.hashCode();
    	return hc;
    }
}
