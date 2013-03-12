/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda;

import java.util.LinkedList;

/**
 * A class for representing current execution states of thread instances.
 *
 * -- Created on September 12, 2005
 *
 * @author  Dennis Giffhorn
 */
public class States implements Cloneable {
    /** The specific states. */
    private LinkedList<TopologicalNumber> states;


    /**
     * Creates a new instance of States
     */
    public States(int threads) {
        states = new LinkedList<TopologicalNumber>();

        for (int i = 0; i < threads; i++) {
            states.add(TopologicalNumber.NONE);
        }
    }

    private States() {
        states = new LinkedList<TopologicalNumber>();
    }

    /**
     * Clones this state.
     *
     * @return  A shallow copy of this state, so be careful when updating states.
     */
    public States clone() {
        States clone = new States();

        for (TopologicalNumber st : states) {
            clone.states.addLast(st);
        }

        return clone;
    }

    public String toString() {
        String str = "";

        for (TopologicalNumber t : states) {
            str += t + "\n";
            str += "--------------\n";
        }

        return str;
    }

    public int hashCode() {
        return states.hashCode();
    }


    /* getter */

    /**
     * Returns the state at the given position.
     *
     * @param pos  The position of the demanded state.
     */
    public TopologicalNumber get(int pos) {
        return this.states.get(pos);
    }

    /**
     * Returns the current amount of states in this state tuple.
     */
    public int size() {
        return this.states.size();
    }

	/**
	 * Checks whether this state tuple and a given one are equal. They are
	 * considered equal if all their states are equal.
	 * 
	 * @param toCheck
	 *            The state tuple to compare with.
	 */
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o == null) {
			return false;
		} else if (!(o instanceof States)) {
			return false;
		} else {
			States toCheck = (States) o;

			if (this.size() != toCheck.size())
				return false;

			for (int i = 0; i < this.size(); i++) {
				if (!this.get(i).equals(toCheck.get(i))) {
					return false;
				}
			}

			return true;
		}
	}

    /* setter */

    /**
     * Changes the values of the state at the given position in the state tuple.
     *
     * @param regionID     The position of the state to change.
     *
     * @param node         The state's new node.
     *
     * @param nodesRegion  The state's new thread region.
     *
     * @param topNr        The state's new topological number (=context).
     */
    public void setState(int thread, TopologicalNumber topNr) {
        states.set(thread, topNr);
    }

    public void setInitialState(int thread, TopologicalNumber topNr) {
    	states.set(thread, topNr);
    }
}
