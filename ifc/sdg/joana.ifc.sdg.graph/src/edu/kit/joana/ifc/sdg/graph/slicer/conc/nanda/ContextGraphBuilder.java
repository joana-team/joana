/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import org.jgrapht.ext.GraphMLExporter;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.ContextGraph.ContextEdge;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.io.graphml.SDG2GraphML;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;


public class ContextGraphBuilder {

	private final Logger debug = Log.getLogger(Log.L_SDG_CALLGRAPH_DEBUG);
	
    /** A counter for the contexts. */
    private int nr;
    /** A counter for the procedure ID's. */
    private int proc;

	private final CFG icfg;
	private HashMap<DynamicContext, TopologicalNumber> visited;
	private HashMap<ContextGraph, TopologicalNumber> roots;
	private HashMap<ContextGraph, int[]> threads;

	private ContextGraph result;
	private HashMap<SDGNode, TreeSet<TopologicalNumber>> resultMap;

	public ContextGraphBuilder(CFG icfg) {
	    nr = 1;
	    proc = 1;
	    this.icfg = icfg;
	}

	public ContextGraphs buildContextGraphs(ISCRGraph[] iscrGraphs) {
		/* build the context graph */
		visited = new HashMap<DynamicContext, TopologicalNumber>();
		roots = new HashMap<ContextGraph, TopologicalNumber>();
		result = new ContextGraph();
		resultMap = new HashMap<SDGNode, TreeSet<TopologicalNumber>>();
		threads = new HashMap<ContextGraph, int[]>();

		final ContextGraph[] contextGraphs = new ContextGraph[iscrGraphs.length];
		for (int i = 0; i < iscrGraphs.length; i++) {
			contextGraphs[i] = buildContextGraph(iscrGraphs[i]);
		}

		// convert the map and add it to the result
		HashMap<SDGNode, List<TopologicalNumber>> finalMap =
			new HashMap<SDGNode, List<TopologicalNumber>>();

		for (Map.Entry<SDGNode, TreeSet<TopologicalNumber>> en : resultMap.entrySet()) {
			final SDGNode node                       = en.getKey();
			final TreeSet<TopologicalNumber> numbers = en.getValue();
			final List<TopologicalNumber> l = new ArrayList<TopologicalNumber>(numbers);
			numbers.clear();
			finalMap.put(node, l);
		}

		result.setNodeMap(finalMap);

		/* insert thread edges */
		insertThreadEdges();


		for (int i = 0; i < iscrGraphs.length; i++) {
			final SDGNode root = iscrGraphs[i].getRoot();
			final CFG cfg = iscrGraphs[i].getData();
			if (iscrGraphs[i].getRoot().getKind() == SDGNode.Kind.FOLDED) {
				// this is complicated
				assert true;
			} else {
				int[] threadsForI = threads.get(contextGraphs[i]);
				
				for (int t : threadsForI) {
					assert icfg.getThreadsInfo().getThread(t).getEntry().equals(root);
				}
			}
		}
		
		/* virtually duplicate the threads */
		ContextGraph[] results = new ContextGraph[icfg.getNumberOfThreads()];
		for (Map.Entry<ContextGraph, int[]> en : threads.entrySet()) {
			for (int t : en.getValue()) {
				results[t] = en.getKey();
			}
		}
		



		return new ContextGraphs(results, result);
	}

	private ContextGraph buildContextGraph(ISCRGraph graph) {
		debug.outln("	create context graph");
		ContextGraph cg = createContextGraph(graph);
		debug.outln("	enumerate contexts");
		enumerateContexts(cg);
		debug.outln("	enumerate procedures");
		enumerateProcedures(cg);
		debug.outln("	insert help edges");
		insertHelpEdges(cg);
		debug.outln("	create node map");
		createMap(graph, cg);
		debug.outln("	done");
		return cg;
	}

	private void insertThreadEdges() {
		@SuppressWarnings("unchecked")
		final HashSet<TopologicalNumber>[] forks =
			(HashSet<TopologicalNumber>[]) new HashSet[icfg.getNumberOfThreads()];
		for (int i = 0; i < forks.length; i++) {
			forks[i] = new HashSet<TopologicalNumber>();
		}

		HashMap<SDGNode, HashSet<TopologicalNumber>> forkSites =
			new HashMap<SDGNode, HashSet<TopologicalNumber>>();

		// insert the edges
		for (SDGEdge e : icfg.edgeSet()) {
			if (e.getKind().isThreadEdge()) {
				Collection<TopologicalNumber> sources = result.getTopologicalNumbers(e.getSource());
				Collection<TopologicalNumber> targets = result.getTopologicalNumbers(e.getTarget());

				for (TopologicalNumber s : sources) {
					for (TopologicalNumber t : targets) {
						result.addEdge(new ContextEdge(s, t, e.getKind()));
					}
				}

				// store the fork sites for later
				if (e.getKind() == SDGEdge.Kind.FORK) {
					HashSet<TopologicalNumber> s = forkSites.get(e.getTarget());
					if (s == null) {
						s = new HashSet<TopologicalNumber>();
						forkSites.put(e.getTarget(), s);
					}
					s.addAll(sources);
				}
			}
		}

		// determine which thread is called by which context
		// FIXME: this is a hack, in fact we need an analysis of the thread invocation structure
		for (Map.Entry<SDGNode, HashSet<TopologicalNumber>> en : forkSites.entrySet()) {
			SDGNode entry = en.getKey();
			HashSet<TopologicalNumber> sources = en.getValue();
			if (sources.size() == entry.getThreadNumbers().length) {
				// works in many, many cases
				int pos = 0;
				for (TopologicalNumber s : sources) {
					int thread = entry.getThreadNumbers()[pos];
					forks[thread].add(s);
					pos++;
				}

			} else {
				// fallback, sound but imprecise
				for (int thread : entry.getThreadNumbers()) {
					forks[thread].addAll(sources);
				}
			}
		}

		result.setForkSites(forks);

		if (debug.isEnabled()) {
			for (int i = 0; i < forks.length; i++) {
				debug.outln("thread " + i + ": " + forks[i]);
			}
		}
	}

	private ContextGraph createContextGraph(ISCRGraph graph) {
		visited.clear();
		HashMap<DynamicContext, DynamicContext> returnSites = new HashMap<DynamicContext, DynamicContext>();
		visited = new HashMap<DynamicContext, TopologicalNumber>();
		LinkedList<DynamicContext> wl = new LinkedList<DynamicContext>();
		SDGNode root = graph.getRoot();
		DynamicContext c = new DynamicContext(root, 0);//graph.getID());
		TopologicalNumber rootNr = new TopologicalNumber();
		ContextGraph cg = new ContextGraph();
		roots.put(cg, rootNr);
		cg.addContext(rootNr);
		result.addContext(rootNr);
		threads.put(cg, root.getThreadNumbers());

		visited.put(c, rootNr);
		wl.add(c);

		HashMap<SDGNode, List<DynamicContext>> tmp = new HashMap<SDGNode, List<DynamicContext>>();
		while (!wl.isEmpty()) {
			final DynamicContext next = wl.poll();
			
			// We expect the ISCRGraph graph to be loop free w.r.t recursive calls.
			// Specifically, we never enter an entry node which is alread on the call stack:
			if (next.stackContains(next.getNode())) {
				throw new IllegalArgumentException();
			}
			final TopologicalNumber nextNr = visited.get(next);
			assert nextNr != null;

			List<DynamicContext> lll = tmp.get(next.getNode());
			if (lll == null) {
				lll = new LinkedList<DynamicContext>();
				lll.add(next);
				tmp.put(next.getNode(), lll);
			} else {
				lll.add(next);
			}


			for (SDGEdge e : graph.outgoingEdgesOf(next.getNode())) {
				if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
					DynamicContext succ = next.copy();
					succ.setNode(e.getTarget());

					TopologicalNumber succNr = visited.get(succ);
					if (succNr == null) {
						succNr = new TopologicalNumber();
						cg.addContext(succNr);
						result.addContext(succNr);
						visited.put(succ, succNr);
						wl.add(succ);
					}

					cg.addEdge(new ContextEdge(nextNr, succNr, SDGEdge.Kind.CONTROL_FLOW));
					result.addEdge(new ContextEdge(nextNr, succNr, SDGEdge.Kind.CONTROL_FLOW));

				} else if (e.getKind() == SDGEdge.Kind.CALL) {
					DynamicContext succ = next.copy();
					succ.push(next.getNode());
					succ.setNode(e.getTarget());

					TopologicalNumber succNr = visited.get(succ);
					if (succNr == null) {
						succNr = new TopologicalNumber();
						cg.addContext(succNr);
						result.addContext(succNr);
						visited.put(succ, succNr);
						wl.add(succ);
					}

					cg.addEdge(new ContextEdge(nextNr, succNr, SDGEdge.Kind.CALL));
					result.addEdge(new ContextEdge(nextNr, succNr, SDGEdge.Kind.CALL));

				} else if (e.getKind() == SDGEdge.Kind.NO_FLOW) {
					DynamicContext succ = next.copy();
					succ.setNode(e.getTarget());

					TopologicalNumber succNr = visited.get(succ);
					if (succNr == null) {
						succNr = new TopologicalNumber();
						cg.addContext(succNr);
						result.addContext(succNr);
						visited.put(succ, succNr);
						wl.add(succ);
					}

					cg.addEdge(new ContextEdge(nextNr, succNr, SDGEdge.Kind.NO_FLOW));
					result.addEdge(new ContextEdge(nextNr, succNr, SDGEdge.Kind.NO_FLOW));
					returnSites.put(succ, next);

				} else if (e.getKind() != SDGEdge.Kind.RETURN) {
					throw new RuntimeException("Wrong kind of edge: "+e);
				}
			}
		}

		// create return edges
		for (DynamicContext retSite : returnSites.keySet()) {
			for (SDGEdge e : graph.incomingEdgesOf(retSite.getNode())) {
				if (e.getKind() == SDGEdge.Kind.RETURN) {
					DynamicContext pred = retSite.copy();
					pred.push(returnSites.get(retSite).getNode());
					pred.setNode(e.getSource());

					TopologicalNumber predNr = visited.get(pred);
					TopologicalNumber retNr = visited.get(retSite);

					cg.addEdge(new ContextEdge(predNr, retNr, SDGEdge.Kind.RETURN));
					result.addEdge(new ContextEdge(predNr, retNr, SDGEdge.Kind.RETURN));
				}
			}
		}

		return cg;
	}

	private void createMap(ISCRGraph graph, ContextGraph cg) {
		// create a map SDGNode -> TopologicalNumbers
		HashMap<SDGNode, List<TopologicalNumber>> iscrTNr = new HashMap<SDGNode, List<TopologicalNumber>>();
		HashMap<SDGNode, List<TopologicalNumber>> sdgTNr  = new HashMap<SDGNode, List<TopologicalNumber>>();

		// map ISCR nodes to TopologicalNumbers
		for (Map.Entry<DynamicContext, TopologicalNumber> en : visited.entrySet()) {
			SDGNode node = en.getKey().getNode();
			List<TopologicalNumber> nrs = iscrTNr.get(node);

			if (nrs == null) {
				nrs = new LinkedList<TopologicalNumber>();
				iscrTNr.put(node, nrs);
			}

			nrs.add(en.getValue());
		}

		// map SDG nodes to TopologicalNumbers
		for (SDGNode n : icfg.vertexSet()) {
			List<SDGNode> iscrNodes = graph.getISCRNodes(n);

			if (iscrNodes == null) continue; // node from another thread, could be done nicer

			// sort the TopologicalNumbers
			TreeSet<TopologicalNumber> tmp = new TreeSet<TopologicalNumber>(TopologicalNumber.getComparator());
			List<TopologicalNumber> nrs = sdgTNr.get(n);
			if (nrs == null) {
				nrs = new LinkedList<TopologicalNumber>();
				sdgTNr.put(n, nrs);
			}

			for (SDGNode iscr : iscrNodes) {
				List<TopologicalNumber> l = iscrTNr.get(iscr);
				if (l != null) {
					tmp.addAll(l);
				}
			}
			nrs.addAll(tmp);

			// update the global map
			TreeSet<TopologicalNumber> global = resultMap.get(n);
			if (global == null) {
				global = new TreeSet<TopologicalNumber>(TopologicalNumber.getComparator());
				resultMap.put(n, global);
			}
			global.addAll(tmp);
		}

		cg.setNodeMap(sdgTNr);
	}

	private void enumerateContexts(ContextGraph cg) {
		LinkedList<TopologicalNumber> wl = new LinkedList<TopologicalNumber>();
		TopologicalNumber root = roots.get(cg);
		wl.add(root);
		root.setNumber(nr);
		nr++;

		while (!wl.isEmpty()) {
			TopologicalNumber next = wl.poll();

			for (ContextEdge e : cg.outgoingEdgesOf(next)) {
				TopologicalNumber succ = e.getTarget();

    			// enumerate if succ has not been visited yet
				// and all of its predecessors have been visited
				if (succ.getNumber() == -1) {
    				boolean ok = true;

        			for (ContextEdge f : cg.incomingEdgesOf(succ)) {
        				if (f.getSource().getNumber() == -1) {
        					// a predecessor has not been visited yet
        					ok = false;
        					break;
        				}
        			}

        			if (ok) {
        				wl.add(succ);
        	    		succ.setNumber(nr);
        	    		nr++;
        			}
				}
			}
		}
	}

	private void enumerateProcedures(ContextGraph cg) {
		HashSet<TopologicalNumber> visited = new HashSet<TopologicalNumber>();
		LinkedList<TopologicalNumber> outer = new LinkedList<TopologicalNumber>();
		LinkedList<TopologicalNumber> inner = new LinkedList<TopologicalNumber>();

		TopologicalNumber root = roots.get(cg);
		outer.add(root);
		visited.add(root);

		while (!outer.isEmpty()) {
			inner.add(outer.poll());

    		while (!inner.isEmpty()) {
    			TopologicalNumber next = inner.poll();
    			next.setProcID(proc);

    			for (ContextEdge e : cg.outgoingEdgesOf(next)) {
        			// traverse only downwards
    				if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW || e.getKind() == SDGEdge.Kind.NO_FLOW) {
    					TopologicalNumber succ = e.getTarget();

	    				if (visited.add(succ)) {
		        			inner.add(succ);
	    				}

    				} else if (e.getKind() == SDGEdge.Kind.CALL) {
    					TopologicalNumber succ = e.getTarget();

    					if (visited.add(succ)) {
    						outer.add(succ);
    					}
    				}
    			}
    		}

    		proc++;
		}
	}

	// edges from a call node to the exit of the called procedure
    private void insertHelpEdges(ContextGraph cg) {
    	LinkedList<ContextEdge> helpEdges = new LinkedList<ContextEdge>();
    	HashMap<Integer, TopologicalNumber> exits = new HashMap<Integer, TopologicalNumber>();
    	Collection<ContextEdge> edges = cg.getAllEdges();

    	for (ContextEdge e : edges) {
    		if (e.getKind() == SDGEdge.Kind.RETURN) {
    			TopologicalNumber exit = e.getSource();
    			exits.put(exit.getProcID(), exit);
    		}
    	}

    	for (ContextEdge e : edges) {
    		if (e.getKind() == SDGEdge.Kind.CALL) {
    			TopologicalNumber call = e.getSource();
    			TopologicalNumber exit = exits.get(e.getTarget().getProcID());
    			// there may be no corresponding exit node if it is unreachable (e.g., due to a while(true) loop)
    			if (exit != null) {
    				helpEdges.add(new ContextEdge(call, exit, SDGEdge.Kind.HELP));
    			}
    		}
    	}

    	for (ContextEdge e : helpEdges) {
    		cg.addEdge(e);
    	}
    }

	public static ContextGraphs build(CFG icfg) {
		/* 1. Build ISCR graphs */
		ISCRBuilder iscrBuilder = new ISCRBuilder();
		ISCRGraph[] iscrGraphs = iscrBuilder.buildISCRGraphs(icfg);

		/* 2. Build context graphs */
		ContextGraphBuilder builder = new ContextGraphBuilder(icfg);
		ContextGraphs contextGraphs = builder.buildContextGraphs(iscrGraphs);

		return contextGraphs;
	}


    public static void main(String[] args) throws Exception {
    	for (String file : PDGs.pdgs) {
    		System.out.println("********************");
    		System.out.println(file);
            SDG g = SDG.readFrom(file);

            CFG cfg = ICFGBuilder.extractICFG(g);
    		System.out.println("ICFG");
    		System.out.println("	nodes: "+cfg.vertexSet().size());
    		System.out.println("	edges: "+cfg.edgeSet().size());

    		long time = System.currentTimeMillis();
            ContextGraphs cg = build(cfg);
            time = System.currentTimeMillis() - time;
    		System.out.println("Context Graph");
    		System.out.println("	nodes: "+cg.getWholeGraph().getAllNodes().size());
    		System.out.println("	edges: "+cg.getWholeGraph().getAllEdges().size());

    		System.out.println("time: "+time);
        }
    }
}
