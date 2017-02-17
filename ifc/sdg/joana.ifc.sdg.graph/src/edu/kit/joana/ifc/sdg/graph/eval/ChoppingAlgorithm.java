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
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;


public class ChoppingAlgorithm implements Algorithm {
	private final Chopper chopper;

	public ChoppingAlgorithm(Chopper s) {
		chopper = s;
	}

	public Collection<SDGNode> run(Criterion crit) {
		return chopper.chop(crit.getSource(), crit.getTarget());
	}

	public String getName() {
		return chopper.getClass().getName();//.getSimpleName();
	}

	public void setSDG(SDG g) {
		chopper.setGraph(g);
	}

	public String toString() {
		return chopper.getClass().getName();
	}
}
