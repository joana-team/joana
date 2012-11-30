/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGForward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGSlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.BitMatrix;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;


public class ThreadRegionsReach {
    private BitMatrix map;

    private ThreadRegionsReach(BitMatrix map) {
    	this.map = map;
    }

    public boolean reaches(int from, int to) {
    	return map.get(from, to);
    }


    /* FACTORY */

    public static ThreadRegionsReach create(Collection<ThreadRegion> regions, SDG sdg) {
    	BitMatrix map = new BitMatrix(regions.size());
        CFG cfg = ICFGBuilder.extractICFG(sdg);
        CFGSlicer slicer = new CFGForward(cfg);

        for (ThreadRegion r : regions) {
        	Collection<SDGNode> s = slicer.slice(r.getStart());

        	for (ThreadRegion q : regions) {
        		if (s.contains(q.getStart())) {
        			map.set(r.getID(), q.getID());
        		}
        	}
        }

        return new ThreadRegionsReach(map);
    }
}
