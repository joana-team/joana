/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.intset.IntIterator;
import edu.kit.joana.ifc.sdg.graph.*;
import edu.kit.joana.util.collections.*;
import edu.kit.joana.util.graph.EfficientGraph;
import edu.kit.joana.util.graph.TarjanStrongConnectivityInspector;
import edu.kit.joana.wala.summary.MainChangeTest.RememberReachedBitVector;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.set.TIntSet;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.Arrays;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class SummaryComputation3< G extends DirectedGraph<SDGNode, SDGEdge> & EfficientGraph<SDGNode, SDGEdge>> {

	private final HashSet<Edge> pathEdge;
    private final Set<Integer> procedureWorkSet;
    private final Map<Integer, IntrusiveList<Edge>> worklists;
    private final G graph;
    private final TIntSet relevantFormalIns;
    private final TIntSet relevantProcs;
    private final TIntSet fullyConnected;
	private final Optional<TIntSet> initialWorklistEntries;
	private final TIntObjectMap<List<SDGNode>> out2in;
    private final boolean rememberReached;
    private final SDGEdge.Kind sumEdgeKind;
    private final Set<SDGEdge.Kind> relevantEdges;
    private final String annotate;
    private final IntIntSimpleVector nodeId2ProcLocalNodeId;
    private final SimpleVectorBase<Integer, SDGNode[]> procLocalNodeId2Node;
    private final List<Set<Integer>> procSccs;
    private final IntIntSimpleVector indexNumberOf;
    private final long relevantEdgesMask;
    private final long relevantEdgesAtActualOutMask;
    private final boolean assertionsEnabled;

    private IntrusiveList<Edge> current;

	private SummaryComputation3(G graph, TIntSet relevantFormalIns, TIntSet relevantProcs, TIntSet fullyConnected, TIntObjectMap<List<SDGNode>> out2in,
			boolean rememberReached, SDGEdge.Kind sumEdgeKind, Set<SDGEdge.Kind> relevantEdges,
			String annotate, Optional<TIntSet> initialWorklistEntries) {
    	this.graph = graph;
    	this.relevantFormalIns = relevantFormalIns;
    	this.relevantProcs = relevantProcs;
    	this.fullyConnected = fullyConnected;
		this.initialWorklistEntries = initialWorklistEntries;
		this.pathEdge = new HashSet<Edge>();
        int maxProcNumber = -1;
        {
            final DirectedGraph<Integer, DefaultEdge> callGraph = extractCallGraph(graph);
            final TarjanStrongConnectivityInspector<Integer, DefaultEdge> sccInspector = new TarjanStrongConnectivityInspector<>(callGraph);

            this.procSccs = sccInspector.stronglyConnectedSets();
            assert procSccs instanceof RandomAccess;

            final Map<Integer, TarjanStrongConnectivityInspector.VertexNumber<Integer>> indices = sccInspector.getVertexToVertexNumber();
            this.indexNumberOf = new IntIntSimpleVector(0, 1);
            for (Entry<Integer, TarjanStrongConnectivityInspector.VertexNumber<Integer>> entry : indices.entrySet()) {
            	maxProcNumber = Math.max(maxProcNumber, entry.getKey());
                indexNumberOf.put(entry.getKey(), entry.getValue().getSccNumber());
            }
            indexNumberOf.trimToSize();

            this.procedureWorkSet = new TreeSet<>(new Comparator<Integer>() {
            	@Override
            	public int compare(Integer o1, Integer o2) {
            		final int sccCompare = Integer.compare(indexNumberOf.getInt(o1), indexNumberOf.getInt(o2));
    				if (sccCompare != 0) return sccCompare;

    				return Integer.compare(o1, o2);
            	}
			});
        }

        this.worklists = new SimpleVectorBase<Integer, IntrusiveList<Edge>>(0, 1) {
            protected int getId(Integer k) {
            	return k;
            };
        };

        this.out2in = out2in;
        this.rememberReached = rememberReached;
        this.sumEdgeKind = sumEdgeKind;
        this.relevantEdges = relevantEdges;
        this.annotate = annotate;
        this.nodeId2ProcLocalNodeId = new IntIntSimpleVector(0, graph.vertexSet().size());
        this.procLocalNodeId2Node = new SimpleVectorBase<Integer, SDGNode[]>(0, maxProcNumber) {
        	@Override
        	protected int getId(Integer procNumber) {
        		return procNumber;
        	}
        };

        long relevantEdgesMask = 0;
        for (SDGEdge.Kind relevant : relevantEdges) {
        	assert 0 <= relevant.getPriority() && relevant.getPriority() < 64;
        	relevantEdgesMask |= ((long) 1 << ((long)relevant.getPriority()));
        }
        this.relevantEdgesMask = relevantEdgesMask;
        this.relevantEdgesAtActualOutMask =
              ( ((long) 1 << ((long)sumEdgeKind.getPriority()))
              | ((long) 1 << ((long)SDGEdge.Kind.DATA_DEP.getPriority()))
              | ((long) 1 << ((long)SDGEdge.Kind.DATA_HEAP.getPriority()))
              | ((long) 1 << ((long)SDGEdge.Kind.DATA_ALIAS.getPriority()))
              )
              & relevantEdgesMask
              | ((long) 1 << ((long)SDGEdge.Kind.CONTROL_DEP_EXPR.getPriority()));
        boolean assertionsEnabled = false;
        assert (assertionsEnabled = true);
        this.assertionsEnabled = assertionsEnabled;
	}

	private boolean relevantEdges_contains(SDGEdge.Kind kind) {
		// TODO: find out whether just using EnumSet<> for relevantEdges is good enough
		final boolean result = ((long) 1 << ((long)kind.getPriority()) & relevantEdgesMask) != 0;
		assert result == relevantEdges.contains(kind);
		return result;
	}

	private boolean relevantEdgesAtActualOut_contains(SDGEdge.Kind kind) {
		final boolean result = ((long) 1 << ((long)kind.getPriority()) & relevantEdgesAtActualOutMask) != 0;
		assert result == (kind == sumEdgeKind
				|| ((kind == SDGEdge.Kind.DATA_DEP || kind == SDGEdge.Kind.DATA_HEAP
				|| kind == SDGEdge.Kind.DATA_ALIAS) && relevantEdges_contains(kind))
				|| (kind == SDGEdge.Kind.CONTROL_DEP_EXPR));
				relevantEdges.contains(kind);
		return result;
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
		SummaryComputation3<SDG> comp = new SummaryComputation3<SDG>(pack.getGraph(), pack.getAllFormalInIds(),
				pack.getRelevantProcIds(), pack.getFullyConnected(), pack.getOut2In(),
				pack.getRememberReached(), sumEdgeKind, relevantEdges, annotate, pack.getInitialWorklistEntries());
		Collection<SDGEdge> formInOutSummaryEdge = comp.computeSummaryEdges(progress);

		for (SDGEdge edge : formInOutSummaryEdge) {
			pack.addSummaryDep(edge.getSource().getId(), edge.getTarget().getId());
		}

		// set work package to immutable and sort summary edges
		pack.workIsDone();

		return formInOutSummaryEdge.size();
	}

	@SuppressWarnings("serial")
	private static class PathEdgeReachedNodesBitvector extends BitVector64 {
		PathEdgeReachedNodesBitvector(int nbits) {
			super(nbits);
		}
	}

	private static class ActualOutInformation {
		final AoPathsNodesBitvector aoPaths;
		final IncomingSummaryEdgesFromBitVector incomingSummaryEdgesFrom;

		public ActualOutInformation(AoPathsNodesBitvector aoPaths, IncomingSummaryEdgesFromBitVector incomingSummaryEdgesFrom) {
			this.aoPaths = aoPaths;
			this.incomingSummaryEdgesFrom = incomingSummaryEdgesFrom;
		}
	}

	@SuppressWarnings("serial")
	private static class AoPathsNodesBitvector extends BitVector64 {
		AoPathsNodesBitvector(int nbits) {
			super(nbits);
		}
	}

	@SuppressWarnings("serial")
	private static class IncomingSummaryEdgesFromBitVector extends BitVector64 {
		IncomingSummaryEdgesFromBitVector(int nbits) {
			super(nbits);
		}
	}

	private static class ActualInInformation {
		private final SDGEdge[] summaryEdges;
		int next;

		public ActualInInformation(SDGEdge[] summaryEdges) {
			this.summaryEdges = summaryEdges;
			this.next = 0;
		}
	}



	private DirectedGraph<Integer, DefaultEdge> extractCallGraph(G graph) {
		final DirectedGraph<Integer, DefaultEdge> ret = new DefaultDirectedGraph<>(DefaultEdge.class);
		for (final SDGNode n : graph.vertexSet()) {
			final int proc = n.getProc();
			ret.addVertex(proc);
			if (n.getKind() == SDGNode.Kind.CALL) {
				for (final SDGEdge callEdge : graph.outgoingEdgesOf(n)) {
					if (callEdge.getKind() != SDGEdge.Kind.CALL) continue;
					final int calledProc = callEdge.getTarget().getProc();
					ret.addVertex(calledProc);
					ret.addEdge(proc, calledProc);
				}
			}
		}
		return ret;
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

        for (Entry<Integer, Set<SDGNode>> entry : proc2nodes.entrySet()) {
        	final int procedure = entry.getKey();
        	final Set<SDGNode> nodes = entry.getValue();
        	final SDGNode[] procLocal2Node = new SDGNode[nodes.size()];
			procLocalNodeId2Node.put(procedure, procLocal2Node);
        	int procLocalNodeId = 0;
        	for (SDGNode n : nodes) {
        		nodeId2ProcLocalNodeId.put(n.getId(), procLocalNodeId);
        		procLocal2Node[procLocalNodeId] =  n;
        		procLocalNodeId++;
        	}
        }

        procLocalNodeId2Node.trimToSize();
        nodeId2ProcLocalNodeId.trimToSize();

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
                worklists.compute(n.getProc(), (proc, workList) -> {
                	if (workList == null) {
                		workList = new IntrusiveList<>();
                	}
                	workList.add(new Edge(n,n));
                	return workList;
                });
								if (!initialWorklistEntries.isPresent() || initialWorklistEntries.get().contains(n.getProc())) {
									procedureWorkSet.add(n.getProc());
								}
            }

            if (n.getKind() == SDGNode.Kind.ACTUAL_OUT) {
                assert n.customData == null || (!(n.customData instanceof ActualOutInformation));
                n.customData = new ActualOutInformation(
                		new AoPathsNodesBitvector(proc2nodes.get(n.getProc()).size()),
                		new IncomingSummaryEdgesFromBitVector(proc2nodes.get(n.getProc()).size())
                );
            }

            if (n.getKind() == SDGNode.Kind.ACTUAL_IN) {
                assert n.customData == null || (!(n.customData instanceof Integer));
                n.customData = 0;
            }

            if (n.getKind() == SDGNode.Kind.CALL) {
                n.customData = null;
            }
        }

        proc2nodes = null;

        assert workListsConsistent();

        while (!procedureWorkSet.isEmpty()) {


            final int procedure; {
                final Iterator<Integer> iterator = procedureWorkSet.iterator();
                procedure = iterator.next();
                iterator.remove();
            }
            final IntrusiveList<Edge> worklist = worklists.get(procedure);
            current = worklist;

            final SDGNode[] procLocal2Node = procLocalNodeId2Node.get(procedure);

            while (!worklist.isEmpty()) {
            	final Edge next = worklist.poll();
            	final SDGNode.Kind k = next.source.getKind();

            	switch(k) {
            	case ACTUAL_OUT:
            		if (fullyConnected != null && fullyConnected.contains(next.source.getId())) {
            			propagateAllActIns(worklist, next.source, next.target);
            		} else {
            			for (SDGEdge e : graph.incomingEdgesOfUnsafe(next.source)) {
            				if (e == null) continue;
            				final SDGEdge.Kind kind = e.getKind();
            				assert kind != SDGEdge.Kind.CONTROL_DEP_EXPR || e.getSource().getKind() == SDGNode.Kind.CALL;
            				if (relevantEdgesAtActualOut_contains(kind)) {
            					propagate(worklist, e.getSource(), next.target);
            				}
            			}
            			final ActualOutInformation aoInformation = (ActualOutInformation) next.source.customData;
            			final IncomingSummaryEdgesFromBitVector incomingSummaryEdgesFrom = aoInformation.incomingSummaryEdgesFrom;

            			for (IntIterator it = incomingSummaryEdgesFrom.intIterator(); it.hasNext(); ) {
            				final int procLocalId = it.next();
            				SDGNode summarySource = procLocal2Node[procLocalId];
            				propagate(worklist, summarySource, next.target);
            			}

            		}
            		break;

            	case FORMAL_IN:
            		// next.source is relevant formal in then:
            		if (relevantFormalIns.contains(next.source.getId())) {
            			SDGEdge fInOut;
            			if (annotate != null && !annotate.isEmpty()) {
            				fInOut =  new LabeledSDGEdge(next.source, next.target, sumEdgeKind, annotate);
            			} else {
            				fInOut = sumEdgeKind.newEdge(next.source, next.target);
            			}

            			formInOutSummaryEdge.add(fInOut);
            		}

            		final Collection<AcutalInActualOutPair> aiaoPairs = aiaoPairs(next);
            		for (AcutalInActualOutPair e : aiaoPairs) {

            			final SDGNode source = e.getActualIn();
            			final SDGNode target = e.getActualOut();

            			assert source != null;
            			if (target == null) continue;

            			boolean connectedInPDG = false;
            			if (assertionsEnabled) {
            				connectedInPDG = graph.containsEdge(source, target, eOut -> eOut.getKind().isSDGEdge());
            			}


            			final int procLocalIdOfSource = nodeId2ProcLocalNodeId.getInt(source.getId());
            			final ActualOutInformation aoInformation = (ActualOutInformation) target.customData;

            			if (aoInformation.incomingSummaryEdgesFrom.setWithResult((procLocalIdOfSource))) {
            				//assert !connectedInPDG; // TODO: improve reuse of old summary edges

            				final Integer nrOfoutgoingSummaryEdgesInto = (Integer) source.customData;
            				source.customData = nrOfoutgoingSummaryEdgesInto + 1;

            				final AoPathsNodesBitvector aoPaths = aoInformation.aoPaths;

            				if (!aoPaths.isZero()) {
            					final int caller = source.getProc();
            					procedureWorkSet.add(caller);
            					final IntrusiveList<Edge> workListInCaller = worklists.get(caller);
            					final SDGNode[] callerLocal2Node = procLocalNodeId2Node.get(caller);

            					for (IntIterator it = aoPaths.intIterator(); it.hasNext(); ) {
            						final int procLocalId = it.next();

            						SDGNode aoPathTarget = callerLocal2Node[procLocalId];
            						propagate(workListInCaller, source, aoPathTarget);
            					}
            				}
            			}
            		}
            		if (assertionsEnabled) {
	            		for (SDGEdge e : graph.incomingEdgesOfUnsafe(next.source)) {
	            			if (e == null) continue;
	            			final SDGEdge.Kind kind = e.getKind();
	            			assert !((kind == SDGEdge.Kind.DATA_DEP || kind == SDGEdge.Kind.DATA_HEAP || kind == SDGEdge.Kind.DATA_ALIAS) && relevantEdges_contains(kind));
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
            			if (e == null) continue;
            			assert e.getKind() != SDGEdge.Kind.CONTROL_DEP_EXPR || e.getSource().getKind() == SDGNode.Kind.CALL;
            			if (relevantEdges_contains(e.getKind()) || e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
            				propagate(worklist, e.getSource(), next.target);
            			}
            		}
            		break;

            	case FORMAL_OUT:
            	case EXIT:
            		if (fullyConnected != null && fullyConnected.contains(next.source.getId())) {
            			propagateAllActIns(worklist, next.source, next.target);
            		} else {
            			for (SDGEdge e : graph.incomingEdgesOfUnsafe(next.source)) {
            				if (e == null) continue;
            				if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
            					if (e.getSource().getKind() == SDGNode.Kind.ENTRY) {
            						propagate(worklist, e.getSource(), next.target);
            					}

            				} else if (relevantEdges_contains(e.getKind())) {
            					propagate(worklist, e.getSource(), next.target);
            				}
            			}
            		}
            		break;

            	default:
            		for (SDGEdge e : graph.incomingEdgesOfUnsafe(next.source)) {
            			if (e == null) continue;
            			if (relevantEdges_contains(e.getKind())) {
            				propagate(worklist, e.getSource(), next.target);
            			}
            		}
            		break;
            	}
            }

            // TODO: somehow update this implicitly when creating summary edges
            boolean leftScc = true;
            for (Integer inSameScc : procSccs.get(indexNumberOf.getInt(procedure))) {
            	if (procedureWorkSet.contains(inSameScc)) {
            		leftScc = false;
            		break;
            	}
            }

            // clear HashSet<SDGNode> at each node whenever we leave a scc
            if (leftScc) {
                for (Integer inSameScc : procSccs.get(indexNumberOf.getInt(procedure))) {
                	final SDGNode[] inSameSccLocal2Node = procLocalNodeId2Node.get(inSameScc);
                	for (SDGNode n : inSameSccLocal2Node) {
                		if (n.getKind() == SDGNode.Kind.ACTUAL_IN) {
                			final Integer nrOfoutgoingSummaryEdges = (Integer) n.customData;
                			n.customData = new ActualInInformation(new SDGEdge[nrOfoutgoingSummaryEdges]);
                			// will be filled in the upcoming loop by the corresponding ACTUAL_OUTs
                		}
                	}

                	for (SDGNode n : inSameSccLocal2Node) {
                		if (n.getKind() == SDGNode.Kind.FORMAL_OUT || n.getKind() == SDGNode.Kind.EXIT) {
                			if (relevantProcs != null && !relevantProcs.contains(n.getProc())) {
                				continue;
                			}

                			if (fullyConnected != null && fullyConnected.contains(n.getId())) {
                				continue;
                			}

                			assert n.customData instanceof PathEdgeReachedNodesBitvector;
                			n.customData = null;
                		}

                		if (n.getKind() == SDGNode.Kind.ACTUAL_OUT) {
                			final ActualOutInformation aoInformation = (ActualOutInformation) n.customData;
                			final IncomingSummaryEdgesFromBitVector incomingSummaryEdgesFrom = aoInformation.incomingSummaryEdgesFrom;

                			final int nrOfSummaryEdges = incomingSummaryEdgesFrom.populationCount();
                			final SDGEdge[] summaryEdges = new SDGEdge[nrOfSummaryEdges];

                			int i = 0;
                			for (IntIterator it = incomingSummaryEdgesFrom.intIterator(); it.hasNext(); ) {
                				final int procLocalId = it.next();
                				final SDGNode source = inSameSccLocal2Node[procLocalId];

                				final SDGEdge sum;
                				if (annotate != null && !annotate.isEmpty()) {
                					sum =  new LabeledSDGEdge(source, n, sumEdgeKind, annotate);
                				} else {
                					sum = sumEdgeKind.newEdge(source, n);
                				}

                				summaryEdges[i++] = sum;

                				final ActualInInformation actualInInformation = (ActualInInformation) source.customData;
                				actualInInformation.summaryEdges[actualInInformation.next++] = sum;
                			}

                			Arrays.sort(summaryEdges, ArraySet.COMPARATOR);

                			final ArraySet<SDGEdge> summaryEdgesSet = ArraySet.own(summaryEdges);

                			graph.addIncomingEdgesAtUNSAFE(n, summaryEdgesSet);

                			n.customData = null;
                		}
                	}

                	for (SDGNode n : inSameSccLocal2Node) {
                		if (n.getKind() == SDGNode.Kind.ACTUAL_IN) {
                			final ActualInInformation actualInInformation = (ActualInInformation) n.customData;

                			final SDGEdge[] summaryEdges = actualInInformation.summaryEdges;
                			assert summaryEdges.length == 0 || summaryEdges[summaryEdges.length - 1] != null;

                			Arrays.sort(summaryEdges, ArraySet.COMPARATOR);

                			final ArraySet<SDGEdge> summaryEdgesSet = ArraySet.own(summaryEdges);

                			graph.addOutgoingEdgesAtUNSAFE(n, summaryEdgesSet);

                			n.customData = null;

                		}

                	}

                	procLocalNodeId2Node.remove(inSameScc);
                }
            }

            assert workListsConsistent();
        }

        return formInOutSummaryEdge;
    }

    private boolean workListsConsistent() {
        for (Entry<Integer, IntrusiveList<Edge>> entry : worklists.entrySet()) {
        	final Integer proc = entry.getKey();
        	final IntrusiveList<Edge> workList = entry.getValue();
        	if (!workList.isEmpty() &&  !procedureWorkSet.contains(proc)) {
        		return false;
        	}
        }
        return true;
    }

    private void propagateAllActIns(IntrusiveList<Edge> worklist, SDGNode outNode, SDGNode target) {
    	for (SDGNode inNode : out2in.get(outNode.getId())) {
    		propagate(worklist, inNode, target);
    	}
    }

    private void propagate(IntrusiveList<Edge> worklist, SDGNode source, SDGNode target) {
    	assert source.getProc() == target.getProc();
    	assert worklist == worklists.get(source.getProc());
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
            assert procedureWorkSet.contains(source.getProc()) || worklist == current;
            if (source.getKind() == SDGNode.Kind.ACTUAL_OUT) {
            	final ActualOutInformation aoInformation = (ActualOutInformation) source.customData;
            	final AoPathsNodesBitvector aoPaths = aoInformation.aoPaths;
            	final int procLocalTargetId = nodeId2ProcLocalNodeId.getInt(target.getId());

            	aoPaths.set(procLocalTargetId);
            }
        } else {
        	assert pathEdge.contains(new Edge(source, target));
        }
    }

    private boolean pathEdge_add(SDGNode source, SDGNode target) {
    	assert source.getProc() == target.getProc();
		final PathEdgeReachedNodesBitvector sources = (PathEdgeReachedNodesBitvector) target.customData;
    	final int procLocalSourceId = nodeId2ProcLocalNodeId.getInt(source.getId());
    	return sources.setWithResult(procLocalSourceId);
    }

//    if (relevantProcs != null) {
//    	if (!(relevantProcs.contains(e.source.getProc())
//    		&& relevantProcs.contains(e.target.getProc()))) {
//    		continue;
//    	} else if (graph.containsEdge(e.source, e.target)) {
//    		continue;
//    	}
//    }



    private Collection<AcutalInActualOutPair> aiaoPairs(Edge e) {
        final IntrusiveList<AcutalInActualOutPair> result = new IntrusiveList<>();

        assert e.source.getKind() == SDGNode.Kind.FORMAL_IN;
        assert e.target.getKind() == SDGNode.Kind.FORMAL_OUT || e.target.getKind() == SDGNode.Kind.EXIT;


        for (SDGEdge pi : graph.incomingEdgesOfUnsafe(e.source)) {
        	if (pi == null) continue;
            if (pi.getKind() == SDGEdge.Kind.PARAMETER_IN) {
                final SDGNode ai = pi.getSource();

                final SDGNode call = getCallSiteFor(ai);
                assert call != null;

                final AcutalInActualOutPair pair = new AcutalInActualOutPair(ai);
                call.customData = pair;
                result.add(pair);
            }
        }

        for (SDGEdge po : graph.outgoingEdgesOfUnsafe(e.target)) {
        	if (po == null) continue;
            if (po.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                final SDGNode ao = po.getTarget();

                final SDGNode call = getCallSiteFor(ao);
                assert call != null;

                final AcutalInActualOutPair newE = (AcutalInActualOutPair) call.customData;
								if (newE == null) {
									System.err.println("customData of " + call + " is null");
								}
                assert newE != null;

               	newE.setActualOut(ao);
               	call.customData = null;
            }
        }

        return result;
    }

    private SDGNode getCallSiteForSlow(SDGNode node){
        if (node.getKind() == SDGNode.Kind.ACTUAL_IN) {
            SDGNode n = node;

            while (true){
                // follow control-dependence-expression edges from the source
                // node of 'edge' to the call node
                for(SDGEdge e : graph.incomingEdgesOfUnsafe(n)){
                	if (e == null) continue;
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
                // follow control-dependence-expression
                // edges from 'node' to the call node
                for(SDGEdge e : graph.incomingEdgesOfUnsafe(n)){
                    if (e == null) continue;
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

    private SDGNode getCallSiteFor(SDGNode node){
    	assert node.getKind() == SDGNode.Kind.ACTUAL_IN || node.getKind() == SDGNode.Kind.ACTUAL_OUT;

    	final SDGEdge[] es = graph.incomingEdgesOfUnsafe(node);
    	int i = 0; while (es[i] == null) i++;
    	final SDGEdge e = es[i];
    	assert e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR;
    	assert e.getSource().getKind() == SDGNode.Kind.CALL;
    	assert e.getSource().equals(getCallSiteForSlow(node));

    	return e.getSource();
    }

    private static class Edge implements Intrusable<Edge> {
        private SDGNode source;
        private SDGNode target;

        private Edge next;

        private Edge(SDGNode s, SDGNode t) {
        	assert t.getKind() == SDGNode.Kind.FORMAL_OUT || t.getKind() == SDGNode.Kind.EXIT;
        	assert s.getProc() == t.getProc();
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

class AcutalInActualOutPair implements Intrusable<AcutalInActualOutPair> {
	private final SDGNode actualIn;
	private SDGNode actualOut;

	private AcutalInActualOutPair next;

	AcutalInActualOutPair(SDGNode ai) {
		assert ai.getKind() == SDGNode.Kind.ACTUAL_IN;
		actualIn = ai;
	}

    @Override
    public void setNext(AcutalInActualOutPair next) {
    	this.next = next;
    }

    @Override
    public AcutalInActualOutPair getNext() {
    	return next;
    }


	final void setActualOut(SDGNode ao) {
		assert ao.getKind() == SDGNode.Kind.ACTUAL_OUT;
		assert ao.getProc() == actualIn.getProc();
		actualOut = ao;
	}

	public SDGNode getActualIn() {
		return actualIn;
	}

	public SDGNode getActualOut() {
		return actualOut;
	}

	@Override
	public String toString() {
		return "(" + actualIn + ", " + actualOut + ")";
	}
}

class SummaryComputer3 implements ISummaryComputer {
	@Override
	public int compute(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress) throws CancelException {
		return SummaryComputation3.compute(pack, progress);
	}

	@Override
	public int computeAdjustedAliasDep(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress)
			throws CancelException {
		return SummaryComputation3.computeAdjustedAliasDep(pack, progress);
	}

	@Override
	public int computePureDataDep(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress)
			throws CancelException {
		return SummaryComputation3.computePureDataDep(pack, progress);
	}

	@Override
	public int computeFullAliasDataDep(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress)
			throws CancelException {
		return SummaryComputation3.computeFullAliasDataDep(pack, progress);
	}

	@Override
	public int computeNoAliasDataDep(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress)
			throws CancelException {
		return SummaryComputation3.computeNoAliasDataDep(pack, progress);
	}

	@Override
	public int computeHeapDataDep(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress)
			throws CancelException {
		return SummaryComputation3.computeHeapDataDep(pack, progress);
	}

	@Override public int getFeatures() {
		return SUPPORTS_INITIAL_WORKLIST;
	}
}
