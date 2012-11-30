/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.regions;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.SimpleMHPAnalysis;

/** An implementation of Nanda's slicer for multithreaded Java programs.
 * It uses an optimization to omit reachability analysis after traversing
 * intra-procedural edges
 *
 * -- Created on September 29, 2005
 *
 * @author  Dennis Giffhorn
 */
public class NandaFlat extends Nanda {

    /** Creates a new instance of this algorithm.
     * @param graph  A SDG.
     */
    public NandaFlat(SDG graph, NandaMode mode) {
        super(graph, mode);
    }

    protected MHPAnalysis mhp() {
    	return SimpleMHPAnalysis.analyze(this.graph);
    }
}
