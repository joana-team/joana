/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.regions;

import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;


/**
 * A class for representing current execution states of thread instances.
 *
 * -- Created on September 12, 2005
 *
 * @author  Dennis Giffhorn
 */
public class States implements Cloneable {
    /** The specific states. */
    private LinkedList<State> states;


    /**
     * Creates a new instance of States
     */
    public States(int threads) {
        states = new LinkedList<State>();

        for (int i = 0; i < threads; i++) {
            states.add(State.NONRESTRICTIVE);
        }
    }

    private States() {
        states = new LinkedList<State>();
    }

    /**
     * Clones this state.
     *
     * @return  A shallow copy of this state, so be careful when updating states.
     */
    public States clone() {
        States clone = new States();

        for (State st : states) {
            clone.states.addLast(st);
        }

        return clone;
    }

    public String toString() {
        String str = "";

        for (State t : states) {
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
    public State get(int pos) {
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
    public void setState(VirtualNode node, int thread, TopologicalNumber topNr) {
        if (topNr == null) throw new NullPointerException();
        State s = new State(node, topNr); // states.get(thread);
        states.set(thread, s);
    }

    public void setState(State s, int thread) {
        if (s == null) throw new NullPointerException();
        states.set(thread, s);
    }

    public void setInitialState(VirtualNode node, int thread, TopologicalNumber topNr) {
    	State s = new State(node, topNr);
    	states.set(thread, s);
    }
}
