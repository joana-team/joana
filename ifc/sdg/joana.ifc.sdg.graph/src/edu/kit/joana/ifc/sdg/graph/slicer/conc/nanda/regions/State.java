/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.regions;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;

/**
 * A class for representing an execution state of a thread.
 * It consists of a context, represented by a topological number,
 * the node of that context, and a thread region containing the
 * node.
 *
 * -- Created on September 12, 2005
 *
 * @author  Dennis Giffhorn
 */
public class State {
	public static final State NONRESTRICTIVE = new State();
	public static final State NONE = new State();

    /** The actual node. */
    private final VirtualNode actualNode;
    /** The actual context. */
    private final TopologicalNumber topolNr;

    /**
     * Creates a new instance of State
     */
    private State() {
        this.actualNode = null;
        this.topolNr = null;
    }

    public State(VirtualNode v, TopologicalNumber n) {
        this.actualNode = v;
        this.topolNr = n;
    }

    public String toString() {
    	if (this == State.NONE) {
    		return "NONE\n";

    	} else if (this == State.NONRESTRICTIVE) {
    		return "NONRESTRICTIVE\n";

    	} else {
	        String str ="Node:         " +actualNode+ "\n";
	        str += "Topolog. Nr.: " +topolNr+ "\n";
	        return str;
	    }
    }

    public int hashCode() {
        if (actualNode == null) return 1;
        else return topolNr==null ? 0 : topolNr.hashCode();
    }


    /* getter */

    /**
     * Returns the actual node of this state.
     *
     * @return  Can be 'null', if the state is the initial state.
     */
    public VirtualNode getActualNode() {
        return actualNode;
    }

    /**
     * Returns the actual context of this state.
     *
     * @return  Can be -1, if the state is the initial state.
     */
    public TopologicalNumber getTopolNr() {
        return topolNr;
    }

	/**
	 * Checks whether this state and a given one are equal. They are equal if
	 * their actual nodes, contexts and thread regions are equal.
	 * 
	 * @param toCheck
	 *            The state to compare with.
	 */
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof State)) {
			return false;
		}

		State toCheck = (State) o;

		if (this == State.NONE && toCheck == State.NONE)
			return true;
		if (this == State.NONRESTRICTIVE && toCheck == State.NONRESTRICTIVE)
			return true;

		return this.actualNode.equals(toCheck.actualNode) && this.topolNr == toCheck.topolNr;
	}
}
