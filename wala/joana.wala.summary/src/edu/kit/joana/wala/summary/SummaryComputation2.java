/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.IntraproceduralSlicerBackward;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class SummaryComputation2 {

    private final SDG graph;
    private final Map<SDGNode, Set<SDGNode>> entry2outs;
    private final Map<SDGNode, SDGNode> actualNodes2call;
    private final TIntSet relevantFormalIns;
    private final TIntSet relevantProcs;
    private final TIntSet fullyConnected;
    private final TIntObjectMap<List<SDGNode>> out2in;
    private final boolean rememberReached;
    private final SDGEdge.Kind sumEdgeKind;
    private final Set<SDGEdge.Kind> relevantEdges;
    private final String annotate;
    private final IntraproceduralSlicerBackward slicer;

	private SummaryComputation2(SDG graph, TIntSet relevantFormalIns,
			TIntSet relevantProcs, TIntSet fullyConnected, TIntObjectMap<List<SDGNode>> out2in,
			boolean rememberReached, SDGEdge.Kind sumEdgeKind, Set<SDGEdge.Kind> relevantEdges,
			String annotate) {
    	this.graph = graph;
    	this.entry2outs = outsByProcedures();
    	this.actualNodes2call = callsForActualNodes();
    	this.relevantFormalIns = relevantFormalIns;
    	this.relevantProcs = relevantProcs;
    	this.fullyConnected = fullyConnected;
        this.out2in = out2in;
        this.rememberReached = rememberReached;
        this.sumEdgeKind = sumEdgeKind;
        this.relevantEdges = relevantEdges;
        this.annotate = annotate;
		this.slicer = new IntraproceduralSlicerBackward(graph);
	}
	

    private Map<SDGNode, Set<SDGNode>> outsByProcedures() {
    	HashMap<SDGNode, Set<SDGNode>> map = new HashMap<SDGNode, Set<SDGNode>>();
    	TIntObjectHashMap<Set<SDGNode>> aux = new TIntObjectHashMap<Set<SDGNode>>();

    	for (SDGNode n : graph.vertexSet()) {
    		if (n.getKind() == SDGNode.Kind.ENTRY) {
    			map.put(n, null);
    			continue;
    		}
    		if (n.getKind() != SDGNode.Kind.FORMAL_OUT && n.getKind() != SDGNode.Kind.EXIT) {
    			continue;
    		}
    		final int procId = n.getProc();
    		Set<SDGNode> set = aux.get(procId);
    		if (set == null) {
    			set = new HashSet<SDGNode>();
    			aux.put(procId, set);
    		}
    		set.add(n);
    	}
    	for (SDGNode entry : map.keySet()) {
    		map.put(entry, aux.get(entry.getProc()));
    	}
    	return map;
    }
    
    private Map<SDGNode, SDGNode> callsForActualNodes() {
    	Map<SDGNode, SDGNode> ret = new HashMap<>();
    	for (SDGNode v : graph.vertexSet()) {
    		SDGNode call = getCallSiteFor(v);
    		if (call != null) {
    			ret.put(v, call);
    		}
    	}
    	System.out.println("call map: "+ret.size());
    	return ret;
    }

	private static DirectedGraph<SDGNode, DefaultEdge> extractReversedCallGraph(WorkPackage<SDG> pack) {
		SDG graph = (SDG) pack.getGraph();
		Map<SDGNode, Set<SDGNode>> entry2procs = graph.sortByProcedures();
		final DirectedGraph<SDGNode, DefaultEdge> ret = new DefaultDirectedGraph<SDGNode, DefaultEdge>(
				DefaultEdge.class);
		for (final Map.Entry<SDGNode, Set<SDGNode>> entryAndProc : entry2procs.entrySet()) {
			ret.addVertex(entryAndProc.getKey());
			for (final SDGNode n : entryAndProc.getValue()) {
				if (n.getKind() == SDGNode.Kind.CALL) {
					for (final SDGEdge callEdge : graph.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CALL)) {
						ret.addVertex(callEdge.getTarget());
						ret.addEdge(graph.getEntry(callEdge.getTarget()), entryAndProc.getKey());
					}
				}
			}
		}
		return ret;
	}

	private static DirectedGraph<Set<SDGNode>, DefaultEdge> computeSCCGraph(DirectedGraph<SDGNode, DefaultEdge> graph) {
		DirectedGraph<Set<SDGNode>, DefaultEdge> ret = new DefaultDirectedGraph<Set<SDGNode>, DefaultEdge>(DefaultEdge.class);
		KosarajuStrongConnectivityInspector<SDGNode, DefaultEdge> sccInsp = 
				new KosarajuStrongConnectivityInspector<SDGNode, DefaultEdge>(graph);
		for (Set<SDGNode> scc : sccInsp.stronglyConnectedSets()) {
			ret.addVertex(scc);
		}
		for (Set<SDGNode> scc1 : ret.vertexSet()) {
			for (Set<SDGNode> scc2 : ret.vertexSet()) {
				if (!scc1.equals(scc2) && isConnected(scc1, scc2, graph)) {
					ret.addEdge(scc1, scc2);
				}
			}
		}
		return ret;
	}

	private static boolean isConnected(Set<SDGNode> scc1, Set<SDGNode> scc2, DirectedGraph<SDGNode, DefaultEdge> graph) {
		for (SDGNode n1 : scc1) {
			for (SDGNode n2 : scc2) {
				if (graph.containsEdge(n1, n2)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static class SCCScheduler {
		CountDownLatch countdown;
		DirectedGraph<Set<SDGNode>, DefaultEdge> sccGraph;
		IProgressMonitor progress;
		Collection<SDGEdge> summary;
		SummaryComputation2 comp;
		int i = 1;
		
		public SCCScheduler(DirectedGraph<Set<SDGNode>, DefaultEdge> sccGraph, IProgressMonitor progress,
				Collection<SDGEdge> summary, SummaryComputation2 comp) {
			this.sccGraph = sccGraph;
			this.countdown = new CountDownLatch(sccGraph.vertexSet().size());
			this.progress = progress;
			this.summary = summary;
			this.comp = comp;
		}

		void start() {
			synchronized (this) {
				for (Set<SDGNode> scc : sccGraph.vertexSet()) {
					if (sccGraph.inDegreeOf(scc) == 0) {
						System.out.println("SCC "+i);
						i++;
						new SumCompThread(scc).start();
					}
				}
			}
			try {
				countdown.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		void callback(Set<SDGNode> scc, SumCompThread t) {
			Set<SDGNode> inheritedSCC = null;
			synchronized (this) {
				List<Set<SDGNode>> succs = new LinkedList<>();
				for (DefaultEdge e : sccGraph.outgoingEdgesOf(scc)) {
					succs.add(sccGraph.getEdgeTarget(e));
				}
				sccGraph.removeVertex(scc);
				for (Set<SDGNode> succ : succs) {
					if (sccGraph.inDegreeOf(succ) == 0) {
						if (inheritedSCC == null) {
							inheritedSCC = succ;
							continue;
						}
						System.out.print("SCC "+i);
						i++;
						new SumCompThread(succ).start();
					}
				}
				countdown.countDown();
			}
			if (inheritedSCC != null) {
				System.out.print("SCC "+i);
				i++;
				t.setSCC(inheritedSCC);
				// calling run() instead of start() here is intentional
				// we reuse the thread that finished the calculation for the next one
				t.run();
			}
		}
		
		class SumCompThread extends Thread {
			Set<SDGNode> scc;
			SumCompThread(Set<SDGNode> scc) {
				this.scc = scc;
			}
			
			void setSCC(Set<SDGNode> scc) {
				this.scc = scc;
			}
			
			@Override
			public void run() {
				if (scc.size() > 1) {
					System.out.println(" with size "+scc.size() + ", se: "+se+", sp: "+sp);
					try {
						comp.computeSCCSummaryEdges(summary, scc, progress);
					} catch (CancelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					System.out.println(" , se: "+se+", sp: "+sp);
					comp.computeMethodSummaryEdges(summary, scc.iterator().next(), progress);
				}
				callback(scc, this);
			}
		}
	}
static int se=0,sp=0;
	public static int compute(WorkPackage<SDG> pack, IProgressMonitor progress, boolean parallel) throws CancelException {
		
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
		
		for (SDGNode v : pack.getGraph().vertexSet()) {
			pack.getGraph().incomingEdgesOf(v);
			pack.getGraph().outgoingEdgesOf(v);
		}
		// default summary computation follows control and date dependencies
		long t = System.currentTimeMillis();
		final DirectedGraph<SDGNode, DefaultEdge> rCallGraph = extractReversedCallGraph(pack);
		DirectedGraph<Set<SDGNode>, DefaultEdge> sccGraph = computeSCCGraph(rCallGraph);

		TopologicalOrderIterator<Set<SDGNode>, DefaultEdge> topIter
				= new TopologicalOrderIterator<Set<SDGNode>, DefaultEdge>(sccGraph);
		
		SummaryComputation2 comp = new SummaryComputation2((SDG) pack.getGraph(), pack.getAllFormalInIds(),
				pack.getRelevantProcIds(), pack.getFullyConnected(), pack.getOut2In(),
				pack.getRememberReached(), SDGEdge.Kind.SUMMARY, relevantEdges, null);
		System.out.println("Summary graph computation: "+(System.currentTimeMillis()-t));
		t = System.currentTimeMillis();
		se=0;sp=0;
		Collection<SDGEdge> summary = new LinkedList<>();
		int i = 1;
		System.out.println("V: "+pack.getGraph().vertexSet().size()+
				", E: "+pack.getGraph().edgeSet().size());
		if (!parallel) {
			SCCScheduler scheduler = new SCCScheduler(sccGraph, progress, summary, comp);
			scheduler.start();
		} else {
			while (topIter.hasNext()) {
				Set<SDGNode> entries = topIter.next();
				if (entries.size() > 1) {
					System.out.println("SCC "+i+" with size "+entries.size() + ", se: "+se+", sp: "+sp);
					comp.computeSCCSummaryEdges(summary, entries, progress);
				} else {
					System.out.println("SCC "+i + ", se: "+se+", sp: "+sp);
					comp.computeMethodSummaryEdges(summary, entries.iterator().next(), progress);
				}
				i++;
			}
		}
		System.out.println("Summary computation: "+(System.currentTimeMillis()-t));

		for (SDGEdge edge : summary) {
			pack.addSummaryDep(edge.getSource().getId(), edge.getTarget().getId());
		}

		// set work package to immutable and sort summary edges
		pack.workIsDone();

		return summary.size();

		//return compute(pack, SDGEdge.Kind.SUMMARY, relevantEdges, progress);
	}


	/*public static int computeAdjustedAliasDep(WorkPackage pack, IProgressMonitor progress) throws CancelException {
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
	}*/

	/*public static int computePureDataDep(WorkPackage pack, IProgressMonitor progress) throws CancelException {
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
	}*/

	public static int computeFullAliasDataDep(WorkPackage<SDG> pack, IProgressMonitor progress, boolean parallel) throws CancelException {
		return compute(pack, progress, parallel);
	}

	/*public static int computeNoAliasDataDep(WorkPackage pack, IProgressMonitor progress) throws CancelException {
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
	}*/

	/*public static int computeHeapDataDep(WorkPackage pack, IProgressMonitor progress) throws CancelException {
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
	}*/

	/*private static int compute(WorkPackage pack, SDGEdge.Kind sumEdgeKind, Set<SDGEdge.Kind> relevantEdges,
			IProgressMonitor progress) throws CancelException {
		return compute(pack, sumEdgeKind, relevantEdges, null, progress);
	}*/

	/*private static int compute(WorkPackage pack, SDGEdge.Kind sumEdgeKind, Set<SDGEdge.Kind> relevantEdges,
			String annotate, IProgressMonitor progress) throws CancelException {
		SummaryComputation2 comp = new SummaryComputation2((SDG) pack.getGraph(), pack.getAllFormalInIds(),
				pack.getRelevantProcIds(), pack.getFullyConnected(), pack.getOut2In(),
				pack.getRememberReached(), sumEdgeKind, relevantEdges, annotate);
		Collection<SDGEdge> summary = comp.computeSummaryEdges(progress);

		for (SDGEdge edge : summary) {
			pack.addSummaryDep(edge.getSource().getId(), edge.getTarget().getId());
		}

		// set work package to immutable and sort summary edges
		pack.workIsDone();

		return summary.size();
	}*/


	private void computeMethodSummaryEdges(Collection<SDGEdge> summary, SDGNode entry, IProgressMonitor progress) {
        for (SDGNode n : entry2outs.get(entry)) {
			Collection<SDGNode> slice = slicer.slice(n);
			for (SDGNode f : slice) {
				if (f.getKind() == SDGNode.Kind.FORMAL_IN) {
                    Collection<Edge> aiaoPairs = aiaoPairs(new Edge(f, n));
                    sp++;
                    for (Edge e : aiaoPairs) {
                        if (e.source == null || e.target == null) continue;

                        boolean connectedInPDG = false;
                        Set<SDGEdge> allEdges;
                        synchronized (graph) {
                        	allEdges = graph.getAllEdges(e.source, e.target);
                        }
                        for (SDGEdge eOut : allEdges) {
                            if (eOut.getKind().isSDGEdge()) {
                                connectedInPDG = true;
                            }
                        }
                        if (connectedInPDG) continue; // already connected

                        SDGEdge sum;
                        if (annotate != null && !annotate.isEmpty()) {
                        	sum = new SDGEdge(e.source, e.target, sumEdgeKind, annotate);
                        } else {
                        	sum = new SDGEdge(e.source, e.target, sumEdgeKind);
                        }
                        se++;
                        synchronized (graph) {
	                        graph.addEdge(e.source, e.target, sum);
	                        summary.add(sum);
                        }
                    }
				}
			}
		}
	}
	
	private Collection<SDGEdge> computeSCCSummaryEdges(Collection<SDGEdge> summary,
    		Set<SDGNode> entries, IProgressMonitor progress) throws CancelException {
		LinkedList<SDGNode> sliceWorklist = new LinkedList<>();
		Map<SDGNode, Set<SDGNode>> sliceMap = new HashMap<>();
		Map<SDGNode, Set<SDGNode>> actualOutSleep = new HashMap<>();
		Map<SDGNode, Set<SDGNode>> actualInVisited = new HashMap<>();
		Map<SDGNode, Set<SDGNode>> formalInVisited = new HashMap<>();
		
        for (SDGNode entry : entries) {
        	for (SDGNode n : entry2outs.get(entry)) {
            	if (relevantProcs != null && !relevantProcs.contains(n.getProc())) {
            		continue;
            	}

            	if (fullyConnected != null && fullyConnected.contains(n.getId())) {
            		continue;
            	}
            	Set<SDGNode> outSet = new HashSet<>();
            	outSet.add(n);
                sliceWorklist.add(n);
                sliceMap.put(n, outSet);
        	}
        }
        System.out.println("S: "+sliceWorklist.size());
        int z=0;
        while (!sliceWorklist.isEmpty()) {
        	z++;
        	if (z == 2000) {
        		z = 2000;
        	}
        	SDGNode n = sliceWorklist.poll();
        	Set<SDGNode> foSet = sliceMap.get(n);
        	sliceMap.remove(n);
        	Collection<SDGNode> slice = slicer.slice(n);
			for (SDGNode f : slice) {
				switch (f.getKind()) {
					case ACTUAL_IN:
						Set<SDGNode> visitedSet = actualInVisited.get(f);
						if (visitedSet == null) {
							visitedSet = new HashSet<>();
							actualInVisited.put(f,visitedSet);
						}
						visitedSet.addAll(foSet);
						break;
					case ACTUAL_OUT:
						Set<SDGNode> sleepSet = actualOutSleep.get(f);
						if (sleepSet == null) {
							sleepSet = new HashSet<>();
							actualOutSleep.put(f, sleepSet);
						}
						sleepSet.addAll(foSet);
						break;
					case FORMAL_IN:
						Set<SDGNode> visitedFI = formalInVisited.get(f);
						if (visitedFI == null) {
							visitedFI = new HashSet<>();
							formalInVisited.put(f, visitedFI);
						}
						for (SDGNode out : foSet) {
							if (visitedFI.contains(out)) {
								continue;
							}
							visitedFI.add(out);
			                Collection<Edge> aiaoPairs = aiaoPairs(new Edge(f, out));
			                sp++;
			                for (Edge e : aiaoPairs) {
			                    if (e.source == null || e.target == null) continue;
			
			                    boolean connectedInPDG = false;
			                    Set<SDGEdge> allEdges;
			                    synchronized (graph) {
			                    	allEdges = graph.getAllEdges(e.source, e.target);
			                    }
			                    for (SDGEdge eOut : allEdges) {
			                        if (eOut.getKind().isSDGEdge()) {
			                            connectedInPDG = true;
			                        }
			                    }
			                    if (connectedInPDG) continue; // already connected
			
			                    SDGEdge sum;
			                    if (annotate != null && !annotate.isEmpty()) {
			                    	sum = new SDGEdge(e.source, e.target, sumEdgeKind, annotate);
			                    } else {
			                    	sum = new SDGEdge(e.source, e.target, sumEdgeKind);
			                    }
			                    se++;
			                    synchronized (graph) {
			                        graph.addEdge(e.source, e.target, sum);
			                        summary.add(sum);
			                    }
			                    sleepSet = actualOutSleep.get(e.target);
			                    if (sleepSet == null) {
			                    	continue;
			                    }
								Set<SDGNode> newFOSet = new HashSet<>(sleepSet);
								sleepSet.clear();
								visitedSet = actualInVisited.get(f);
								if (visitedSet == null) {
									visitedSet = new HashSet<>();
									actualInVisited.put(f,visitedSet);
								}
								newFOSet.removeAll(visitedSet);
								if (newFOSet.isEmpty()) {
									continue;
								}
								visitedSet.addAll(newFOSet);
								Set<SDGNode> oldFOSet = sliceMap.get(e.source);
								if (oldFOSet == null) {
									sliceWorklist.add(e.source);
									sliceMap.put(e.source, newFOSet);
								} else {
									oldFOSet.addAll(newFOSet);
								}
			                }
						}
						break;
					default: //no-op
				}
			}
        }
        System.out.println("Z: "+z);
		return summary;
	}


    private Collection<Edge> aiaoPairs(Edge e) {
        HashMap<SDGNode, Edge> result = new HashMap<SDGNode, Edge>();
        Set<SDGEdge> incEdges, outEdges;
        //synchronized (graph) {
        	incEdges = graph.incomingEdgesOf(e.source);
        	outEdges = graph.outgoingEdgesOf(e.target);
        //}

        for (SDGEdge pi : incEdges) {
            if (pi.getKind() == SDGEdge.Kind.PARAMETER_IN) {
                SDGNode ai = pi.getSource();

//                if (relevantProcs != null && !relevantProcs.contains(ai.getProc())) {
//            		continue;
//                }


                SDGNode call = actualNodes2call.get(ai);

                if(call != null) {
                    result.put(call, new Edge(ai, null));
                }
            }
        }

        for (SDGEdge po : outEdges) {
            if (po.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                SDGNode ao = po.getTarget();

//                if (relevantProcs != null && !relevantProcs.contains(ao.getProc())) {
//            		continue;
//                }

                SDGNode call = actualNodes2call.get(ao);

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
        if (node.getKind() == SDGNode.Kind.ACTUAL_IN) {
            SDGNode n = node;

            while (true){
            	Set<SDGEdge> edges;
            	//synchronized (graph) {
            		edges = graph.incomingEdgesOf(n);
            	//}

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
            	Set<SDGEdge> edges;
            	//synchronized (graph) {
            		edges = graph.incomingEdgesOf(n);
            	//}
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

