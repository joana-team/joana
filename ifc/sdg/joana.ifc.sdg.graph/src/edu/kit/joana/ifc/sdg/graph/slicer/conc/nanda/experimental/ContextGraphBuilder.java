/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.ISCRBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.ISCRGraph;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental.ContextGraph.ContextEdge;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;


public class ContextGraphBuilder {

	private final Logger debug = Log.getLogger(Log.L_SDG_CALLGRAPH_DEBUG);
	
    /** A counter for the contexts. */
    private int nr;
    /** A counter for the procedure IDs. */
    private int proc;

	private final CFG icfg;
	private HashMap<DynamicContext, TopologicalNumber> visited;
	private HashMap<ContextGraph, TopologicalNumber> roots;

	private ContextGraph result;
	private HashMap<SDGNode, TreeSet<TopologicalNumber>> resultMap;

	public ContextGraphBuilder(CFG icfg) {
	    nr = 1;
	    proc = 1;
	    this.icfg = icfg;
	}

	public ContextGraphs buildContextGraphs(Map<SDGNode, ISCRGraph> iscrGraphs) {
		/* build the context graph */
		visited = new HashMap<DynamicContext, TopologicalNumber>();
		roots = new HashMap<ContextGraph, TopologicalNumber>();
		result = new ContextGraph();
		resultMap = new HashMap<SDGNode, TreeSet<TopologicalNumber>>();

		final Map<SDGNode, ContextGraph> contextGraphs = new HashMap<>(iscrGraphs.size());
		for (Entry<SDGNode, ISCRGraph> eISCR : iscrGraphs.entrySet()) {
			final SDGNode entry       = eISCR.getKey();
			final ISCRGraph iscrGraph = eISCR.getValue();
			contextGraphs.put(entry, buildContextGraph(iscrGraph));
		}

		// convert the map and add it to the result
		HashMap<SDGNode, LinkedList<TopologicalNumber>> finalMap =
			new HashMap<SDGNode, LinkedList<TopologicalNumber>>();

		for (Map.Entry<SDGNode, TreeSet<TopologicalNumber>> en : resultMap.entrySet()) {
			LinkedList<TopologicalNumber> l = new LinkedList<TopologicalNumber>();
			l.addAll(en.getValue());
			finalMap.put(en.getKey(), l);
		}

		result.setNodeMap(finalMap);

		/* insert thread edges */
		insertThreadEdges();

		/* virtually duplicate the threads */
		ContextGraph[] results = new ContextGraph[icfg.getNumberOfThreads()];
		for (int t = 0; t < results.length; t++) {
			final ThreadInstance threadInstance = icfg.getThreadsInfo().getThread(t);
			assert threadInstance.getId() == t;
			
			final ContextGraph contextGraph = contextGraphs.get(threadInstance.getEntry());
			assert  contextGraph != null;
			results[t] = contextGraph;
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
				debug.outln("thread "+i+": "+forks[i]);
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

		visited.put(c, rootNr);
		wl.add(c);

		HashMap<SDGNode, List<DynamicContext>> tmp = new HashMap<SDGNode, List<DynamicContext>>();
		while (!wl.isEmpty()) {
			DynamicContext next = wl.poll();
			TopologicalNumber nextNr = visited.get(next);

			List<DynamicContext> lll = tmp.get(next.getNode());
			if (lll == null) {
				lll = new LinkedList<DynamicContext>();
				lll.add(next);
				tmp.put(next.getNode(), lll);
			} else {
				for (DynamicContext d : lll) {
					if (next.isSuffixOf(d) || d.isSuffixOf(next)) {
						// rekursion!
						Log.ERROR.outln(d + "\n subsumes " + next);
						throw new RuntimeException();
					}
				}
				lll.add(next);
			}


			for (SDGEdge e : graph.outgoingEdgesOf(next.getNode())) {
				if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
					DynamicContext succ = next.copyWithNewNode(e.getTarget());

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
					DynamicContext succ = next.copyWithNewNode(e.getTarget());
					succ.push(next.getNode());

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
					DynamicContext succ = next.copyWithNewNode(e.getTarget());

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
					DynamicContext pred = retSite.copyWithNewNode(e.getSource());
					pred.push(returnSites.get(retSite).getNode());

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
		HashMap<SDGNode, LinkedList<TopologicalNumber>> sdgTNr = new HashMap<SDGNode, LinkedList<TopologicalNumber>>();

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
			LinkedList<TopologicalNumber> nrs = sdgTNr.get(n);
			if (nrs == null) {
				nrs = new LinkedList<TopologicalNumber>();
				sdgTNr.put(n, nrs);
			}

			for (SDGNode iscr : iscrNodes) {
				List<TopologicalNumber> l = iscrTNr.get(iscr);
				tmp.addAll(l);
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
    			helpEdges.add(new ContextEdge(call, exit, SDGEdge.Kind.HELP));
    		}
    	}

    	for (ContextEdge e : helpEdges) {
    		cg.addEdge(e);
    	}
    }

    public static void main(String[] args) throws Exception {
    	for (String file : PDGs.pdgs) {
    		System.out.println(file);
            SDG g = SDG.readFrom(file);
            CFG cfg = ICFGBuilder.extractICFG(g);
            build(cfg);
        }
    }

	public static ContextGraphs build(CFG icfg) {
		/* 1. Build ISCR graphs */
		ISCRBuilder iscrBuilder = new ISCRBuilder();
		Map<SDGNode, ISCRGraph> iscrGraphs = iscrBuilder.buildISCRGraphs(icfg);

		/* 2. Build context graphs */
		ContextGraphBuilder builder = new ContextGraphBuilder(icfg);
		ContextGraphs contextGraphs = builder.buildContextGraphs(iscrGraphs);

		return contextGraphs;
	}
}
