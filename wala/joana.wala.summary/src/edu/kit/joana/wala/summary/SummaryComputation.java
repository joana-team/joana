/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

import edu.kit.joana.ifc.sdg.graph.BitVector;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.util.collections.Intrusable;
import edu.kit.joana.util.collections.IntrusiveList;
import edu.kit.joana.util.collections.SimpleVector;
import edu.kit.joana.util.graph.EfficientGraph;
import edu.kit.joana.wala.summary.MainChangeTest.RememberReachedBitVector;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.set.TIntSet;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class SummaryComputation< G extends DirectedGraph<SDGNode, SDGEdge> & EfficientGraph<SDGNode, SDGEdge>> {

	private final HashSet<Edge> pathEdge;
    private final HashMap<SDGNode, Set<SDGNode>> aoPaths;
    private final IntrusiveList<Edge> worklist;
    private final G graph;
    private final TIntSet relevantFormalIns;
    private final TIntSet relevantProcs;
    private final TIntSet fullyConnected;
    private final TIntObjectMap<List<SDGNode>> out2in;
    private final boolean rememberReached;
    private final SDGEdge.Kind sumEdgeKind;
    private final Set<SDGEdge.Kind> relevantEdges;
    private final String annotate;
    private final Map<SDGNode, Integer> nodeId2ProcLocalNodeId;

	private SummaryComputation(G graph, TIntSet relevantFormalIns,
			TIntSet relevantProcs, TIntSet fullyConnected, TIntObjectMap<List<SDGNode>> out2in,
			boolean rememberReached, SDGEdge.Kind sumEdgeKind, Set<SDGEdge.Kind> relevantEdges,
			String annotate) {
    	this.graph = graph;
    	this.relevantFormalIns = relevantFormalIns;
    	this.relevantProcs = relevantProcs;
    	this.fullyConnected = fullyConnected;
        this.pathEdge = new HashSet<Edge>();
        this.aoPaths = new HashMap<SDGNode, Set<SDGNode>>();
        this.worklist = new IntrusiveList<>();
        this.out2in = out2in;
        this.rememberReached = rememberReached;
        this.sumEdgeKind = sumEdgeKind;
        this.relevantEdges = relevantEdges;
        this.annotate = annotate;
        this.nodeId2ProcLocalNodeId = new SimpleVector<>(0, graph.vertexSet().size());
	}

	public static int compute(WorkPackage<SDG> pack, IProgressMonitor progress) throws CancelException {
		// default summary computation follows control and date dependencies
		Set<SDGEdge.Kind> relevantEdges = new HashSet<SDGEdge.Kind>();
		relevantEdges.add(SDGEdge.Kind.DATA_DEP);
		relevantEdges.add(SDGEdge.Kind.DATA_HEAP);
		relevantEdges.add(SDGEdge.Kind.DATA_ALIAS);
		relevantEdges.add(SDGEdge.Kind.DATA_LOOP);
		relevantEdges.add(SDGEdge.Kind.DATA_DEP_EXPR_VALUE);
		relevantEdges.add(SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE);
		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_COND);
		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_UNCOND);
		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_EXPR);
		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_CALL);
		relevantEdges.add(SDGEdge.Kind.JUMP_DEP);
		relevantEdges.add(SDGEdge.Kind.SUMMARY);
		relevantEdges.add(SDGEdge.Kind.SUMMARY_DATA);
		relevantEdges.add(SDGEdge.Kind.SUMMARY_NO_ALIAS);
		relevantEdges.add(SDGEdge.Kind.SYNCHRONIZATION);

		return compute(pack, SDGEdge.Kind.SUMMARY, relevantEdges, progress);
	}

	public static int computeAdjustedAliasDep(WorkPackage<SDG> pack, IProgressMonitor progress) throws CancelException {
		// default summary computation follows control and date dependencies
		Set<SDGEdge.Kind> relevantEdges = new HashSet<SDGEdge.Kind>();
		relevantEdges.add(SDGEdge.Kind.DATA_DEP);
		relevantEdges.add(SDGEdge.Kind.DATA_HEAP);
		relevantEdges.add(SDGEdge.Kind.DATA_ALIAS);
		relevantEdges.add(SDGEdge.Kind.DATA_LOOP);
		relevantEdges.add(SDGEdge.Kind.DATA_DEP_EXPR_VALUE);
		relevantEdges.add(SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE);
		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_COND);
		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_UNCOND);
		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_EXPR);
		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_CALL);
		relevantEdges.add(SDGEdge.Kind.JUMP_DEP);
//		relevantEdges.add(SDGEdge.Kind.SUMMARY);
//		relevantEdges.add(SDGEdge.Kind.SUMMARY_DATA);
		relevantEdges.add(SDGEdge.Kind.SUMMARY_NO_ALIAS);
		relevantEdges.add(SDGEdge.Kind.SYNCHRONIZATION);

		return compute(pack, SDGEdge.Kind.SUMMARY_DATA, relevantEdges, progress);
	}

	public static int computePureDataDep(WorkPackage<SDG> pack, IProgressMonitor progress) throws CancelException {
		// default summary computation follows control and date dependencies
		Set<SDGEdge.Kind> relevantEdges = new HashSet<SDGEdge.Kind>();
		relevantEdges.add(SDGEdge.Kind.DATA_DEP);
//		relevantEdges.add(SDGEdge.Kind.DATA_HEAP);
//		relevantEdges.add(SDGEdge.Kind.DATA_LOOP);
//		relevantEdges.add(SDGEdge.Kind.DATA_DEP_EXPR_VALUE);
//		relevantEdges.add(SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE);
//		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_COND);
//		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_UNCOND);
//		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_EXPR);
//		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_CALL);
//		relevantEdges.add(SDGEdge.Kind.JUMP_DEP);
//		relevantEdges.add(SDGEdge.Kind.SUMMARY);
		relevantEdges.add(SDGEdge.Kind.SUMMARY_DATA);
//		relevantEdges.add(SDGEdge.Kind.SYNCHRONIZATION);

		return compute(pack, SDGEdge.Kind.SUMMARY_DATA, relevantEdges, progress);
	}

	public static int computeFullAliasDataDep(WorkPackage<SDG> pack, IProgressMonitor progress) throws CancelException {
		return compute(pack, progress);
	}

	public static int computeNoAliasDataDep(WorkPackage<SDG> pack, IProgressMonitor progress) throws CancelException {
		Set<SDGEdge.Kind> relevantEdges = new HashSet<SDGEdge.Kind>();
		relevantEdges.add(SDGEdge.Kind.DATA_DEP);
		relevantEdges.add(SDGEdge.Kind.DATA_HEAP);
//		relevantEdges.add(SDGEdge.Kind.DATA_ALIAS);
		relevantEdges.add(SDGEdge.Kind.DATA_LOOP);
		relevantEdges.add(SDGEdge.Kind.DATA_DEP_EXPR_VALUE);
		relevantEdges.add(SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE);
		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_COND);
		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_UNCOND);
		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_EXPR);
		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_CALL);
		relevantEdges.add(SDGEdge.Kind.JUMP_DEP);
//		relevantEdges.add(SDGEdge.Kind.SUMMARY);
		relevantEdges.add(SDGEdge.Kind.SUMMARY_DATA);
		relevantEdges.add(SDGEdge.Kind.SUMMARY_NO_ALIAS);
		relevantEdges.add(SDGEdge.Kind.SYNCHRONIZATION);

		return compute(pack, SDGEdge.Kind.SUMMARY_NO_ALIAS, relevantEdges, progress);
	}

	public static int computeHeapDataDep(WorkPackage<SDG> pack, IProgressMonitor progress) throws CancelException {
		// default summary computation follows control and date dependencies
		Set<SDGEdge.Kind> relevantEdges = new HashSet<SDGEdge.Kind>();
		relevantEdges.add(SDGEdge.Kind.DATA_DEP);
		relevantEdges.add(SDGEdge.Kind.DATA_HEAP);
		relevantEdges.add(SDGEdge.Kind.DATA_ALIAS);
//		relevantEdges.add(SDGEdge.Kind.DATA_LOOP);
//		relevantEdges.add(SDGEdge.Kind.DATA_DEP_EXPR_VALUE);
//		relevantEdges.add(SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE);
//		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_COND);
//		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_UNCOND);
//		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_EXPR);
//		relevantEdges.add(SDGEdge.Kind.CONTROL_DEP_CALL);
//		relevantEdges.add(SDGEdge.Kind.JUMP_DEP);
//		relevantEdges.add(SDGEdge.Kind.SUMMARY);
		relevantEdges.add(SDGEdge.Kind.SUMMARY_DATA);
//		relevantEdges.add(SDGEdge.Kind.SUMMARY_NO_ALIAS);
//		relevantEdges.add(SDGEdge.Kind.SYNCHRONIZATION);

		return compute(pack, SDGEdge.Kind.SUMMARY_DATA, relevantEdges, progress);
	}

	private static int compute(WorkPackage<SDG> pack, SDGEdge.Kind sumEdgeKind, Set<SDGEdge.Kind> relevantEdges,
			IProgressMonitor progress) throws CancelException {
		return compute(pack, sumEdgeKind, relevantEdges, null, progress);
	}

	private static int compute(WorkPackage<SDG> pack, SDGEdge.Kind sumEdgeKind, Set<SDGEdge.Kind> relevantEdges,
			String annotate, IProgressMonitor progress) throws CancelException {
		SummaryComputation<SDG> comp = new SummaryComputation<SDG>(pack.getGraph(), pack.getAllFormalInIds(),
				pack.getRelevantProcIds(), pack.getFullyConnected(), pack.getOut2In(),
				pack.getRememberReached(), sumEdgeKind, relevantEdges, annotate);
		Collection<SDGEdge> summary = comp.computeSummaryEdges(progress);

		for (SDGEdge edge : summary) {
			pack.addSummaryDep(edge.getSource().getId(), edge.getTarget().getId());
		}

		// set work package to immutable and sort summary edges
		pack.workIsDone();

		return summary.size();
	}

	@SuppressWarnings("serial")
	private static class PathEdgeReachedNodesBitvector extends BitVector {
		PathEdgeReachedNodesBitvector(int nbits) {
			super(nbits);
		}
	}

    private Collection<SDGEdge> computeSummaryEdges(IProgressMonitor progress) throws CancelException {
    	HashSet<SDGEdge> formInOutSummaryEdge = new HashSet<SDGEdge>();

    	Map<Integer, Set<SDGNode>> proc2nodes = new HashMap<>();
        for (SDGNode n : (Set<SDGNode>) graph.vertexSet()) {
            proc2nodes.compute(n.getProc(), (k, nodes) -> {
            	if (nodes == null) {
            		nodes = new HashSet<>();
            	}
            	nodes.add(n);
            	return nodes;
            });
        }
        
        for (Set<SDGNode> nodes : proc2nodes.values()) {
        	int procLocalNodeId = 0;
        	for (SDGNode n : nodes) {
        		nodeId2ProcLocalNodeId.put(n, procLocalNodeId++);
        	}
        }
        
        for (SDGNode n : (Set<SDGNode>) graph.vertexSet()) {
            if (n.getKind() == SDGNode.Kind.FORMAL_OUT || n.getKind() == SDGNode.Kind.EXIT) {
            	if (relevantProcs != null && !relevantProcs.contains(n.getProc())) {
            		continue;
            	}

            	if (fullyConnected != null && fullyConnected.contains(n.getId())) {
            		continue;
            	}

                assert pathEdge.add(new Edge(n,n));
                assert n.customData == null || (!(n.customData instanceof RememberReachedBitVector));
                n.customData = new PathEdgeReachedNodesBitvector(proc2nodes.get(n.getProc()).size());
                worklist.add(new Edge(n,n));
            }
        }
        
        proc2nodes = null;

        while (!worklist.isEmpty()) {
        	MonitorUtil.throwExceptionIfCanceled(progress);

            Edge next = worklist.poll();
            SDGNode.Kind k = next.source.getKind();

            switch(k) {
                case ACTUAL_OUT:
                	if (fullyConnected != null && fullyConnected.contains(next.source.getId())) {
                		propagateAllActIns(next.source, next.target);
                	} else {
	                    for (SDGEdge e : graph.incomingEdgesOfUnsafe(next.source)) {
	                    	final SDGEdge.Kind kind = e.getKind();
	                        if (kind == sumEdgeKind
	                        		|| ((kind == SDGEdge.Kind.DATA_DEP || kind == SDGEdge.Kind.DATA_HEAP
	                        				|| kind == SDGEdge.Kind.DATA_ALIAS) && relevantEdges.contains(kind))
	                                || (kind == SDGEdge.Kind.CONTROL_DEP_EXPR
	                                        && e.getSource().getKind() == SDGNode.Kind.CALL)) {
	                    		propagate(e.getSource(), next.target);
	                        }
	                    }
                	}
                    break;

                case FORMAL_IN:
                	// next.source is relevant formal in then:
                	if (relevantFormalIns.contains(next.source.getId())) {
	                	SDGEdge fInOut;
	                	if (annotate != null && !annotate.isEmpty()) {
	                		fInOut = new SDGEdge(next.source, next.target, sumEdgeKind, annotate);
	                	} else {
	                		fInOut = new SDGEdge(next.source, next.target, sumEdgeKind);
	                	}

	                	formInOutSummaryEdge.add(fInOut);
                	}

                    Collection<Edge> aiaoPairs = aiaoPairs(next);
                    for (Edge e : aiaoPairs) {
                        if (e.source == null || e.target == null) continue;

                        final boolean connectedInPDG = graph.containsEdge(e.source, e.target, eOut -> eOut.getKind().isSDGEdge());
                        if (connectedInPDG) continue; // already connected

                        SDGEdge sum;
                        if (annotate != null && !annotate.isEmpty()) {
                        	sum = new SDGEdge(e.source, e.target, sumEdgeKind, annotate);
                        } else {
                        	sum = new SDGEdge(e.source, e.target, sumEdgeKind);
                        }

                        if (graph.addEdgeUnsafe(e.source, e.target, sum)) {
                            Set<SDGNode> s = aoPaths.get(e.target);
                            if (s != null) {
                                for (SDGNode target : s) {
                                    propagate(sum.getSource(), target);
                                }
                            }
                        }
                    }
                    for (SDGEdge e : graph.incomingEdgesOfUnsafe(next.source)) {
                    	final SDGEdge.Kind kind = e.getKind();
                    	if ((kind == SDGEdge.Kind.DATA_DEP || kind == SDGEdge.Kind.DATA_HEAP
                    			|| kind == SDGEdge.Kind.DATA_ALIAS) && relevantEdges.contains(kind)) {
                        	propagate(e.getSource(), next.target);
                        }
                    }
                    break;

                case ACTUAL_IN:
                	if (rememberReached) {
                		BitVector bv = (RememberReachedBitVector) next.source.customData;
                		int id = next.target.tmp;

                		if (bv.contains(id)) {
                			continue;
                		}

                		bv.set(id);
                	}

                	for (SDGEdge e : graph.incomingEdgesOfUnsafe(next.source)) {
                        if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
                            if (e.getSource().getKind() == SDGNode.Kind.CALL) {
                                propagate(e.getSource(), next.target);
                            }

                        } else if (relevantEdges.contains(e.getKind())) {
                            propagate(e.getSource(), next.target);
                        }
                    }

                    break;

                case FORMAL_OUT:
                case EXIT:
                	if (fullyConnected != null && fullyConnected.contains(next.source.getId())) {
                		propagateAllActIns(next.source, next.target);
                	} else {
                    for (SDGEdge e : graph.incomingEdgesOfUnsafe(next.source)) {
                        if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
                            if (e.getSource().getKind() == SDGNode.Kind.ENTRY) {
                                propagate(e.getSource(), next.target);
                            }

                        } else if (relevantEdges.contains(e.getKind())) {
                            propagate(e.getSource(), next.target);
                        }
                    }
                	}
//                    for (SDGEdge e : graph.incomingEdgesOf(next.source)) {
//                        if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
//                            if (e.getSource().getKind() == SDGNode.Kind.ENTRY) {
//                                propagate(new Edge(e.getSource(), next.target));
//                            }
//
//                        } else if (e.getKind().isIntraSDGEdge()) {
//                            propagate(new Edge(e.getSource(), next.target));
//                        }
//                    }
                    break;

                default:
                    for (SDGEdge e : graph.incomingEdgesOfUnsafe(next.source)) {
                        if (relevantEdges.contains(e.getKind())) {
                            propagate(e.getSource(), next.target);
                        }
                    }
                    break;
            }
        }
        
        // clear HashSet<SDGNode> at each node
        for (SDGNode n : (Set<SDGNode>) graph.vertexSet()) {
            if (n.getKind() == SDGNode.Kind.FORMAL_OUT || n.getKind() == SDGNode.Kind.EXIT) {
            	if (relevantProcs != null && !relevantProcs.contains(n.getProc())) {
            		continue;
            	}

            	if (fullyConnected != null && fullyConnected.contains(n.getId())) {
            		continue;
            	}

                assert n.customData instanceof BitVector;
                n.customData = null;
            }
        }

        return formInOutSummaryEdge;
    }

    private void propagateAllActIns(SDGNode outNode, SDGNode target) {
    	for (SDGNode inNode : out2in.get(outNode.getId())) {
    		propagate(inNode, target);
    	}
    }

    private void propagate(SDGNode source, SDGNode target) {
        if (relevantProcs != null && !(relevantProcs.contains(source.getProc())
        		&& relevantProcs.contains(target.getProc()))) {
            return;
        }

//    	if (fullyConnected != null && fullyConnected.contains(e.target.getId())) {
//    		return;
//    	}
//
        
        if (pathEdge_add(source, target)) {
        	final Edge e = new Edge(source, target);
        	assert pathEdge.add(e);
            worklist.add(e);
        } else {
        	assert pathEdge.contains(new Edge(source, target));
        }
        if (source.getKind() == SDGNode.Kind.ACTUAL_OUT) {
        	aoPaths.compute(source, (k, s) -> {
        		if (s == null) {
        			s = new HashSet<SDGNode>();
        		}
        		s.add(target);
        		return s;
        	});
        }
    }
    
    private boolean pathEdge_add(SDGNode source, SDGNode target) {
    	assert source.getProc() == target.getProc();
		final PathEdgeReachedNodesBitvector sources = (PathEdgeReachedNodesBitvector) target.customData;
    	final int procLocalSourceId = nodeId2ProcLocalNodeId.get(source);
    	boolean isNew = !sources.get(procLocalSourceId);
    	sources.set(procLocalSourceId);
    	return isNew;
    }

//    if (relevantProcs != null) {
//    	if (!(relevantProcs.contains(e.source.getProc())
//    		&& relevantProcs.contains(e.target.getProc()))) {
//    		continue;
//    	} else if (graph.containsEdge(e.source, e.target)) {
//    		continue;
//    	}
//    }



    private Collection<Edge> aiaoPairs(Edge e) {
        HashMap<SDGNode, Edge> result = new HashMap<SDGNode, Edge>();

        for (SDGEdge pi : graph.incomingEdgesOfUnsafe(e.source)) {
            if (pi.getKind() == SDGEdge.Kind.PARAMETER_IN) {
                SDGNode ai = pi.getSource();

//                if (relevantProcs != null && !relevantProcs.contains(ai.getProc())) {
//            		continue;
//                }


                SDGNode call = getCallSiteFor(ai);

                if(call != null) {
                    result.put(call, new Edge(ai, null));
                }
            }
        }

        for (SDGEdge po : graph.outgoingEdgesOfUnsafe(e.target)) {
            if (po.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                SDGNode ao = po.getTarget();

//                if (relevantProcs != null && !relevantProcs.contains(ao.getProc())) {
//            		continue;
//                }

                SDGNode call = getCallSiteFor(ao);

                Edge newE = result.get(call);
                if (newE != null) {
                	
                    newE.target = ao;
                    assert newE.target.getProc() == newE.source.getProc();
                    result.put(call, newE);
                }
            }
        }

        return result.values();
    }


    public SDGNode getCallSiteFor(SDGNode node){
        if (node.getKind() == SDGNode.Kind.ACTUAL_IN) {
            SDGNode n = node;

            while (true){
                Set<SDGEdge> edges = graph.incomingEdgesOfUnsafe(n);

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
                Set<SDGEdge> edges = graph.incomingEdgesOfUnsafe(n);
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


    private static class Edge implements Intrusable<Edge> {
        private SDGNode source;
        private SDGNode target;
        
        private Edge next;

        private Edge(SDGNode s, SDGNode t) {
        	assert t == null || t.getKind() == SDGNode.Kind.FORMAL_OUT || t.getKind() == SDGNode.Kind.EXIT;
        	assert t == null || s.getProc() == t.getProc();
            source = s;
            target = t;
        }
        
        @Override
        public void setNext(Edge next) {
        	this.next = next;
        }
        
        @Override
        public Edge getNext() {
        	return next;
        }
        

        public boolean equals(Object o) {
            if (o instanceof Edge) {
                Edge e = (Edge) o;
                return (e.source == source && e.target == target);
            } else {
                return false;
            }
        }

        @Deprecated
        @SuppressWarnings("unused")
        public int hashCodeOld() {
            return source.getId() | target.getId() << 16;
        }
        
        @Override
        public int hashCode() {
        	return (source.hashCode() ^ (Integer.rotateRight(target.hashCode(), 16)));
        }

        public String toString() {
            return source.getId()+" -> "+target.getId();
        }
    }

}
