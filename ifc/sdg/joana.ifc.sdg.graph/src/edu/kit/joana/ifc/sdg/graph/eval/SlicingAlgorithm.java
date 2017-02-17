/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.eval;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;


public class SlicingAlgorithm implements Algorithm {
	private final Slicer slicer;

	public SlicingAlgorithm(Slicer s) {
		slicer = s;
	}

	public Collection<SDGNode> run(Criterion crit) {
		return slicer.slice(crit.getTarget());
	}

	public String getName() {
		return slicer.getClass().getName();//.getSimpleName();
	}

	public void setSDG(SDG g) {
		slicer.setGraph(g);
	}

	public String toString() {
        return slicer.getClass().getName();
    }
}
