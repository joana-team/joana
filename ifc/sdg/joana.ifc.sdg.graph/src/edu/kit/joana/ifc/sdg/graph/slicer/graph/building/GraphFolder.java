/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.building;

import java.util.Set;
import java.util.List;
import java.util.LinkedList;

import org.jgrapht.alg.StrongConnectivityInspector;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CallGraph;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCallGraph;

/** A utility class with some methods to fold cycles in graphs.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class GraphFolder {
    /** Marks a fold node that folds a recursive call cycle. */
    public static final String FOLDED_CALL = "CALL";

    /** Marks a fold node that folds a recursive return cycle. */
    public static final String FOLDED_RETURN = "RETURN";

    /** Marks a fold node that folds both a call and a return cycle. */
    public static final String FOLDED_BOTH = "BOTH";

    /** Marks a fold node that folds an intraprocedural cycle. */
    public static final String FOLDED_LOOP = "LOOP";

    /** A utility class. */
    private GraphFolder() { }

    /** Uses Krinke's two-pass interprocedural SCC folding algorithm to fold
     * cycles due to recursion in ICFGs.
     * In the first pass, only cycles consisting of call edges and control flow
     * edges are folded, in the second pass, only cycles consisting of return
     * edges and control flow edges are folded.
     *
     * @param icfg  The ICFG to fold.
     * @return      A graph with folded cycles.
     */
    public static FoldedCFG twoPassFolding(CFG icfg){
        // the new graph
        CFG folded_icfg = new CFG();

        // copy vertices and edges into it
        Set<SDGNode> vertexes = icfg.vertexSet();

        for(SDGNode n : vertexes){
            folded_icfg.addVertex(n);
        }

        for(SDGEdge e : icfg.edgeSet()){
            folded_icfg.addEdge((SDGEdge) e.clone());
        }

        // provides the cycles for the first pass and the second pass
        KrinkeSCCInspector fbi = new KrinkeSCCInspector(folded_icfg);

        // ID's for the fold vertices
        int id = -1;


        // === first pass folding ===

        // cycles consisting of call edges and control flow edges only
        List<Set<SDGNode>> first = fbi.firstPass();
        LinkedList<Set<SDGNode>> compute = new LinkedList<Set<SDGNode>>();

        for (Set<SDGNode> scc : first) {
            if (scc.size() > 1) {
                compute.addFirst(scc);
            }
        }

        // fold these cycles
        id = folding(compute, id, folded_icfg);


        // === second pass folding ===

        // cycles consisting of return edges and control flow edges only
        List<Set<SDGNode>> second = fbi.secondPass();
        compute.clear();

        for (Set<SDGNode> scc : second) {
            if (scc.size() > 1) {
                compute.addFirst(scc);
            }
        }

        // fold these cycles
        id = folding(compute, id, folded_icfg);

        return new FoldedCFG(folded_icfg);
    }


    /** Folds cycles in call or entry graphs.
     *
     * @param call  A call graph
     * @return      A new folded call graph.
     */
    public static FoldedCallGraph foldCallGraphWithoutForkEdges(CallGraph call) {
        // new graph, copy edges and vertices into it
    	CallGraph slicing_call = new CallGraph();
        LinkedList<SDGEdge> forkEdges = new LinkedList<SDGEdge>();
        Set<SDGNode> vertices = call.vertexSet();

        for (SDGNode n : vertices) {
            slicing_call.addVertex(n);
        }

        for (SDGEdge e : call.edgeSet()) {
            if (e.getKind() == SDGEdge.Kind.FORK) {
                forkEdges.add(e);

            } else {
                slicing_call.addEdge((SDGEdge) e.clone());
            }
        }

        // compute SCC's
        StrongConnectivityInspector<SDGNode, SDGEdge> sci = new StrongConnectivityInspector<SDGNode, SDGEdge>(slicing_call);
        List<Set<SDGNode>> tmp = sci.stronglyConnectedSets();
        List<Set<SDGNode>> erg = new LinkedList<Set<SDGNode>>();

        for (Set<SDGNode> scc : tmp) {
        	if (scc.size() > 1) {
        		// only non-trivial scc's
        		erg.add(scc);

        	} else {
        		// add direct cycles
        		SDGNode n = scc.iterator().next();

        		for (SDGEdge e : call.outgoingEdgesOf(n)) {
        			if (e.getKind() == SDGEdge.Kind.CALL && e.getTarget() == n) {
        				erg.add(scc);
        				break;
        			}
        		}
        	}
        }

        slicing_call.addAllEdges(forkEdges);

        // fold SCC's
        folding(erg, -1, slicing_call);

        return new FoldedCallGraph(slicing_call);
    }

    /** Folds cycles in call or entry graphs.
    *
    * @param call  A call graph
    * @return      A new folded call graph.
    */
   public static FoldedCallGraph foldCallGraph(CallGraph call) {
       // new graph, copy edges and vertices into it
	   CallGraph slicing_call = new CallGraph();
       LinkedList<SDGEdge> forkEdges = new LinkedList<SDGEdge>();
       Set<SDGNode> vertices = call.vertexSet();

       for (SDGNode n : vertices) {
           slicing_call.addVertex(n);
       }

       for (SDGEdge e : call.edgeSet()) {
           slicing_call.addEdge((SDGEdge) e.clone());
       }

       // compute SCC's
       StrongConnectivityInspector<SDGNode, SDGEdge> sci = new StrongConnectivityInspector<SDGNode, SDGEdge>(slicing_call);
       List<Set<SDGNode>> tmp = sci.stronglyConnectedSets();
       List<Set<SDGNode>> erg = new LinkedList<Set<SDGNode>>();

       for (Set<SDGNode> scc : tmp) {
       	if (scc.size() > 1) {
       		// only non-trivial scc's
       		erg.add(scc);

       	} else {
       		// add direct cycles
       		SDGNode n = scc.iterator().next();

       		for (SDGEdge e : call.outgoingEdgesOf(n)) {
       			if ((e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.FORK) && e.getTarget() == n) {
       				erg.add(scc);
       				break;
       			}
       		}
       	}
       }

       slicing_call.addAllEdges(forkEdges);

       // fold SCC's
       folding(erg, -1, slicing_call);

       return new FoldedCallGraph(slicing_call);
   }

    /** Folds cycles in call graphs.
     *
     * @param g     A call graph
     * @return      A new folded call graph.
     */
    public static FoldedCFG foldCallGraphWithoutForkEdges(CFG g) {
         // new graph, copy edges and vertices into it
    	CFG slicing_call = new CFG();
        LinkedList<SDGEdge> forkEdges = new LinkedList<SDGEdge>();
        Set<SDGNode> vertices = g.vertexSet();

        for (SDGNode n : vertices) {
            slicing_call.addVertex(n);
        }

        for (SDGEdge e : g.edgeSet()) {
            if (e.getKind() == SDGEdge.Kind.FORK) {
                forkEdges.add(e);

            } else {
                slicing_call.addEdge((SDGEdge) e.clone());
            }
        }

        // compute SCC's
        StrongConnectivityInspector<SDGNode, SDGEdge> sci = new StrongConnectivityInspector<SDGNode, SDGEdge>(slicing_call);
        List<Set<SDGNode>> tmp = sci.stronglyConnectedSets();
        List<Set<SDGNode>> erg = new LinkedList<Set<SDGNode>>();

        for (Set<SDGNode> scc : tmp) {
        	if (scc.size() > 1) {
        		// only non-trivial scc's
        		erg.add(scc);
        	}
        }

        slicing_call.addAllEdges(forkEdges);

        // fold SCC's
        folding(erg, -1, slicing_call);

        return new FoldedCFG(slicing_call);
    }


    /** Folds SCC's in a graph.
     * The given graph is modified, no new graph is constructed!
     * The fold nodes are represented through SDGNode objects of kind FOLDED.
     * They are new inserted SDGNode nodes with all integer attributes set at -1,
     * ecxept for their ID.
     * The ID for the first fold node must be given as a parameter. It is decreased
     * for every following fold node so negative numbers should be used as ID.
     * The folded nodes are not removed but all their edges are deflected to the
     * corresponding fold node. They are connected with the fold node via SDGEdges
     * of type FOLD_INCLUDE with the fold node as target.
     * Note that all other edges are not SDGEdges.
     *
     * @param sccs   The cycles to fold.
     * @param id     The ID the enumeration of the fold vertices shall start.
     * @param graph  The graph to fold.
     * @return       The last used ID.
     */
    private static int folding(List<Set<SDGNode>> sccs, int id, JoanaGraph graph) {
        LinkedList<SDGEdge> to_remove = new LinkedList<SDGEdge>();
        LinkedList<SDGEdge> to_add = new LinkedList<SDGEdge>();
        LinkedList<SDGNode> fold_nodes = new LinkedList<SDGNode>();

        /* iterate all SCCs and fold every SCC with more than one element */
        for(Set<SDGNode> scc : sccs){
            // process only SCCs resulting from recursions
            // create new fold node
            int type = foldType(scc, graph);
            SDGNode fold = new SDGNode(SDGNode.Kind.FOLDED, id, -1);

            switch (type) {
                case 1: fold.setLabel(FOLDED_CALL);
                        break;

                case 2: fold.setLabel(FOLDED_RETURN);
                        break;

                case 3: fold.setLabel(FOLDED_BOTH);
                        break;

                case 4: fold.setLabel(FOLDED_LOOP);
                        break;
            }

            id--;
            fold_nodes.addFirst(fold);

            // for every node of the SCC create a fold-include edge to the fold node//
            for(SDGNode folded : scc){
                SDGEdge fi = new SDGEdge(folded, fold, SDGEdge.Kind.FOLD_INCLUDE);

                // new fold-include edge
                to_add.addFirst(fi);

                // remove its outgoing edges and deflect the ones leaving the SCC to the fold node
                for(SDGEdge e : graph.outgoingEdgesOf(folded)){
                    if(!scc.contains(e.getTarget())){
                        // deflect edge
                        SDGEdge deflect = new SDGEdge(fold, e.getTarget(), e.getKind());
                        if (!exists(deflect, to_add)) {
                            to_add.addFirst(deflect);
                        }
                    }

                    // memorise edge for removing
                    to_remove.addFirst(e);
                }

                // remove its incoming edges and deflect the ones
                // arriving from outside of the SCC to the fold node
                for(SDGEdge e : graph.incomingEdgesOf(folded)){
                    if(!scc.contains(e.getSource())){
                        if (e.getKind() == SDGEdge.Kind.FOLD_INCLUDE) {
                            SDGEdge deflect = new SDGEdge(e.getSource(), fold, SDGEdge.Kind.FOLD_INCLUDE);
                             if (!exists(deflect, to_add)) {
                                to_add.addFirst(deflect);
                            }
                        } else{
                            // deflect edge
                            SDGEdge deflect = new SDGEdge(e.getSource(), fold, e.getKind());
                            if (!exists(deflect, to_add)) {
                                to_add.addFirst(deflect);
                            }
                        }

                        // memorise edge for removing
                        to_remove.addFirst(e);
                    }
                }
            }

            // modify graph
            while(!to_remove.isEmpty()){
                // remove edges
                graph.removeEdge(to_remove.poll());
            }

            while(!fold_nodes.isEmpty()){
                // add folded nodes
                graph.addVertex(fold_nodes.poll());
            }

            while(!to_add.isEmpty()){
                // add edges
                graph.addEdge(to_add.poll());
            }
        }

        return id;
    }

    /**
     *  1: folded call
     *  2: folded return
     *  3: folded call and return
     *  4: folded loop
     */
    private static int foldType(Set<SDGNode> fold, JoanaGraph graph) {
        boolean call = false;
        boolean ret = false;

        for (SDGNode toFold : fold) {
            for (SDGEdge e : graph.outgoingEdgesOf(toFold)) {
                if (e.getKind() == SDGEdge.Kind.CALL && fold.contains(e.getTarget())) {
                    call = true;

                } else if (e.getKind() == SDGEdge.Kind.RETURN && fold.contains(e.getTarget())) {
                    ret = true;
                }
            }

            if (toFold.getKind() == SDGNode.Kind.FOLDED) {
                if (toFold.getLabel() == FOLDED_CALL) {
                    call = true;

                } else if (toFold.getLabel() == FOLDED_RETURN) {
                    ret = true;

                } else if (toFold.getLabel() == FOLDED_BOTH) {
                    call = true;
                    ret = true;
                }
            }
        }

        if (call && ret) {
            return 3;

        } else if (call) {
            return 1;

        } else if (ret) {
            return 2;
        }

        return 4;
    }

    /** Checks whether a given list contains a copy of a given edge.
     * For that purpose it compares the sources and targets of the edges.
     *
     * @param edge  An edge whose copies are searched.
     * @param list  A list of edges.
     */
    private static boolean exists(SDGEdge edge, LinkedList<SDGEdge> list){
        for (SDGEdge e : list) {
            if (e.getSource() == edge.getSource() && e.getTarget() == edge.getTarget()) {
                return true;
            }
        }
        return false;
    }


    /* intra-procedural folding */

    /** Folds loop-based cycles in a given graph.
     * It strictly avoids interprocedural edges, so the resulting fold vertices
     * are intraprocedural.
     *
     * @param graph  The graph whose loops shall be fold.
     * @return       A new graph with folded loops.
     */
    public static FoldedCFG foldIntraproceduralSCC(CFG graph) {
        LinkedList<SDGEdge> edges = new LinkedList<SDGEdge>();

        // remove all return and call edges
        for (SDGEdge e : graph.edgeSet()) {
            if (e.getKind() == SDGEdge.Kind.RETURN ||
                    e.getKind() == SDGEdge.Kind.CALL) {

                edges.addFirst(e);
            }
        }

        for (SDGEdge e : edges) {
            graph.removeEdge(e);
        }

        FoldedCFG folded = foldSCC(graph);

        // put the edges back to both graphs
        for (SDGEdge e : edges) {
            graph.addEdge(e);
            folded.addEdge(e);
        }

        return folded;
    }

    /** Folds any SCC in a given graph.
     *
     * @param graph  The graph to fold.
     * @return       A new graph with folded SCC's.
     */
	public static FoldedCFG foldSCC(CFG graph) {
        List<Set<SDGNode>> allSCCs = new LinkedList<Set<SDGNode>>();
        StrongConnectivityInspector<SDGNode, SDGEdge> sci = null;

        // construct new graph by copying edges and vertices
        CFG newGraph = new CFG();
        Set<SDGNode> vertexes = graph.vertexSet();
        int index = -1;

        for (SDGNode n : vertexes) {
            newGraph.addVertex(n);

            // index analysis
            if (n.getId() <= index) {
                index = n.getId() -1;
            }
        }

        for (SDGEdge e : graph.edgeSet()) {
            newGraph.addEdge((SDGEdge) e.clone());
        }

        // determine all SCC's of 'newGraph'
        sci = new StrongConnectivityInspector<SDGNode, SDGEdge>(newGraph);
        List<Set<SDGNode>> tmp = sci.stronglyConnectedSets();

        for (Set<SDGNode> scc : tmp) {
        	if (scc.size() > 1) {
        		// only non-trivial scc's
        		allSCCs.add(scc);
        	}
        }

        // fold them
        folding(allSCCs, index, newGraph);

        return new FoldedCFG(newGraph);
    }
}
