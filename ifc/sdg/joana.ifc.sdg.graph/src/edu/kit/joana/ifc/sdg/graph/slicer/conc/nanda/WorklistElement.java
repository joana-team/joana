/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda;

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
public class WorklistElement {
    private final SDGNode node;
    private final int thread;
    private final TopologicalNumber tnr;

    /** The state tuple of the node. */
    private final States states;

    /**
     * Creates a new instance of WorklistElement.
     *
     * @param node    The node for this worklist element.
     * @param states  The node's state tuple.
     */
    public WorklistElement(SDGNode node, int thread, TopologicalNumber tnr, States states) {
        this.node = node;
        this.thread = thread;
        this.tnr = tnr;
        this.states = states;
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
     * Returns this WorklistElement's state tuple.
     *
     * @return  The state tuple as States.
     */
    public States getStates() {
        return states;
    }

    /**
     * Returns the State of a given region.
     *
     * @param region  The region whose state is needed.
     * @return        The region's state.
     */
    public TopologicalNumber getStateOf(int region) {
        return states.get(region);
    }

    /**
     * Returns the data of this WorklistElement.
     */
    public String toString() {
        String str = "Virtual Node: \n";

        str += node.toString() + "\n";
        str += "Topol. Nr.: "+tnr+"\n";
        str += "States: \n";
        str += states + "\n";

        return str;
    }
}
