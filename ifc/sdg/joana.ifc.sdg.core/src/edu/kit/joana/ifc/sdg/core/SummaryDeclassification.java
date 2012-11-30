/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core;
//package edu.kit.joana.ifc.sdg.core;
//
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.Set;
//
//import edu.kit.joana.ifc.sdg.core.conc.Rule;
//import edu.kit.joana.ifc.sdg.graph.SDG;
//import edu.kit.joana.ifc.sdg.graph.SDGEdge;
//import edu.kit.joana.ifc.sdg.graph.SDGNode;
//import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
//
///** Berechnet die Deklassifikationen fuer Summary-Kanten fuer Krinke-Deklassifikationen.
// * Der Algorithmus ist eine modifizierte Summarykantenberechnung, die zusaetzlich eine
// * MOP-Datenflussanalyse fuer erreichende Deklassifikationen durchfuehrt.
// *
// * @author giffhorn
// *
// */
//public class SummaryDeclassification {
//	private static final HashSet<Edge> pathEdge = new HashSet<Edge>();
//	private static final HashMap<SDGNode, Set<Edge>> aoPaths = new HashMap<SDGNode, Set<Edge>>();
//	private static final HashSet<SDGEdge> summaryEdge = new HashSet<SDGEdge>();
//	private static final LinkedList<Edge> worklist = new LinkedList<Edge>();
//	private static SDG sdg = null;
//	private static Collection<SecurityNode> declass = null;
//
//	private SummaryDeclassification() { }
//
//	private static void init() {
//		pathEdge.clear();
//		aoPaths.clear();
//		summaryEdge.clear();
//		worklist.clear();
//	}
//
//	public static HashMap<SDGEdge, Set<Rule>> declassSummaryEdges(SDG g, Collection<SecurityNode> d) {
//		HashMap<SDGEdge, Set<Rule>> result = new HashMap<SDGEdge, Set<Rule>>();
//		init();
//		sdg = g;
//		declass = d;
//
//        for (SDGNode n : sdg.vertexSet()) {
//            if (n.getKind() == SDGNode.Kind.FORMAL_OUT || n.getKind() == SDGNode.Kind.EXIT) {
//                pathEdge.add(new Edge(n,n));
//                worklist.add(new Edge(n,n));
//            }
//        }
//
//        while (!worklist.isEmpty()) {
//            Edge next = worklist.poll();
//            SDGNode.Kind k = next.source.getKind();
//
//            switch(k) {
//                case ACTUAL_OUT:
//                    for (SDGEdge e : sdg.incomingEdgesOf(next.source)) {
//                        if (e.getKind() == SDGEdge.Kind.SUMMARY
//                        		|| e.getKind() == SDGEdge.Kind.DATA_DEP
//                                || (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR
//                                        && e.getSource().getKind() == SDGNode.Kind.CALL)) {
//
//                            propagate(new Edge(e.getSource(), next.target));
//                        }
//                    }
//                    break;
//
//                case FORMAL_IN:
//                    Collection<SDGNodeTuple> aiaoPairs = aiaoPairs(next);
//                    for (SDGNodeTuple e : aiaoPairs) {
//                        if (e.getFirstNode() == null || e.getSecondNode() == null) continue;
//
//                        SDGEdge sum = new SDGEdge(e.getFirstNode(), e.getSecondNode(), SDGEdge.Kind.SUMMARY);
//                        if (sdg.addEdge(sum)) {
//                            summaryEdge.add(sum);
//
//                            Set<Edge> s = aoPaths.get(e.getSecondNode());
//                            if (s != null) {
//                                for (Edge f : s) {
//                                    propagate(new Edge(sum.getSource(), f.target));
//                                }
//                            }
//                        }
//                    }
//                    for (SDGEdge e : sdg.incomingEdgesOf(next.source)) {
//                        if (e.getKind() == SDGEdge.Kind.DATA_DEP) {
//                            propagate(new Edge(e.getSource(), next.target));
//                        }
//                    }
//                    break;
//
//                case ACTUAL_IN:
//                    for (SDGEdge e : sdg.incomingEdgesOf(next.source)) {
//                        if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
//                            if (e.getSource().getKind() == SDGNode.Kind.CALL) {
//                                propagate(new Edge(e.getSource(), next.target));
//                            }
//
//                        } else if (e.getKind().isIntraSDGEdge()) {
//                            propagate(new Edge(e.getSource(), next.target));
//                        }
//                    }
//                    break;
//
//                case FORMAL_OUT:
//                    for (SDGEdge e : sdg.incomingEdgesOf(next.source)) {
//                        if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
//                            if (e.getSource().getKind() == SDGNode.Kind.ENTRY) {
//                                propagate(new Edge(e.getSource(), next.target));
//                            }
//
//                        } else if (e.getKind().isIntraSDGEdge()) {
//                            propagate(new Edge(e.getSource(), next.target));
//                        }
//                    }
//                    break;
//
//                case EXIT:
//                    for (SDGEdge e : sdg.incomingEdgesOf(next.source)) {
//                        if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
//                            if (e.getSource().getKind() == SDGNode.Kind.ENTRY) {
//                                propagate(new Edge(e.getSource(), next.target));
//                            }
//
//                        } else if (e.getKind().isIntraSDGEdge()) {
//                            propagate(new Edge(e.getSource(), next.target));
//                        }
//                    }
//                    break;
//
//                default:
//                    for (SDGEdge e : sdg.incomingEdgesOf(next.source)) {
//                        if (e.getKind().isIntraSDGEdge()) {
//                            propagate(new Edge(e.getSource(), next.target));
//                        }
//                    }
//                    break;
//            }
//        }
//
//        return result;
//    }
//
//    private static void propagate(Edge e) {
//        if (pathEdge.add(e)) {
//            worklist.add(e);
//        }
//        if (e.source.getKind() == SDGNode.Kind.ACTUAL_OUT) {
//            Set<Edge> s = aoPaths.get(e.source);
//            if (s == null) {
//                s = new HashSet<Edge>();
//                aoPaths.put(e.source, s);
//            }
//            s.add(e);
//        }
//    }
//
//    private static Collection<SDGNodeTuple> aiaoPairs(Edge e) {
//        HashMap<SDGNode, SDGNodeTuple> result = new HashMap<SDGNode, SDGNodeTuple>();
//
//        for (SDGEdge pi : sdg.incomingEdgesOf(e.source)) {
//            if (pi.getKind() == SDGEdge.Kind.PARAMETER_IN || pi.getKind() == SDGEdge.Kind.FORK_IN) {
//                SDGNode ai = pi.getSource();
//                SDGNode call = sdg.getCallSiteFor(ai);
//
//                if(call != null) {
//                    result.put(call, new SDGNodeTuple(ai, null));
//                }
//            }
//        }
//
//        for (SDGEdge po : sdg.outgoingEdgesOf(e.target)) {
//            if (po.getKind() == SDGEdge.Kind.PARAMETER_OUT || po.getKind() == SDGEdge.Kind.FORK_OUT) {
//                SDGNode ao = po.getTarget();
//                SDGNode call = sdg.getCallSiteFor(ao);
//
//                SDGNodeTuple newE = result.get(call);
//                if (newE != null) {
//                    newE.setSecondNode(ao);
//                    result.put(call, newE);
//                }
//            }
//        }
//
//        return result.values();
//    }
//
//
//    private static class Edge {
//        private SDGNode source;
//        private SDGNode target;
//        private HashSet<Rule> lvls;
//
//        private Edge(SDGNode s) {
//            source = s;
//            target = s;
//            lvls = new HashSet<Rule>();
//            if (declass.contains(s)) {
//                Rule r = new Rule();
//                r.in(((SecurityNode)s).getRequired());
//                r.out(((SecurityNode)s).getProvided());
//                lvls.add(r);
//            }
//        }
//
////        private Edge(SDGNode s, SDGNode t, HashSet<Rule> l) {
////            source = s;
////            target = t;
////
////            lvls = l;
////        }
//
//        private Edge progress(SDGNode n) {
//        	Edge e = new Edge(source, n);
//            e.lvls.addAll(lvls);
//            if (declass.contains(n)) {
//                Rule r = new Rule();
//                r.in(((SecurityNode)n).getRequired());
//                r.out(((SecurityNode)n).getProvided());
//                lvls.add(r);
//            }
//        }
//
//        public boolean equals(Object o) {
//            if (o instanceof Edge) {
//                Edge e = (Edge) o;
//                return (e.source == source && e.target == target);
//            } else {
//                return false;
//            }
//        }
//
//        public int hashCode() {
//            return source.getId() | target.getId() << 16;
//        }
//
//        public String toString() {
//            return source.getId()+" -> "+target.getId();
//        }
//    }
//}
