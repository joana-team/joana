/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.violations.paths;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;


public class ChopWrapper {
    private final RepsRosayChopper chopper;

    public ChopWrapper(SDG g) {
        chopper = new RepsRosayChopper(g);
    }

    public void setSDG(SDG g) {
        chopper.setGraph(g);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Collection<SecurityNode> chop (SecurityNode outNode, SecurityNode violation) {
        return (Collection) chopper.chop(violation, outNode);
    }
}
