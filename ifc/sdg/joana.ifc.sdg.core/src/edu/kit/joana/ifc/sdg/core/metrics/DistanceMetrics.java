/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.metrics;

import java.util.Collection;
import java.util.Map;

import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.conc.I2PLevelBackward;


public class DistanceMetrics implements IMetrics {
	private final I2PLevelBackward slicer;

	public DistanceMetrics() {
		slicer = new I2PLevelBackward(null);
	}

	public Collection<ClassifiedViolation> computeMetrics(SDG g, Collection<ClassifiedViolation> vios) {
		slicer.setGraph(g);

		for (ClassifiedViolation v : vios) {
			Map<SDGNode, Integer> filters = slicer.slice(v.getSink());
			int dist = filters.get(v.getSource());
			IMetrics.Rating rat = determineRating(dist);
			v.addClassification("Distance", "Flows through at least "+dist+" dependences.", dist, rat);
//			System.out.println(v);
//			System.out.println("Flows through at least "+dist+" dependences.");
		}

		return vios;
	}

	private IMetrics.Rating determineRating(int dist) {
		if (dist < 100) return IMetrics.Rating.DANGEROUS;
		else if (dist < 1000) return IMetrics.Rating.MOSTLY_HARMLESS;
		else return IMetrics.Rating.HARMLESS;
	}
}
