/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.old;

import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.ContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;


/**
 * Offers two context-based intra-threadual slicing algorithms.
 *
 * -- Created on March 20, 2006
 *
 * @author  Dennis Giffhorn
 */
public class DummyContextSlicer extends ContextSlicer {

    public DummyContextSlicer(SDG g, ContextManager man, MHPAnalysis m){
        super(g, man, m);
    }

    /**
     * Creates a new worklist element from the given values.
     * If this element was not inserted into the worklist yet,
     * it will be added and marked as such.
     *
     * @param node The node of the new element.
     * @param con The context of that node.
     * @param states Its thread states.
     * @param thread The thread the instance of the node belongs to.
     */
    protected void addToWorklist(LinkedList<WorklistElement> worklist,
    		SDGNode node, Context con, States states, int thread) {
        // update the states
        States newStates = states.clone();
        newStates.set(thread, con);

        WorklistElement twe = new WorklistElement(con, newStates);

        // Has vertex 'node' already been visited ?
        if (!isProperlyMarked(node, twe)) {
            worklist.add(twe);
            addMark(node, twe);
        }
    }
}

