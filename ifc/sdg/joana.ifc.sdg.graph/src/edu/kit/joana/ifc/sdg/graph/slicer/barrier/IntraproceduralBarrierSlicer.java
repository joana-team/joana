/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.barrier;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/** An intra-procedural barrier slicer.
 * A barrier slicer receives a set of nodes - the barrier - which it will not trespass.
 *
 * @author Dennis Giffhorn
 */
public abstract class IntraproceduralBarrierSlicer implements BarrierSlicer {
    protected SDG g;
    private Collection<SDGEdge.Kind> omit;
    private BarrierManager barrier;

    protected abstract Collection<SDGEdge> edgesToTraverse(SDGNode node);

    protected abstract SDGNode reachedNode(SDGEdge edge);

    public IntraproceduralBarrierSlicer(SDG graph) {
    	omit = Collections.emptySet();
    	barrier = new BarrierManager();
        setGraph(graph);
    }

    public void setGraph(SDG graph) {
        g = graph;
    }

    public void setOmittedEdges(Set<SDGEdge.Kind> omit){
        this.omit = omit;
    }

	public void setBarrier(Collection<SDGNode> barrier) {
		this.barrier.setBarrier(g, barrier);
	}

	public void setBarrier(Collection<SDGNode> barrier, Collection<SDGEdge> blockedSummaryEdges) {
		this.barrier.setBarrier(barrier, blockedSummaryEdges);
	}

	public void setBarrier(BarrierManager barrier) {
		this.barrier = barrier;
	}

	public Collection<SDGNode> slice(SDGNode criterion) {
		return slice(Collections.singleton(criterion));
	}

    public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
    	HashSet<SDGNode> slice = new HashSet<SDGNode>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();

        worklist.addAll(criteria);
        slice.addAll(criteria);

        while (!worklist.isEmpty()) {
            SDGNode w = worklist.poll();

            for (SDGEdge e : edgesToTraverse(w)) {
            	// keep intra-procedural and don't traverse blocked summary edges
            	if (!e.getKind().isSDGEdge()
            			|| !e.getKind().isIntraproceduralEdge()
            			|| omit.contains(e.getKind())
                        || barrier.isBlocked(e)){

                    continue;
                }

                SDGNode v = reachedNode(e);

            	// don't trespass the barrier
            	if (barrier.isBlocked(v)) continue;

                // only visit fresh nodes
                if (slice.add(v)) {
                	worklist.add(v);
                }
            }
        }

        return slice;
    }
}
