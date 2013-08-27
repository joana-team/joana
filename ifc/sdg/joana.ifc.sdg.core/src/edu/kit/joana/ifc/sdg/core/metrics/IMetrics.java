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


public interface IMetrics {
	enum Rating {
        HARMLESS("GREEN"),
        MOSTLY_HARMLESS("YELLOW"),
        DANGEROUS("RED");

        private final String value;

        Rating(String s) { value = s; }

        public String toString() {
            return value;
        }
    }

	Collection<ClassifiedViolation> computeMetrics(SDG g, Collection<ClassifiedViolation> vios);
}
