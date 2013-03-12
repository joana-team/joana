/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;

/**
 * States represent the execution states of a program's threads or thread regions.
 * This states are represented by Contexts.
 *
 * -- Created on December 5, 2005
 *
 * @author  Dennis Giffhorn
 */
public class States implements Cloneable {
	public static final Context NONRESTRICTIVE = new DynamicContext();
	public static final Context NONE = new DynamicContext();

    /** The state-representing Contexts. */
    private Context[] states;

    /**
     * Creates a new instance of States
     *
     * @param size  The size of the state tuple.
     */
    public States(int size) {
        states = new Context[size];

        for (int i = 0; i < states.length; i++) {
            states[i] = NONE;
        }
    }

    /**
     * Clones the state tuple up to the SDGNodes the Contexts consist of.
     *
     * return  A clone.
     */
    public States clone() {
        States clone = new States(states.length);

        for (int i = 0; i < states.length; i++) {
        	if (states[i] == NONRESTRICTIVE || states[i] == NONE) {
        		clone.set(i, states[i]);

        	} else {
        		Context cl = states[i].copy();
        		clone.set(i, cl);
        	}
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
     * @return  The size of the state tuple.
     */
    public int size() {
        return states.length;
    }

	/**
	 * @return true, iff given States s equals this States.
	 */
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o == null) {
			return false;
		} else if (!(o instanceof States)) {
			return false;
		} else {
			States s = (States) o;

			if (s.size() != size()) {
				return false;
			}

			for (int i = 0; i < size(); i++) {
				if ((states[i] == NONRESTRICTIVE && s.states[i] == NONRESTRICTIVE)
						|| (states[i] == NONE && s.states[i] == NONE)) {
					// skip

				} else if (states[i] == NONRESTRICTIVE || s.states[i] == NONRESTRICTIVE || states[i] == NONE
						|| s.states[i] == NONE) {
					// don't let special cases slip to the equals()-check
					return false;

				} else if (!s.state(i).equals(state(i))) {
					return false;
				}
			}

			return true;
		}
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
    		if (c == NONE)  hc = 31*hc + 1;
    		else if (c == NONRESTRICTIVE)  hc = 31*hc + 2;
    		else hc = 31*hc + c.hashCode();
        }

    	return hc;
    }
}
