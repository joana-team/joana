/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.building;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import gnu.trove.set.hash.TIntHashSet;



/** This utility class offers various methods for modifying graphs.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public final class GraphModifier {

    /** A utility class. */
    private GraphModifier() { }

    /** This method adds return edges to an incomplete ICFG without return edges.
     *
     * @param icfg  An ICFG without return edges.
     */
    public static void addReturnEdgesTo(JoanaGraph sdg){
        Set<SDGNode> set = sdg.vertexSet();

        // search for call vertices
        for (SDGNode n : set) {
            if (n.getKind() == SDGNode.Kind.CALL) {
                SDGNode exit = null;

                // compute exit vertex of called procedure
                for (SDGEdge e : sdg.outgoingEdgesOf(n)) {

                    // don't consider thread calls
                    if (e.getKind() == SDGEdge.Kind.CALL) {
                                final SDGNode entry = e.getTarget();
                                exit = sdg.getExit(entry);

                                // compute return nodes and add return edges (with 'exit' as source)
                                for (SDGEdge edge : sdg.outgoingEdgesOf(n)) {
                                    if (edge.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
                                        SDGEdge ret =  new SDGEdge(exit, edge.getTarget(), SDGEdge.Kind.RETURN);
                                        sdg.addEdge(ret);
                                    }
                                }
                    }
                }
            }
        }
    }

    /** This method adds virtual join vertices to a threaded ICFG.
     * A join vertex is positioned directly after some call of the
     * 'java.lang.Thread.join()' method.
     * This position is considered to be the point where the joined thread is
     * extincted.
     * So far, the method doesn't compute join edges!
     *
     * @param icfg     A threaded ICFG without join vertices.
     * @param idStart  The ID the join vertices' enumeration shall start with.
     */
    public static void addJoinNodesTo(CFG icfg, int idStart) {
        LinkedList<SDGNode> joinCalls = new LinkedList<SDGNode>();

        // get join calls
        for (SDGNode node : icfg.vertexSet()) {
            if (node.getKind() == SDGNode.Kind.CALL) {
                if(node.getLabel().equals("java.lang.Thread.join")) {
                    joinCalls.addFirst(node);
                }
            }
        }

        // add new nodes
        for (SDGNode call : joinCalls) {
            SDGNode join = new SDGNode(SDGNode.Kind.JOIN, idStart, call.getProc());
            icfg.addVertex(join);

            // deflect edges
            LinkedList<SDGEdge> remove = new LinkedList<SDGEdge>();
            LinkedList<SDGEdge> add = new LinkedList<SDGEdge>();

            for (SDGEdge e : icfg.outgoingEdgesOf(call)) {
                if (SDGEdge.Kind.CALL != e.getKind()) {
                    SDGEdge deflect = new SDGEdge(join, e.getTarget(), e.getKind());
                    add.addFirst(deflect);
                    remove.addFirst(e);
                }
            }

            // remove deflected edges
            for (SDGEdge e : remove) {
                icfg.removeEdge(e);
            }

            // add deflect edges
            for (SDGEdge e : add) {
                icfg.addEdge(e);
            }

            // add edge btwn. call and join
            SDGEdge edge = new SDGEdge(call, join, SDGEdge.Kind.CONTROL_FLOW);
            icfg.addEdge(edge);

            idStart++;
        }
    }

//    /** Removes control flow edges between entry and exit vertices from a given ICFG.
//     *
//     * @param icfg  An ICFG.
//     */
//    public static void removeRedundantControlFlow(CFG icfg){
//        Set<SDGNode> set = icfg.vertexSet();
//
//        for (SDGNode n : set) {
//            if (n.getKind() == SDGNode.Kind.EXIT) {
//                int cfs = 0;
//                SDGEdge remove = null;
//
//                for (SDGEdge cf : icfg.incomingEdgesOf(n)) {
//                    if (cf.getKind() == SDGEdge.Kind.CONTROL_FLOW){
//                        cfs++;
//                        if (cf.getSource().getKind() == SDGNode.Kind.ENTRY) {
//                            remove = cf;
//                        }
//                    }
//                }
//
//                if (cfs > 1 && remove != null) {
//                    icfg.removeEdge(remove);
//                }
//            }
//        }
//
//    }

//    /** Removes compound vertices off a given graph.
//     *
//     * @param ipdg  A Graph.
//     */
//    public static void removeCompoundVertices(SDG ipdg) {
//        LinkedList<SDGNode> compounds = new LinkedList<SDGNode>();
//        LinkedList<SDGEdge> remove = new LinkedList<SDGEdge>();
//        LinkedList<SDGEdge> add = new LinkedList<SDGEdge>();
//
//        // deflect the edges
//        for (SDGNode n : ipdg.vertexSet()) {
//            if (n.getOperation() == SDGNode.Operation.COMPOUND) {
//                for (SDGEdge e : ipdg.incomingEdgesOf(n)) {
//                    SDGNode source = e.getSource();
//
//                    for (SDGEdge out : ipdg.outgoingEdgesOf(n)) {
//                        SDGNode target = out.getTarget();
//
//                        add.add(new SDGEdge(source, target, out.getKind()));
//                        remove.add(out);
//                    }
//
//                    compounds.add(n);
//                    remove.add(e);
//                    break;
//                }
//            }
//        }
//
//        // remove/add edges and vertices
//        for (SDGEdge e : add) {
//            ipdg.addEdge(e);
//        }
//
//        for (SDGEdge e : remove) {
//            ipdg.removeEdge(e);
//        }
//
//        for (SDGNode n : compounds) {
//            ipdg.removeVertex(n);
//        }
//    }

    /** Inserts dummy return sites into a given ICFG.
     * A dummy return site is a vertex inserted directly behind a call vertex,
     * recieving the return edge corresponding to the call vertex' call edge.
     * The method needs an integer which it uses as a start value for enumerating
     * the created return vertices.
     *
     * @param icfg     An ICFG that needs dummy return sites.
     * @param idStart  The start value for the enumeration of the return sites.
     */
    public static void dummyReturnSites(CFG icfg, int idStart) {
        Set<SDGNode> nodes = icfg.vertexSet();
        LinkedList<SDGNode> dummies = new LinkedList<SDGNode>();
        LinkedList<SDGEdge> remove = new LinkedList<SDGEdge>();
        LinkedList<SDGEdge> add = new LinkedList<SDGEdge>();

        // add dummy return sites
        for (SDGNode n : nodes) {

            // for every call that doesn't invoke a thread..
            if (n.getKind() == SDGNode.Kind.CALL && !icfg.isFork(n)) {

                // ..add dummy return sites - needed later
                SDGNode dummy = new SDGNode(SDGNode.Kind.NORMAL, idStart, n.getProc());

                idStart++;

                // add new CF edge between call and dummy
                add.add(new SDGEdge(n, dummy, SDGEdge.Kind.CONTROL_FLOW));

                // deflect edges at dummy return sites
                // change the source of outgoing CF edges of call sites to the corresponding return site
                for (SDGEdge cf : icfg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CONTROL_FLOW)) {

                    add.add(new SDGEdge(dummy, cf.getTarget(), SDGEdge.Kind.CONTROL_FLOW));
                    remove.add(cf);
                }

                dummies.add(dummy);
            }
        }

        // commit
        for (SDGNode n : dummies) {
            icfg.addVertex(n);
        }

        for (SDGEdge e : add) {
            icfg.addEdge(e);
        }

        for (SDGEdge e : remove) {
            icfg.removeEdge(e);
        }
    }

    /** This method inlines parameter vertices into a CFG.
     * - actual-in vertices are placed before the corresponding call node,
     *   actual-out vertices after it
     * - formal-in vertices are placed after the entry vertex, formal-out
     *   vertices are inserted before the exit vertex
     * - exit vertices may have object trees if the procedure returns an object
     *   The vertices of such trees are inserted behind the exit vertex (the last
     *   inserted vertex becomes the new exit vertex of the procedure)
     *
     * @param ipdg  The SDG with parameter vertices.
     */
    public static void inlineParameterVertices(SDG sdg) {
        LinkedList<SDGNode> list = new LinkedList<SDGNode>();

        // remove the return edges
        removeReturnEdges(sdg);

        // first filter the call and entry nodes
        for (SDGNode n : sdg.vertexSet()) {
            if (n.getKind() == SDGNode.Kind.CALL || n.getKind() == SDGNode.Kind.ENTRY) {
                list.add(n);
            }
        }

        // collect all nodes currently connected via control flow
        HashSet<SDGNode> cfg = new HashSet<SDGNode>();
        for (SDGEdge e : sdg.edgeSet()) {
            for (SDGEdge.Kind k : SDGEdge.Kind.controlFlowEdges()) {
                if (e.getKind() == k) {
                    cfg.add(e.getSource());
                    cfg.add(e.getTarget());
                }
            }
        }

        // now inline the parameter nodes
        // ???
//        for (SDGNode n : list) {
//            inline(sdg, cfg, n);
//        }

        // compute the new return edges
        addReturnEdgesTo(sdg);
    }

    /** Inlines all parameter nodes corresponding to a given call or entry node into the given CFG.
     *
     * @param ipdg    The SDG with the parameter vertices.
     * @param icfg    The target CFG.
     * @param source  A call or entry node.
     */
    @SuppressWarnings("unused")
	private static void inline(SDG sdg, HashSet<SDGNode> cfg, SDGNode source) {
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        LinkedList<SDGNode> inList = new LinkedList<SDGNode>();
        LinkedList<SDGNode> outList = new LinkedList<SDGNode>();
        SDGNode.Kind out = null;
        SDGNode.Kind in = null;

        // set in and out flag depending on the type of 'source''
        if (source.getKind() == SDGNode.Kind.CALL) {
            out = SDGNode.Kind.ACTUAL_OUT;
            in = SDGNode.Kind.ACTUAL_IN;

        } else if (source.getKind() == SDGNode.Kind.ENTRY) {
            out = SDGNode.Kind.FORMAL_OUT;
            in = SDGNode.Kind.FORMAL_IN;
        }

        // init worklist
        worklist.add(source);

        // now collect all parameter vertices that belong to 'source'
        while (!worklist.isEmpty()) {
            SDGNode next = worklist.poll();

            // traverse the parameter node trees down to the leaves
            for (SDGEdge edge : sdg.getOutgoingEdgesOfKind(next, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
                SDGNode target = edge.getTarget();

                // save the reached node in the suiting list
                if (target.getKind() == out) {
                    outList.addLast(target);

                } else if (target.getKind() == in) {
                    inList.addLast(target);
                }

                // add it to the worklist
                worklist.addLast(target);
            }
        }

        if (source.getKind() == SDGNode.Kind.CALL) {
            // inline actual-in and -out nodes
            inlineActualParameters(sdg, cfg, source, inList, outList);

        } else if (source.getKind() == SDGNode.Kind.ENTRY) {
            // inline formal-in and -out nodes
            inlineFormalParameters(sdg, cfg, source, inList, outList);

            // exit nodes can have an object tree when returning objects
            // inline those
            inlineExitParameters(sdg, cfg, source);
        }
    }

    /** Inlines actual-in and actual-out nodes belonging to a given call node.
     * Actual-in nodes are placed before the call node, actual-out nodes are
     * placed behind.
     *
     * @param icfg  The target CFG.
     * @param call  The call node.
     * @param aIn   The actual-in nodes of 'call'
     * @param aOut  The actual-out nodes of 'call'
     */
    private static void inlineActualParameters(SDG sdg, HashSet<SDGNode> cfg, SDGNode call,
            LinkedList<SDGNode> aIn, LinkedList<SDGNode> aOut) {

        SDGNode anchor = call;

        // insert actual-out nodes
        for (SDGNode next : aOut) {
            // the outgoing CF edges of the last inserted node
            List<SDGEdge> oldOutgoing = sdg.getOutgoingEdgesOfKind(anchor, SDGEdge.Kind.CONTROL_FLOW);

            // if 'next' was not inserted yet, insert it
            if (cfg.add(next)) {
                // deflect the CF edges
                for (SDGEdge oldEdge : oldOutgoing) {
                    SDGEdge anchor_next = new SDGEdge(anchor, next, SDGEdge.Kind.CONTROL_FLOW);
                    SDGEdge next_target = new SDGEdge(next, oldEdge.getTarget(), SDGEdge.Kind.CONTROL_FLOW);

                    sdg.addEdge(anchor_next);
                    sdg.addEdge(next_target);
                    sdg.removeEdge(oldEdge);
                }
            }

            anchor = next;
        }

        // reset the anchor
        anchor = call;

        // insert actual-in nodes
        // read the list backwards (the last nodes in the list are the leaves of the object trees)
        for (int i = aIn.size() -1; i >= 0; i--) {
            SDGNode next = aIn.get(i);

            // the incoming CF edges of the last inserted node
            List<SDGEdge> oldIncoming = sdg.getIncomingEdgesOfKind(anchor, SDGEdge.Kind.CONTROL_FLOW);

            // if 'next' was not inserted yet, insert it
            if (cfg.add(next)) {
                 // deflect the CF edges: put 'next' between 'anchor' and its old predecessors
                for (SDGEdge oldEdge : oldIncoming) {
                    SDGEdge source_next = new SDGEdge(oldEdge.getSource(), next, SDGEdge.Kind.CONTROL_FLOW);
                    SDGEdge next_anchor = new SDGEdge(next, anchor, SDGEdge.Kind.CONTROL_FLOW);

                    sdg.addEdge(source_next);
                    sdg.addEdge(next_anchor);
                    sdg.removeEdge(oldEdge);
                }
            }

            anchor = next;
        }
    }

    /** Inlines formal-in and formal-out nodes belonging to a given entry node.
     * Formal-in nodes are placed before the entry node, formal-out nodes are
     * placed before the exit node.
     *
     * @param icfg   The target CFG.
     * @param entry  The entry node.
     * @param fIn    The formal-in nodes of 'entry'
     * @param fOut   The formal-out nodes of 'entry'
     */
    private static void inlineFormalParameters(SDG sdg, HashSet<SDGNode> cfg, SDGNode entry,
             LinkedList<SDGNode> fIn, LinkedList<SDGNode> fOut) {

        SDGNode anchor = entry;

        // insert formal-in nodes
        for (SDGNode next : fIn) {
            // the outgoing CF edges of the last inserted node
            List<SDGEdge> oldOutgoing = sdg.getOutgoingEdgesOfKind(anchor, SDGEdge.Kind.CONTROL_FLOW);

            if (cfg.add(next)) {
                // put 'next' between the last inserted node and its successors
                if (oldOutgoing.size() > 1) {
                    for (SDGEdge oldEdge : oldOutgoing) {

                        assert (oldEdge.getTarget().getKind() != SDGNode.Kind.EXIT);
                        {
                            SDGEdge anchor_next = new SDGEdge(anchor, next, SDGEdge.Kind.CONTROL_FLOW);
                            SDGEdge next_target = new SDGEdge(next, oldEdge.getTarget(), SDGEdge.Kind.CONTROL_FLOW);

                            sdg.addEdge(anchor_next);
                            sdg.addEdge(next_target);
                            sdg.removeEdge(oldEdge);
                        }
                    }

                } else {
                    SDGEdge oldEdge = oldOutgoing.get(0);
                    SDGEdge anchor_next = new SDGEdge(anchor, next, SDGEdge.Kind.CONTROL_FLOW);
                    SDGEdge next_target = new SDGEdge(next, oldEdge.getTarget(), SDGEdge.Kind.CONTROL_FLOW);

                    sdg.addEdge(anchor_next);
                    sdg.addEdge(next_target);

                    assert (oldEdge.getTarget().getKind() != SDGNode.Kind.EXIT);
                    {
                        sdg.removeEdge(oldEdge);
                    }
                }
            }

            anchor = next;
        }

        // determine the exit node of entry's procedure
        anchor = getExit(sdg, entry);

        // insert the formal-out nodes before the exit node
        // read the list backwards (the last nodes in the list are the leaves of the object trees)
        for (int i = fOut.size() -1; i >= 0; i--) {
            SDGNode next = fOut.get(i);

            // the incoming CF edges of the last inserted node
            List<SDGEdge> oldIncoming = sdg.getIncomingEdgesOfKind(anchor, SDGEdge.Kind.CONTROL_FLOW);

            if (cfg.add(next)) {
                // insert 'next' between 'anchor' and its predecesors
                for (SDGEdge oldEdge : oldIncoming) {
                    if (oldEdge.getSource().getKind() != SDGNode.Kind.ENTRY) {
                        SDGEdge source_next = new SDGEdge(oldEdge.getSource(), next, SDGEdge.Kind.CONTROL_FLOW);
                        SDGEdge next_anchor = new SDGEdge(next, anchor, SDGEdge.Kind.CONTROL_FLOW);

                        sdg.addEdge(source_next);
                        sdg.addEdge(next_anchor);
                        sdg.removeEdge(oldEdge);
                    }
                }
            }

            anchor = next;
        }
    }

    /** Inlines the nodes of a return value's object tree behind the procedure's exit node.
     *
     * @param ipdg   The SDG.
     * @param icfg   The target CFG
     * @param entry  The entry node of the target procedure.
     */
    private static void inlineExitParameters(SDG sdg, HashSet<SDGNode> cfg, SDGNode entry) {
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        LinkedList<SDGNode> list = new LinkedList<SDGNode>();
        SDGNode exit = sdg.getExit(entry);

        //happens if 'entry' is the root node of the graph (has no corresponding exit node)
        if (exit == null) return;

        // init worklist
        worklist.add(exit);

        // collect all nodes belonging to the exit object tree in list 'list'
        while (!worklist.isEmpty()) {
            SDGNode next = worklist.poll();

            for (SDGEdge edge : sdg.getOutgoingEdgesOfKind(next, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
                SDGNode target = edge.getTarget();

                list.addLast(target);
                worklist.addLast(target);
            }
        }

        SDGNode anchor = exit;

        // put all nodes in 'list' behind the exit node
        for (SDGNode next : list) {
            if (cfg.add(next)) {
                // put next behind the last inserted node
                SDGEdge anchor_next = new SDGEdge(anchor, next, SDGEdge.Kind.CONTROL_FLOW);

                sdg.addEdge(anchor_next);
            }

            anchor = next;
        }
    }

    /** When given an entry node of some procedure p, it returns the exit node of p.
     *
     * @param icfg   A CFG.
     * @param entry  An entry node.
     */
    private static SDGNode getExit(SDG sdg, SDGNode entry) {
    	return sdg.getExit(entry);
    }

    /** Removes all return edges from a given CFG.
     *
     * @param icfg  A CFG.
     */
    private static void removeReturnEdges(SDG sdg) {
        LinkedList<SDGEdge> returns = new LinkedList<SDGEdge>();

        // colect the return edges
        for (SDGNode n : sdg.vertexSet()) {
            if (n.getKind() == SDGNode.Kind.EXIT) {
                for (SDGEdge e : sdg.outgoingEdgesOf(n)) {
                    if (e.getKind() == SDGEdge.Kind.RETURN) {
                        returns.add(e);
                    }
                }
            }
        }

        // delete them
        for (SDGEdge e : returns) {
            sdg.removeEdge(e);
        }
    }

    public static void repairForkSiteEdges(JoanaGraph graph) {
        Set<SDGNode> forkNodes = new HashSet<SDGNode>();

        // collect all fork nodes
        for (SDGEdge e : graph.edgeSet()) {
            if (e.getKind() == SDGEdge.Kind.FORK) {
                forkNodes.add(e.getSource());
            }
        }

        // all edges that must be substituted
        List<SDGEdge> toSubstitute = forkSiteEdges(graph, forkNodes);

        // substitute them
        substitute(graph, toSubstitute);
    }

    /** Returns all call-, parameter-in and parameter-out- edges of a SDG's thread invocations.
     *
     * @param ipdg    A SDG.
     * @param forker  The fork nodes of the SDG.
     */
    private static List<SDGEdge> forkSiteEdges(JoanaGraph ipdg, Collection<SDGNode> forker) {
        List<SDGEdge> l = new LinkedList<SDGEdge>();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();

        worklist.addAll(forker);

        // collect all call-, parameter-out and parameter-in edges of the fork sites
        while(!worklist.isEmpty()) {
            SDGNode next = worklist.poll();

            // descend into the object trees of actual parameters
            for (SDGEdge e : ipdg.outgoingEdgesOf(next)) {

                // add call- and parameter-in- edges to list 'l'
                if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
                    worklist.add(e.getTarget());

                } else if (e.getKind() == SDGEdge.Kind.CALL
                        || e.getKind() == SDGEdge.Kind.PARAMETER_IN) {

                    l.add(e);
                }
            }

            // collect the parameter-out edges
            if (next.getKind() == SDGNode.Kind.ACTUAL_OUT) {
                for (SDGEdge e : ipdg.incomingEdgesOf(next)) {
                    if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                        l.add(e);
                    }
                }
            }
        }

        return l;
    }

    /** Substitute a given set of call-, parameter-in and parameter-out- edges
     * with fork-, fork-in- and fork-out- edges, respectively.
     *
     * @param ipdg          A SDG.
     * @param toSubstitute  The set of edges.
     */
    private static void substitute(JoanaGraph ipdg, List<SDGEdge> toSubstitute) {
        List<SDGEdge> l = new LinkedList<SDGEdge>();

        // substitute
        for (SDGEdge e : toSubstitute) {
            if (e.getKind() == SDGEdge.Kind.CALL) {
                SDGEdge f = new SDGEdge(e.getSource(), e.getTarget(), SDGEdge.Kind.FORK);

                l.add(f);

            } else if (e.getKind() == SDGEdge.Kind.PARAMETER_IN) {
                SDGEdge f = new SDGEdge(e.getSource(), e.getTarget(), SDGEdge.Kind.FORK_IN);

                l.add(f);

            } else if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                SDGEdge f = new SDGEdge(e.getSource(), e.getTarget(), SDGEdge.Kind.FORK_OUT);

                l.add(f);
            }
        }

        for (SDGEdge e : l) {
            ipdg.addEdge(e);
        }

        for (SDGEdge e : toSubstitute) {
            ipdg.removeEdge(e);
        }
    }

    /** Returns the set of summary edges to block for a bounded slice.
     *
     * @param ipdg     The SDG.
     * @param barrier  The barrier of the bounded slice.
     */
    public static Collection<SDGEdge> blockSummaryEdges(SDG sdg, Collection<SDGNode> barrier) {
        // initialisation
        HashSet<SDGEdge> deact = new HashSet<SDGEdge>();
        HashSet<SDGNodeTuple> fofo = new HashSet<SDGNodeTuple>();
        LinkedList<SDGNodeTuple> worklist = new LinkedList<SDGNodeTuple>();
        TIntHashSet markedProcs = new TIntHashSet();

        // block _all_ reachable summary edgies
        for (SDGNode next : sdg.vertexSet()) {
            if (markedProcs.contains(next.getProc())) {
                continue;
            }

            SDGNode entry = sdg.getEntry(next);
            Collection<SDGNode> formOuts = sdg.getFormalOutsOfProcedure(entry);

            for (SDGNode fo : formOuts) {
                if (!barrier.contains(fo)) {
                    fofo.add(new SDGNodeTuple(fo, fo));
                }
            }

            for (SDGEdge call : sdg.incomingEdgesOf(entry)) {
                SDGNode callSite = call.getSource();
                Collection<SDGEdge> summaryEdgies = sdg.getSummaryEdges(callSite);
                deact.addAll(summaryEdgies);
            }
        }

        // unblock some summary edges
        HashSet<SDGNodeTuple> markedEdges = new HashSet<SDGNodeTuple>();
        worklist.addAll(fofo);
        markedEdges.addAll(worklist);

        while (!worklist.isEmpty()) {
        	SDGNodeTuple next = worklist.poll();

            if (next.getFirstNode().getKind() == SDGNode.Kind.FORMAL_IN) {
                for (SDGEdge pi : sdg.getIncomingEdgesOfKind(next.getFirstNode(), SDGEdge.Kind.PARAMETER_IN)) {
                    for (SDGEdge po : sdg.getOutgoingEdgesOfKind(next.getSecondNode(), SDGEdge.Kind.PARAMETER_OUT)) {
                        SDGEdge unblock = null;

                        for (SDGEdge su : deact) {
                            if (su.getSource() == pi.getSource() && su.getTarget() == po.getTarget()) {
                                // unblock summary edge
                                unblock = su;
                                break;
                            }
                        }

                        if (unblock != null) {
                            deact.remove(unblock);
                            LinkedList<SDGNodeTuple> l = new LinkedList<SDGNodeTuple>();

                            for (SDGNodeTuple np : markedEdges) {
                                if (np.getFirstNode() == unblock.getTarget()
                                			&& !barrier.contains(unblock.getSource())) {
                                	SDGNodeTuple p = new SDGNodeTuple(unblock.getSource(), np.getSecondNode());

                                    if (!markedEdges.contains(p)) {
                                        l.add(p);
                                    }
                                }
                            }

                            for (SDGNodeTuple np : l) {
                                markedEdges.add(np);
                                worklist.add(np);
                            }
                        }
                    }
                }

            } else{
                for (SDGEdge edge : sdg.incomingEdgesOf(next.getFirstNode())) {
                    if (edge.getKind() != SDGEdge.Kind.DATA_DEP
                    		&& edge.getKind() != SDGEdge.Kind.DATA_HEAP
                    		&& edge.getKind() != SDGEdge.Kind.DATA_ALIAS
                            && edge.getKind() != SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE
                            && edge.getKind() != SDGEdge.Kind.DATA_DEP_EXPR_VALUE
                            && edge.getKind() != SDGEdge.Kind.DATA_LOOP
                            && edge.getKind() != SDGEdge.Kind.CONTROL_DEP_CALL
                            && edge.getKind() != SDGEdge.Kind.CONTROL_DEP_COND
                            && edge.getKind() != SDGEdge.Kind.CONTROL_DEP_EXPR
                            && edge.getKind() != SDGEdge.Kind.CONTROL_DEP_UNCOND) {

                        continue;
                    }

                    SDGNodeTuple np = new SDGNodeTuple(edge.getSource(), next.getSecondNode());

                    if (!barrier.contains(edge.getSource()) && !markedEdges.contains(np)) {
                        markedEdges.add(np);
                        worklist.add(np);
                    }
                }

                for (SDGEdge su : sdg.getIncomingEdgesOfKind(next.getFirstNode(), SDGEdge.Kind.SUMMARY)) {
                	SDGNodeTuple np = new SDGNodeTuple(su.getSource(), next.getSecondNode());

                    if (!barrier.contains(su.getSource())
                            && !deact.contains(su)
                            && !markedEdges.contains(np)) {

                        markedEdges.add(np);
                        worklist.add(np);
                    }
                }
            }
        }

        return deact;
    }

    /**
     * Gets a set of nodes and computes a barrier for this set.
     * This barrier can be used for bounded slices that shall only visit the
     * nodes of the nodes set.
     *
     * @param ipdg        A SDG.
     * @param validNodes  The set of nodes
     */
    public static Collection<SDGNode> barrier(SDG sdg, Collection<SDGNode> validNodes) {
        HashSet<SDGNode> barrier = new HashSet<SDGNode>();

        for (SDGNode n : validNodes) {
            for (SDGEdge e : sdg.incomingEdgesOf(n)) {
                if (!e.getKind().isSDGEdge()) {
                    continue;
                }

                if (!validNodes.contains(e.getSource())) {
                    barrier.add(e.getSource());
                }
            }

            for (SDGEdge e : sdg.outgoingEdgesOf(n)) {
                if (!e.getKind().isSDGEdge()) {
                    continue;
                }

                if (!validNodes.contains(e.getTarget())) {
                    barrier.add(e.getTarget());
                }
            }
        }

        return barrier;
    }

    public static void annotateRowNumbers(SDG g) {
		LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
		Set<SDGNode> donelist = new HashSet<SDGNode>();
		LinkedList<SDGNode> findlist = new LinkedList<SDGNode>();

		findlist.add(g.getRoot()); //Entry+Call Knoten finden
		while (!findlist.isEmpty()) {
			for (SDGEdge e : g.outgoingEdgesOf(findlist.remove())) {
				SDGNode node = e.getTarget();
				if (donelist.add(node)) {
					if (node.getKind() == SDGNode.Kind.CALL || node.getKind() == SDGNode.Kind.ENTRY) {
						worklist.add(node);
					}
					findlist.add(node);
				}
			}
		}
		donelist.clear();

		while (!worklist.isEmpty()) {
			List<SDGEdge> edges = g.getOutgoingEdgesOfKind(worklist.remove(), Kind.CONTROL_DEP_EXPR);
			for (SDGEdge e : edges) {
				SDGNode node = e.getTarget();
				if (node.getSr() <= 0) {
					System.out.println(node.getId());
					node.setLine(e.getSource().getSr(), e.getSource().getEr());
				}
				if (donelist.add(node)) {
					worklist.add(node);
				}
			}
		}
	}
}
