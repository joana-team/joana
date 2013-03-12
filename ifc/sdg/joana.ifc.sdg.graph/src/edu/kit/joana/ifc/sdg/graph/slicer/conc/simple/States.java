/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.graph.slicer.conc.simple;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * Contains an execution state of one program's instances of threads.
 * The execution state of a thread is represented by the node
 * that was executed last.
 * The states are represented by an array of SDGNodes.
 *
 * -- Created on March 20, 2006
 *
 * @author  Dennis Giffhorn
 */
public class States implements Cloneable {
    /** The states, consisting of one element per thread instance. */
    private SDGNode[] states;

    /**
     * Creates a new instance of States
     *
     * @param size  The amount of thread instances.
     */
    public States(int size) {
        states = new SDGNode[size];
    }

    /**
     * Creates a clone of this State object.
     */
    public States clone() {
        States clone = new States(states.length);

        for (int i = 0; i < states.length; i++) {
            clone.set(i, states[i]);
        }

        return clone;
    }

    /**
     * Sets the value of the states array at the given position to the new given node.
     *
     * @param pos   The position within the states array to change.
     * @param data  The new value.
     */
    public void set(int pos, SDGNode data) {
        states[pos] = data;
    }

    /**
     * Retrieves the state of some thread.
     *
     * @param pos  The position of the thread's state within the states array.
     * @return     The state represented by a node.
     */
    public SDGNode get(int pos) {
        return states[pos];
    }

    /**
     * Returns the amount of states;
     */
    public int size() {
        return states.length;
    }

    /**
     * Returns a textual representation of this States object.
     */
    public String toString() {
        String str = "";

        for (SDGNode n : states) {
            str += n+"\n";
        }

        return str;
    }

    public int hashCode() {
    	int hc = 1;

    	for (SDGNode n : states) {
    		hc = 31*hc + (n==null ? 0 : n.hashCode());
    	}

    	return hc;
    }

    public boolean equals(Object o) {
    	States s = (States) o;

    	for (int i = 0; i < states.length; i++) {
    		if (states[i] != s.states[i]) return false;
    	}

    	return true;
    }
}
