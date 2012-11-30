/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda;

import java.util.Collection;
import java.util.Iterator;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;


/**
 * Distinction between forward and backward slice.
 *
 * @author giffhorn
 *
 */
public interface NandaMode {
	public void init(ContextGraphs contextGraphs, MHPAnalysis mhp, SDG graph);

    public SummarySlicer initSummarySlicer(SDG g);

    public boolean restrictiveTest(TopologicalNumber actual, TopologicalNumber previous, int thread);

    public Iterator<TopologicalNumber> reachingContexts(SDGNode reached, int reachedThread, TopologicalNumber state);

    public Iterator<TopologicalNumber> getTopologicalNumbers(SDGNode node, int thread);

    public VirtualNode initialState(int thread);

    public Nanda.Treatment phase1Treatment(SDGEdge edge);

    public Nanda.Treatment phase2Treatment(SDGEdge edge);

    public Collection<SDGEdge> getEdges(SDGNode node);

    public SDGNode adjacentNode(SDGEdge edge);

    public Iterator<TopologicalNumber> intraproceduralNeighbours(SDGNode neighbourNode, TopologicalNumber tnr, int thread);

    public Iterator<TopologicalNumber> interproceduralNeighbours(SDGNode neighbourNode, TopologicalNumber from, int thread);

    public Collection<TopologicalNumber> getForkJoin(SDGEdge edge, TopologicalNumber from, int fromThread);
}
