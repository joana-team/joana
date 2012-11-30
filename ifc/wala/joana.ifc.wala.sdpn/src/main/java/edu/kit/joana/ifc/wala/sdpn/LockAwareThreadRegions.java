/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.wala.sdpn;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegions;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class LockAwareThreadRegions extends ThreadRegions {

	public LockAwareThreadRegions(List<ThreadRegion> regions, CFG icfg, TIntObjectHashMap<TIntObjectHashMap<ThreadRegion>> map) {
		super(regions, icfg, map);
	}

	public static ThreadRegions compute(SDG sdg) {
		ThreadsInformation info = sdg.getThreadsInfo();
        CFG icfg = ICFGBuilder.extractICFG(sdg);
    	//List<SDGEdge> syntheticEdges =
        PreciseMHPAnalysis.removeSyntheticEdges(icfg);
        LockAwareThreadRegionBuilder builder = new LockAwareThreadRegionBuilder(icfg, info);
        ThreadRegions tr = builder.computeRegions();
        //icfg.addAllEdges(syntheticEdges);

        return tr;
	}

}
