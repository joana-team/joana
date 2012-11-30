/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.util.Collection;
import java.util.Collections;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * @author  Dennis Giffhorn
 */
public abstract class IntraproceduralSlicer implements Slicer {
    protected SDG graph;
    /**
     * Creates a new instance of IntraproceduralSlicer
     */
    public IntraproceduralSlicer(SDG g) {
        graph = g;
    }

    public void setGraph(SDG graph) {
        this.graph = graph;
    }

    public Collection<SDGNode> slice(SDGNode criterion) {
    	return slice(Collections.singleton(criterion));
    }

    public abstract Collection<SDGNode> slice(Collection<SDGNode> nodes);
}
