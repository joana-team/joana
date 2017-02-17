/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;


/**  This class represents ISCR graphs as explained in Nanda's PhD thesis.
 * Basically, it's a folded ICFG with a virtual inlining of procedures using
 * topological enumeration of the vertices.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class ISCRGraph {
	private final CFG data;
    private final Map<SDGNode, List<SDGNode>> nodeMap; // maps SDGNode -> ISCR nodes
    private final Map<SDGNode, SDGNode> params;
    private final int[] threads;
    private final SDGNode root;

    /** Creates a new instance of ISCRGraph.
     * @param data  The raw graph.
     */
    public ISCRGraph(CFG data, int[] threads, SDGNode root,
    		Map<SDGNode, List<SDGNode>> nodeMap,
    		HashMap<SDGNode, SDGNode> params) {
        this.data = data;
        this.nodeMap = nodeMap;
        this.threads = threads;
        this.root = root;
        this.params = params;
    }

    public SDGNode getRoot() {
    	return root;
    }

    public int[] getThreads() {
    	return threads;
    }

    public Set<SDGEdge> incomingEdgesOf(SDGNode n) {
    	return data.incomingEdgesOf(n);
    }

    public Set<SDGEdge> outgoingEdgesOf(SDGNode n) {
    	return data.outgoingEdgesOf(n);
    }

    public Set<SDGEdge> edgeSet() {
    	return data.edgeSet();
    }

    /** Checks whether a given vertex has outgoing edges of a given kind.
     *
     * @param node
     *              The vertex to check.
     * @param kind
     *              The demanded kind of edges.
     */
    public boolean hasOutgoingEdgesOfKind(SDGNode node, SDGEdge.Kind kind) {
    	for (SDGEdge e : outgoingEdgesOf(node)) {
    		if (e.getKind() == kind) {
    			return true;
    		}
    	}

    	return false;
    }

    /** Returns a list with all outgoing edges of a given kind of a given vertex.
     *
     * @param node
     *              The vertex whose edges are needed.
     * @param kind
     *              The demanded kind of edges.
     */
    public List<SDGEdge> getOutgoingEdgesOfKind(SDGNode node, SDGEdge.Kind kind) {
    	LinkedList<SDGEdge> res = new LinkedList<SDGEdge>();

    	for (SDGEdge e : outgoingEdgesOf(node)) {
    		if (e.getKind() == kind) {
    			res.add(e);
    		}
    	}

    	return res;
    }

    /** Checks whether a given vertex has incoming edges of a given kind.
     *
     * @param node
     *              The vertex to check.
     * @param kind
     *              The demanded kind of edges.
     */
    public boolean hasIncomingEdgesOfKind(SDGNode node, SDGEdge.Kind kind) {
    	for (SDGEdge e : incomingEdgesOf(node)) {
    		if (e.getKind() == kind) {
    			return true;
    		}
    	}

    	return false;
    }

    /** Returns a list with all incoming edges of a given kind of a given vertex.
     *
     * @param node
     *              The vertex whose edges are needed.
     * @param kind
     *              The demanded kind of edges.
     */
    public List<SDGEdge> getIncomingEdgesOfKind(SDGNode node, SDGEdge.Kind kind) {
    	LinkedList<SDGEdge> res = new LinkedList<SDGEdge>();

    	for (SDGEdge e : incomingEdgesOf(node)) {
    		if (e.getKind() == kind) {
    			res.add(e);
    		}
    	}

    	return res;
    }

    public boolean addEdge(SDGEdge edge) {
    	return data.addEdge(edge);
    }

    /**
     * This method serves only for debugging purposes.
     * DON'T USE IT unless you fully understood the structure of ISCR graphs and the structure of this implementation.
     */
    boolean containsNode(SDGNode node) {
        return data.containsVertex(node);
    }

    public boolean addVertex(SDGNode node) {
    	return data.addVertex(node);
    }

    public Set<SDGNode> vertexSet() {
    	return data.vertexSet();
    }

    /**
     * Maps an original ICFG node to the ISCR nodes by which it is represented.
     *
     * @param node  A ICFG node.
     * @return  A List of ISCR nodes.
     */
    public List<SDGNode> getISCRNodes(SDGNode node) {
    	SDGNode n = mapParamNode(node);
    	return nodeMap.get(n);
    }

    private SDGNode mapParamNode(SDGNode node) {
    	SDGNode n = params.get(node);
    	return (n == null ? node : n);
    }
}
