/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.krinke;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;

/** A class representing a worklist element in Krinke's outer worklist.
 * Consists of a Context and a State.
 *
 * -- Created on December 5, 2005
 *
 * @author  Dennis Giffhorn
 */
public class WorklistElement implements Cloneable {
    private Context context;
    private States states;

    /** Creates a new instance of WorklistElement
     * @param con  The Context.
     * @param states  The thread execution states.
     */
    public WorklistElement(Context con, States states) {
    	if (con == null) throw new NullPointerException();
        context = con;
        this.states = states;
    }

    /** Clones this WorklistElement.
     * @return  A deep-copy of this WorklistElement.
     */
    public WorklistElement clone() {
        return new WorklistElement(context.copy(), states.clone());
    }

    /** Returns the Context of the WorklistElement.
     * @return  The Context of the WorklistElement.
     */
    public Context getContext() {
        return context;
    }

    /** Returns the thread the WorklistElement belongs to.
     * @return  The thread the WorklistElement belongs to.
     */
    public int getThread() {
        return context.getThread();
    }

    /** Returns the States of this WorklistElement.
     * @return  A clone of the States.
     */
    public States getStates() {
        return states.clone();
    }

    /** Returns the node of the WorklistElement.
     * @return  The node of the WorklistElement.
     */
    public SDGNode getNode() {
        return context.getNode();
    }

    /** Sets a new State for a certain thread.
     * @param thread  The thread.
     * @param c  The new State.
     */
    public void setState(int thread, Context c) {
        states.set(thread, c);
    }

    /** Returns 'true' if the given element equals this element.
     * They equal if their contexts consist of the same vertices in the
     * same order and their threads are the same.
     *
     * @param elem  The WorklistElement to compare with.
     */
    public boolean equals(Object o) {
    	WorklistElement elem = (WorklistElement) o;

        if (elem == null || context.size() != elem.getContext().size() ||
                elem.getThread() != this.getThread()) {

            return false;
        }

        if (!context.equals(elem.getContext())) {
            return false;
        }

        return elem.getStates().equals(this.states);
    }

    /** Returns a String representing the State of the WorklistElement.
     * @return  A String representing the State of the WorklistElement.
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

