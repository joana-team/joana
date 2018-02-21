/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.building;

import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CallGraph;


/** Constructs call graphs of graphs containing an ICFG.
 * It can build call graphs and bipartite call graphs.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public final class CallGraphBuilder {

    /** A utility class. */
    private CallGraphBuilder() {}

    /** Extracts the bipartite call graph of a given graph that contains an ICFG.
     * The resulting graph consists of call and entry nodes and connecting edges
     * summarising their dependencies in the given graph. Edges starting at a call
     * node are labelled as call edges, edges starting at entry nodes are labelled
     * as control flow edges.
     *
     * @param graph  A graph containing an ICFG.
     * @return       The bipartite call graph.
     */
    public static CFG buildBipartiteCallGraph(SDG graph) {
    	CFG call = new CFG();
        call.setName(graph.getName());
        Set<SDGNode> vertexes = graph.vertexSet();

        // traverse the vertexes and include entry and call nodes in the call graph
        for(SDGNode n : vertexes){
            if(n.getKind() == SDGNode.Kind.ENTRY || n.getKind() == SDGNode.Kind.CALL){
                call.addVertex(n);
            }
        }
        
        call.setRoot(graph.getRoot());

        // adopt call and fork edges
        for(SDGEdge e : graph.edgeSet()){
            if(e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.FORK){
                call.addEdge(e.getKind().newEdge(e.getSource(), e.getTarget()));
            }
        }

        // edges btwn. call nodes and corresponding entry node
        // they have the same procedure ID
        Set<SDGNode> s = call.vertexSet();
        for(SDGNode n : s){
            if(n.getKind() == SDGNode.Kind.ENTRY){
                for(SDGNode target : s){
                    if(target.getKind() == SDGNode.Kind.CALL && target.getProc() == n.getProc()){
                        // 'target' is call node in 'n's procedure
                        call.addEdge( SDGEdge.Kind.CONTROL_FLOW.newEdge(n, target));
                    }
                }
            }
        }

        return call;
    }

    /** Extracts the call graph of a given graph that contains an ICFG.
     * The resulting graph consists of entry nodes and call edges
     * summarizing their dependencies in the given graph.
     *
     * @param graph  A graph containing an ICFG.
     * @return       The call graph.
     */
    public static CFG buildEntryGraph(JoanaGraph graph) {
        CFG call = new CFG();
        Set<SDGNode> vertexes = graph.vertexSet();

        // traverse the vertexes and add all entry nodes to the call graph
        for(SDGNode n : vertexes){
            if(n.getKind() == SDGNode.Kind.ENTRY){
                call.addVertex(n);
            }
        }
        
        call.setRoot(graph.getRoot());

        // construct edges btwn. the entry nodes
        LinkedList<SDGEdge> edges = new LinkedList<SDGEdge>();
        Set<SDGNode> entries = call.vertexSet();

        for (SDGNode n : entries) {
            for (SDGEdge e : graph.incomingEdgesOf(n)) {
                if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.FORK) {
                    int callingProc = e.getSource().getProc();

                    // search entry node of calling procedure
                    for (SDGNode entry : entries) {
                        if (entry.getProc() == callingProc) {

                            // construct call edge and add it to edges list
                            SDGEdge callEdge = e.getKind().newEdge(entry, n);
                            edges.addFirst(callEdge);
                        }
                    }
                }
            }
        }

        // add all call edges
        for (SDGEdge e : edges) {
            call.addEdge(e);
        }

        return call;
    }

    /** Extracts the call graph of a given graph that contains an ICFG.
     * The resulting graph consists of call nodes and call edges
     * summarizing their dependencies in the given graph.
     *
     * @param graph  A graph containing an ICFG.
     * @return       The call graph.
     */
    public static CallGraph buildCallGraph(JoanaGraph graph) {
    	CallGraph call = new CallGraph();
        Set<SDGNode> vertexes = graph.vertexSet();

        // traverse the vertexes and add all entry nodes to the call graph
        for(SDGNode n : vertexes){
            if(n.getKind() == SDGNode.Kind.CALL){
                call.addVertex(n);
            }
        }

        // construct call edges btwn. the call nodes
        LinkedList<SDGEdge> edges = new LinkedList<SDGEdge>();
        Set<SDGNode> calls = call.vertexSet();

        for (SDGNode n : calls) {
            for (SDGEdge e : graph.outgoingEdgesOf(n)) {
                if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.FORK) {
                    int callingProc = e.getTarget().getProc();

                    // search call nodes in the called procedure
                    for (SDGNode c : calls) {
                        if (c.getProc() == callingProc) {
                            // construct call edge and add it to edges list
                            SDGEdge callEdge = e.getKind().newEdge(n, c);
                            edges.addFirst(callEdge);
                        }
                    }
                }
            }
        }

        // add all call edges
        for (SDGEdge e : edges) {
            call.addEdge(e);
        }

//        // add convenience root node
//        SDGNode root = graph.getNode(1);
//        call.addRoot(root);
//        for (SDGNode c : calls) {
//        	if (c.getProc() == root.getProc() && c != root) {
//        		SDGEdge callEdge =  SDGEdge.Kind.CALL.newEdge(root, c);
//        		call.addEdge(callEdge);
//        	}
//        }

        return call;
    }
}
