/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;


/**
 * A worklist element class for Nanda's Slicing Algorithm.
 * A WorklistElement contains a node, its thread region and the corresponding
 * state tuple.
 *
 * -- Created on September 12, 2005
 *
 * @author  Dennis Giffhorn
 * @version 1.0
 * @see VirtualNode
 * @see States
 */
public class SimpleWorklistElement {
    private SDGNode node;
    private int thread;
    private TopologicalNumber tnr;

    /**
     * Creates a new instance of WorklistElement.
     *
     * @param node    The node for this worklist element.
     * @param states  The node's state tuple.
     */
    public SimpleWorklistElement(SDGNode node, int thread, TopologicalNumber tnr) {
        this.node = node;
        this.thread = thread;
        this.tnr = tnr;
    }


    /* getter */

    /**
     * Returns this WorklistElemnt's node as a VirtualNode.
     *
     * @return  A VirtualNode, containing the node and the ID of its
     *          thread region
     */
    public SDGNode getNode() {
        return node;
    }

    public int getThread() {
    	return thread;
    }

    public TopologicalNumber getTopolNr() {
        return tnr;
    }

    /**
     * Returns the data of this WorklistElement.
     */
    public String toString() {
        String str = "Node: "+node+"\n"+"Thread: "+thread+"\n"+"TNr: "+tnr;
        return str;
    }

    public int hashCode() {
    	return (31*thread + node.hashCode()) * 31 + tnr.hashCode();
    }

    public boolean equals(Object o) {
    	SimpleWorklistElement w= (SimpleWorklistElement) o;
    	return w.node == node && w.thread == thread && w.tnr == tnr;
    }
}
