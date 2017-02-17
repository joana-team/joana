/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.violations.paths;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerForward;


public class SliceWrapper {
    private final SummarySlicerBackward sback;
    private final SummarySlicerForward sfor;

    public SliceWrapper(SDG g) {
        sback = new SummarySlicerBackward(g);
        sfor = new SummarySlicerForward(g);
    }

    public void setGraph(SDG g) {
        sback.setGraph(g);
        sfor.setGraph(g);
    }

    /**
     * Wraps a backward slice into security nodes
     * @param startNode
     * @return
     */
    public HashSet<SecurityNode> backwardSlice(SecurityNode startNode) {
        HashSet<SecurityNode> set = new HashSet<SecurityNode>();
        LinkedList<SDGNode> criterion = new LinkedList<SDGNode>();

        criterion.add(startNode);

        Collection<SDGNode> c = sback.slice(criterion);

        for (SDGNode n: c) {
            set.add((SecurityNode) n);
        }

        return set;
    }

    /**
     * Wraps a backward slice into security nodes
     * @param startNode
     * @return
     */
    public HashSet<SecurityNode> forwardSlice(SecurityNode startNode) {
        HashSet<SecurityNode> set = new HashSet<SecurityNode>();
        LinkedList<SDGNode> criterion = new LinkedList<SDGNode>();

        criterion.add(startNode);

        Collection<SDGNode> c = sfor.slice(criterion);

        for (SDGNode n: c) {
            set.add((SecurityNode) n);
        }

        return set;
    }
}
