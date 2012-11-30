/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CallGraph;


/**
 * This slicer slices CallGraph's. Employable for simple reachability analyses.
 *
 * @author grafj
 */
public abstract class CallGraphSlicer {

	protected CallGraph g;

    /**
     * Creates a new instance of ContextInsensitiveSlicer
     */
    protected CallGraphSlicer(CallGraph graph, Set<SDGEdge.Kind> omit) {
        this.g = graph;
    	throw new UnsupportedOperationException("Omitting edges not supported. A callgraph contains only one type of edges.");
    }

    /**
     * Creates a new instance of ContextInsensitiveSlicer
     */
    protected CallGraphSlicer(CallGraph graph) {
        this.g = graph;
    }

    public void setGraph(CallGraph graph) {
        g = graph;
    }

    public Collection<SDGNode> slice(SDGNode criterion) {
    	return slice(Collections.singleton(criterion));
    }

    public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
    	HashSet<SDGNode> slice = new HashSet<SDGNode>();
    	LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
    	worklist.addAll(criteria);

    	for (SDGNode node : criteria) {
    		slice.add(node);
    	}

    	while (!worklist.isEmpty()) {
    		SDGNode w = worklist.poll();

    		for (SDGEdge e : edgesToTraverse(w)) {
    			if (e.getKind() != SDGEdge.Kind.CALL)
    				continue;

    			SDGNode v = reachedNode(e);

    			if (slice.add(v)) {
    				worklist.addFirst(v);
    			}
    		}
    	}

    	return slice;
    }

    public Collection<SDGNode> slice(Collection<SDGNode> criteria, Collection<SDGNode> subgraph) {
    	HashSet<SDGNode> slice = new HashSet<SDGNode>();
    	LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
    	worklist.addAll(criteria);

    	for (SDGNode node : criteria) {
    		slice.add(node);
    	}

    	while (!worklist.isEmpty()) {
    		SDGNode w = worklist.poll();

    		for (SDGEdge e : edgesToTraverse(w)) {
    			if (e.getKind() != SDGEdge.Kind.CALL)
    				continue;

    			SDGNode v = reachedNode(e);

    			if (!subgraph.contains(v)) continue;

    			if (slice.add(v)) {
    				worklist.addFirst(v);
    			}
    		}
    	}

    	return slice;
    }

    public void setOmittedEdges(Set<SDGEdge.Kind> omit){
    	throw new UnsupportedOperationException();
    }

    protected abstract Collection<SDGEdge> edgesToTraverse(SDGNode node);

    protected abstract SDGNode reachedNode(SDGEdge edge);

}
