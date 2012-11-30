/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;

/**
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class SlicerFlat extends Slicer implements edu.kit.joana.ifc.sdg.graph.slicer.Slicer {

    public SlicerFlat(SDG graph) {
        super(graph);
    }

    protected States update(States s, Context c) {
    	States newStates = s.clone();
    	int thread = c.getThread();

		if (!mhp.isDynamic(thread)) {
			// adjust the state of the thread
			newStates.set(thread, c);
		}

        return newStates;
    }
}
