/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.metrics;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;


public class NoMetrics implements IMetrics {

	public Collection<ClassifiedViolation> computeMetrics(SDG g, Collection<ClassifiedViolation> vios) {
		return vios;
	}
}
