/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.old;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;


/**
 * Represents a worklist element consisting of
 * - a context
 * - a state tuple for the program's threads
 *
 * -- Created on December 5, 2005
 *
 * @author  Dennis Giffhorn
 */
public class WorklistElement {
    /** The context. */
    private Context context;
    /** The state tuple. */
    private States states;

    /**
     * Creates a new instance of WorklistElement
     *
     * @param con     A context.
     * @param states  A state tuple.
     */
    public WorklistElement(Context con, States states) {
    	if (con == null) throw new NullPointerException();
        context = con;
        this.states = states;
    }

    /**
     * Clones this WorklistElement.
     * All data up to SDGNodes is cloned.
     *
     * @return A deep-copy up to SDGNodes.
     */
    public WorklistElement clone() {
        return new WorklistElement(context.copy(), states.clone());
    }

    /**
     * @return The Context.
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return The thread the context belongs to.
     */
    public int getThread() {
        return context.getThread();
    }

    /**
     * @return The state tuple.
     */
    public States getStates() {
        return states;
    }

    /**
     * @return The node of the context.
     */
    public SDGNode getNode() {
        return context.getNode();
    }

    /**
     * Modifies the state tuple with the given values.
     *
     * @param pos  The position in the state tuple to set.
     * @param c    The new context for that position.
     */
    public void setState(int pos, Context c) {
        states.set(pos, c);
    }

    /**
     * Returns 'true' if the given element equals this element.
     * They equal if their contexts consist of the same vertices in the
     * same order and their state tuples are the same.
     *
     * @param elem  The WorklistElement to compare with.
     */
    public boolean equals(Object o) {
    	WorklistElement elem = (WorklistElement) o;

        if (elem == null || elem.getThread() != this.getThread()) {
            return false;
        }

        if (!context.equals(elem.getContext())) {
            return false;
        }

        return elem.getStates().equals(this.states);
    }

    /**
     * @return A textual representation of the element.
     */
    public String toString() {
        return "Node: "+context.getNode()+"\n"
                +"Thread: "+getThread()+"\n"
                +"Context: "+context+"\n"
                +"States: \n"+states;
    }

    public int hashCode() {
    	return 31*context.hashCode() + states.hashCode();
    }
}

