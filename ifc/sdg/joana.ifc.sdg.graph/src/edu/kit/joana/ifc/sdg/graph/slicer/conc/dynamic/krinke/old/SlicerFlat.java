/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.old;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegions;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;

/**
 * This class realises Krinke's optimized Algorithm for threaded interprocedural slicing.
 * It uses Krinke's model of concurrency.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class SlicerFlat extends Slicer implements edu.kit.joana.ifc.sdg.graph.slicer.Slicer {
    /**
     * Creates a new instance of this slicer.
     *
     * @param graph A threaded interprocedural program dependencies graph that shall be sliced.
     *              It has to contain control flow edges.
     */
    public SlicerFlat(SDG graph) {
        super(graph);
    }

    /** Computes the thread regions of the given ICFG.
     * It uses Krinke's model of concurrency: 1 thread region per thread.
     * @param icfg  The ICG
     * @param foldedIcfg  The ICFG with cycles folded.
     * @param threadsinfo  Informations about the threads of the graph.
     * @return The thread regions.
     */
    protected ThreadRegions threadRegions(CFG icfg, FoldedCFG foldedIcfg, ThreadsInformation threadsinfo){
        return ThreadRegions.allThreadsParallel(icfg, threadsinfo);
    }
}
