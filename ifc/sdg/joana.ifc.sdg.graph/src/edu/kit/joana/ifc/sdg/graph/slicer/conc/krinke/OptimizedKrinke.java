/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.krinke;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.TruncatedNonSameLevelChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;


/** This class realises Krinke's optimised algorithm for threaded interprocedural slicing.
 *
 * -- Created on October 7, 2005
 *
 * @author  Dennis Giffhorn
 * @deprecated
 */
@Deprecated
public class OptimizedKrinke extends Krinke implements Slicer {
    /** An intrathreadual summary slicer. */
    private Context2PhaseSlicer<DynamicContext> contextSlicer;
    /** A truncated non-same-level chopper. */
    private TruncatedNonSameLevelChopper truncated;

    public OptimizedKrinke() { }

    /** Creates a new instance of Krinke's optimized algorithm.
     *
     * @param graph A threaded interprocedural program dependencies graph that shall be sliced.
     *              It has to contain control flow edges.
     *
     */
    public OptimizedKrinke(SDG graph) {
        super(graph);
    	init(graph);
    }

    /** Initializes the fields and calls super.setGraph(graph).
     * @param graph  A SDG.
     */
    public void setGraph(SDG graph) {
        super.setGraph(graph);
        init(graph);
    }

    /** Initialises the fields of the Krinke slicer.
     */
    private void init(SDG graph) {
        // the used 2-phase slicer
        contextSlicer = new Context2PhaseSlicer<>(graph, this.man);

        // a truncated non-same-level chopper
        truncated = new TruncatedNonSameLevelChopper(graph);
    }

    /** Computes the inter-threadual slice for a WorklistElement and returns the encountered
     * Contexts having incoming inter-threadual edges.
     * Uses Krinke's chop + bounded slice - optimization.
     *
     * @param next  The slicing criterion.
     * @param slice  Containing the computed slice.
     * @return  A set of WorklistElements, representing Contexts with incoming inter-threadual edges.
     */
    protected Collection<WorklistElement<DynamicContext>> threadSlice(WorklistElement<DynamicContext> next, HashSet<SDGNode> slice) {
        // perform a 2-phase slice
        Collection<SDGNode> interferingNodes = contextSlicer.slice(Collections.singleton(next.getContext()), slice);

        // truncated non-same-level chop
        Collection<SDGNode> crit = Collections.singleton(next.getNode());
        Collection<SDGNode> chop = truncated.chop(interferingNodes, crit);

        // compute explicitly context-sensitive slice on chop result
        Collection<WorklistElement<DynamicContext>> withInterference = slicer.subGraphSlice(next, chop);

        return withInterference;
    }
}
