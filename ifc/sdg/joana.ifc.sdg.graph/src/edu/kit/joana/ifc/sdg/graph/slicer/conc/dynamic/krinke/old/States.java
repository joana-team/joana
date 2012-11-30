/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.old;

import java.util.Collection;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;


/**
 * States represent the execution states of a program's threads or thread regions.
 * This states are represented by Contexts.
 *
 * -- Created on December 5, 2005
 *
 * @author  Dennis Giffhorn
 */
public class States {
    /** The state-representing Contexts. */
    private Context[] states;

    /**
     * Creates a new instance of States
     *
     * @param size  The size of the state tuple.
     */
    public States(List<Context> states) {
        this.states = new Context[states.size()];

        for (int i = 0; i< this.states.length; i++) {
            this.states[i] = states.get(i);
        }
    }

    private States(int size) {
        this.states = new Context[size];
    }

    /**
     * Clones the state tuple up to the SDGNodes the Contexts consist of.
     *
     * return  A clone.
     */
    public States clone() {
        States clone = new States(states.length);

        for (int i = 0; i < states.length; i++) {
        	Context cl = states[i].copy();
            clone.set(i, cl);
        }

        return clone;
    }

    /**
     * Returns the Context at a given position of the state tuple.
     *
     * @param pos  The position.
     */
    public Context state(int pos) {
        return states[pos];
    }

    /**
     * Sets one tuple position to a new value.
     *
     * @param pos      The position to change.
     * @param context  The new value.
     */
    public void set(int pos, Context newState) {
        states[pos] = newState;
    }

    /**
     * Updates the state tuple entires of all given regions to the given new Contetxt.
     * It creates a clone of this States, modifies the clone's state tuple
     * and returns the clone.
     *
     * @param seq  The threads to modify.
     * @param con  The new value.
     * @return     A 'modified clone'.
     */
    public States update(Collection<Integer> seq, Context con) {
        States clone = clone();

        for (int i : seq) {
            clone.set(i, con);
        }

        return clone;
    }

    /**
     * @return  The size of the state tuple.
     */
    public int size() {
        return states.length;
    }

    /**
     * @return  true, iff given States s equals this States.
     */
    public boolean equals(Object o) {
    	States s = (States) o;

        if (s.size() != size()) {
            return false;
        }

        for (int i = 0; i < size(); i++) {
            if (!s.state(i).equals(state(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return  A textual representation of this States.
     */
    public String toString() {
        String str = "";

        for (int i = 0; i < states.length; i++) {
            str += "ThreadRegion "+i+" : "+states[i]+"\n";
        }

        return str;
    }

    public int hashCode() {
    	int hc = 1;

    	for (Context c : states) {
            hc = 31*hc +(c==null ? 0 : c.hashCode());
        }

    	return hc;
    }
}
