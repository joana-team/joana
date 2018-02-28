/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 *
 */
package edu.kit.joana.deprecated.jsdg.sdg.dataflow;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * Summary edge computation.
 *
 * @author giffhorn
 *
 */
public class SummaryEdgeComputation {

	private final HashSet<Edge> pathEdge;
    private final HashMap<SDGNode, Set<Edge>> aoPaths;
    private final HashSet<SDGEdge> summaryEdge;
    private final LinkedList<Edge> worklist;
    private final SDG g;


    public static Collection<SDGEdge> compute(SDG sdg, IProgressMonitor cancel) throws CancelException {
    	SummaryEdgeComputation comp = new SummaryEdgeComputation(sdg);

    	return comp.computeSummaryEdges(cancel);
    }

    private SummaryEdgeComputation(SDG sdg) {
    	this.g = sdg;
        this.pathEdge = new HashSet<Edge>();
        this.aoPaths = new HashMap<SDGNode, Set<Edge>>();
        this.worklist = new LinkedList<Edge>();
        this.summaryEdge = new HashSet<SDGEdge>();
    }

    private Collection<SDGEdge> computeSummaryEdges(IProgressMonitor cancel) throws CancelException {
        long tmp;
        long time1 = 0;
        long time2 = 0;
        long time3 = 0;
        long time4 = 0;

        tmp = System.currentTimeMillis();
        for (SDGNode n : (Set<SDGNode>)g.vertexSet()) {
            if (n.getKind() == SDGNode.Kind.FORMAL_OUT || n.getKind() == SDGNode.Kind.EXIT) {
                pathEdge.add(new Edge(n,n));
                worklist.add(new Edge(n,n));
            }
        }
        time1 += System.currentTimeMillis() - tmp;

        int ctr = 0;
        while (!worklist.isEmpty()) {
            if (cancel.isCanceled()) {
                throw CancelException.make("Operation aborted.");
            }

            ctr++;
            if (ctr % 10000 == 0) {
                Log.info(worklist.size()+", ");
            }
            if (ctr % 100000 == 0) {
                ctr = 0;
                Log.info("\n");
            }
            Edge next = worklist.poll();
            SDGNode.Kind k = next.source.getKind();

            switch(k) {
                case ACTUAL_OUT:
                    tmp = System.currentTimeMillis();
                    for (SDGEdge e : g.incomingEdgesOf(next.source)) {
                        if (e.getKind() == SDGEdge.Kind.SUMMARY
                        		|| e.getKind() == SDGEdge.Kind.DATA_DEP
                        		|| e.getKind() == SDGEdge.Kind.DATA_HEAP
                        		|| e.getKind() == SDGEdge.Kind.DATA_ALIAS
                                || (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR
                                        && e.getSource().getKind() == SDGNode.Kind.CALL)) {

                            propagate(new Edge(e.getSource(), next.target));
                        }
                    }
                    time2 += System.currentTimeMillis() - tmp;
                    break;

                case FORMAL_IN:
                    tmp = System.currentTimeMillis();
                    Collection<Edge> aiaoPairs = aiaoPairs(next);
                    for (Edge e : aiaoPairs) {
                        if (e.source == null || e.target == null) continue;

                        SDGEdge sum = SDGEdge.Kind.SUMMARY.newEdge(e.source, e.target);
                        if (g.addEdge(sum)) {
                            summaryEdge.add(sum);

                            Set<Edge> s = aoPaths.get(e.target);
                            if (s != null) {
                                for (Edge f : s) {
                                    propagate(new Edge(sum.getSource(), f.target));
                                }
                            }
                        }
                    }
                    for (SDGEdge e : g.incomingEdgesOf(next.source)) {
                        if (e.getKind() == SDGEdge.Kind.DATA_DEP || e.getKind() == SDGEdge.Kind.DATA_HEAP
                        		|| e.getKind() == SDGEdge.Kind.DATA_ALIAS) {
                            propagate(new Edge(e.getSource(), next.target));
                        }
                    }
                    time3 += System.currentTimeMillis() - tmp;
                    break;

                case ACTUAL_IN:
                    tmp = System.currentTimeMillis();
                    for (SDGEdge e : g.incomingEdgesOf(next.source)) {
                        if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
                            if (e.getSource().getKind() == SDGNode.Kind.CALL) {
                                propagate(new Edge(e.getSource(), next.target));
                            }

                        } else if (e.getKind().isIntraSDGEdge()) {
                            propagate(new Edge(e.getSource(), next.target));
                        }
                    }
                    time4 += System.currentTimeMillis() - tmp;
                    break;

                case FORMAL_OUT:
                    tmp = System.currentTimeMillis();
                    for (SDGEdge e : g.incomingEdgesOf(next.source)) {
                        if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
                            if (e.getSource().getKind() == SDGNode.Kind.ENTRY) {
                                propagate(new Edge(e.getSource(), next.target));
                            }

                        } else if (e.getKind().isIntraSDGEdge()) {
                            propagate(new Edge(e.getSource(), next.target));
                        }
                    }
                    time4 += System.currentTimeMillis() - tmp;
                    break;

                case EXIT:
                    tmp = System.currentTimeMillis();
                    for (SDGEdge e : g.incomingEdgesOf(next.source)) {
                        if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
                            if (e.getSource().getKind() == SDGNode.Kind.ENTRY) {
                                propagate(new Edge(e.getSource(), next.target));
                            }

                        } else if (e.getKind().isIntraSDGEdge()) {
                            propagate(new Edge(e.getSource(), next.target));
                        }
                    }
                    time4 += System.currentTimeMillis() - tmp;
                    break;

                default:
                    tmp = System.currentTimeMillis();
                    for (SDGEdge e : g.incomingEdgesOf(next.source)) {
                        if (e.getKind().isIntraSDGEdge()) {
                            propagate(new Edge(e.getSource(), next.target));
                        }
                    }
                    time4 += System.currentTimeMillis() - tmp;
                    break;
            }
        }

        Log.info("\ntime consumed: "+((double)(time1+time2+time3+time4)/1000)+" seconds\n"
        		+ "init: "+(double)time1/1000+" seconds\n"
        		+ "case 1: "+(double)time2/1000+" seconds\n"
        		+ "case 2: "+(double)time3/1000+" seconds\n"
        		+ "case 3: "+(double)time4/1000+" seconds");

        return summaryEdge;
    }

    private void propagate(Edge e) {
        if (pathEdge.add(e)) {
            worklist.add(e);
        }
        if (e.source.getKind() == SDGNode.Kind.ACTUAL_OUT) {
            Set<Edge> s = aoPaths.get(e.source);
            if (s == null) {
                s = new HashSet<Edge>();
                aoPaths.put(e.source, s);
            }
            s.add(e);
        }
    }

    private Collection<Edge> aiaoPairs(Edge e) {
        HashMap<SDGNode, Edge> result = new HashMap<SDGNode, Edge>();

        for (SDGEdge pi : g.incomingEdgesOf(e.source)) {
            if (pi.getKind() == SDGEdge.Kind.PARAMETER_IN || pi.getKind() == SDGEdge.Kind.FORK_IN) {
                SDGNode ai = pi.getSource();
                SDGNode call = getCallSiteFor(ai);

                if(call != null) {
                    result.put(call, new Edge(ai, null));
                }
            }
        }

        for (SDGEdge po : g.outgoingEdgesOf(e.target)) {
            if (po.getKind() == SDGEdge.Kind.PARAMETER_OUT || po.getKind() == SDGEdge.Kind.FORK_OUT) {
                SDGNode ao = po.getTarget();
                SDGNode call = getCallSiteFor(ao);

                Edge newE = result.get(call);
                if (newE != null) {
                    newE.target = ao;
                    result.put(call, newE);
                }
            }
        }

        return result.values();
    }


    public SDGNode getCallSiteFor(SDGNode node){
        if (node.getKind() == SDGNode.Kind.ACTUAL_IN){
            SDGNode n = node;

            while (true){
                Set<SDGEdge> edges = g.incomingEdgesOf(n);

                // follow control-dependence-expression edges from the source
                // node of 'edge' to the call node
                for(SDGEdge e : edges){
                    if(e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR){
                        if(e.getSource().getKind() == SDGNode.Kind.CALL){
                            return e.getSource();
                        }
                        n = e.getSource();
                        break;
                    }
                }
            }

        } else if (node.getKind() == SDGNode.Kind.ACTUAL_OUT){
            SDGNode n = node;

            while(true){
                Set<SDGEdge> edges = g.incomingEdgesOf(n);
                // follow control-dependence-expression
                // edges from 'node' to the call node
                for(SDGEdge e : edges){
                    if(e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR){
                        if(e.getSource().getKind() == SDGNode.Kind.CALL){
                            return e.getSource();
                        }
                        n = e.getSource();
                        break;
                    }
                }
            }

        }
        return null;
    }


    private static class Edge {
        private SDGNode source;
        private SDGNode target;

        private Edge(SDGNode s, SDGNode t) {
            source = s;
            target = t;
        }

        public boolean equals(Object o) {
            if (o instanceof Edge) {
                Edge e = (Edge) o;
                return (e.source == source && e.target == target);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return source.getId() | target.getId() << 16;
        }

        public String toString() {
            return source.getId()+" -> "+target.getId();
        }
    }

}
