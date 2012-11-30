/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.regions;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.regions.ContextGraph.ContextEdge;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;


public class ContextGraphBuilder {
	private static boolean DEBUG = false;

    /** A counter for the contexts. */
    private int nr;
    /** A counter for the procedure ID's. */
    private int proc;
    /** The currently processed ISCR graph. */
	private ISCRGraph graph;

	private CFG icfg;


	public ContextGraphBuilder(CFG icfg) {
	    nr = 1;
	    proc = 1;
	    this.icfg = icfg;
	}

	public ContextGraph[] buildContextGraphs(ISCRGraph[] iscrGraphs) {
		contextGraphs = new ContextGraph[iscrGraphs.length];
		visited = new HashMap[iscrGraphs.length];
		for (ISCRGraph g : iscrGraphs) {
			graph = g;
			contextGraphs[graph.getID()] = buildContextGraph();
		}
		return contextGraphs;
	}

	private ContextGraph buildContextGraph() {
		if (DEBUG) System.out.println("	create context graph");
		ContextGraph cg = createContextGraph();
		if (DEBUG) System.out.println("	enumerate contexts");
		enumerateContexts(cg);
		if (DEBUG) System.out.println("	enumerate procedures");
		enumerateProcedures(cg);
		if (DEBUG) System.out.println("	insert help edges");
		insertHelpEdges(cg);
		if (DEBUG) System.out.println("	create node map");
		createMap(cg);
		if (DEBUG) System.out.println("	done");
		return cg;
	}

	HashMap<DynamicContext, TopologicalNumber>[] visited;
	ContextGraph[] contextGraphs;

	private ContextGraph createContextGraph() {
		HashMap<DynamicContext, DynamicContext> returnSites = new HashMap<DynamicContext, DynamicContext>();
		visited[graph.getID()] = new HashMap<DynamicContext, TopologicalNumber>();
		LinkedList<DynamicContext> wl = new LinkedList<DynamicContext>();
		SDGNode root = graph.getRoot();
		DynamicContext c = new DynamicContext(root, graph.getID());
		TopologicalNumber rootNr = new TopologicalNumber(graph.getID());
		ContextGraph cg = new ContextGraph(rootNr);

		visited[graph.getID()].put(c, rootNr);
		wl.add(c);

		HashMap<SDGNode, List<DynamicContext>> tmp = new HashMap<SDGNode, List<DynamicContext>>();
		while (!wl.isEmpty()) {
			DynamicContext next = wl.poll();
			TopologicalNumber nextNr = visited[graph.getID()].get(next);

			List<DynamicContext> lll = tmp.get(next.getNode());
			if (lll == null) {
				lll = new LinkedList<DynamicContext>();
				lll.add(next);
				tmp.put(next.getNode(), lll);
			} else {
				for (DynamicContext d : lll) {
					if (next.isSuffixOf(d) || d.isSuffixOf(next)) {
						// rekursion!
						System.out.println(d+"\n subsumes "+next);
						throw new RuntimeException();
					}
				}
				lll.add(next);
			}


			for (SDGEdge e : graph.outgoingEdgesOf(next.getNode())) {
				if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
					DynamicContext succ = next.copy();
					succ.setNode(e.getTarget());

					TopologicalNumber succNr = visited[graph.getID()].get(succ);
					if (succNr == null) {
						succNr = new TopologicalNumber(graph.getID());
						cg.addContext(succNr);
						visited[graph.getID()].put(succ, succNr);
						wl.add(succ);
					}

					cg.addEdge(new ContextEdge(nextNr, succNr, SDGEdge.Kind.CONTROL_FLOW));

				} else if (e.getKind() == SDGEdge.Kind.CALL) {
					DynamicContext succ = next.copy();
					succ.push(next.getNode());
					succ.setNode(e.getTarget());

					TopologicalNumber succNr = visited[graph.getID()].get(succ);
					if (succNr == null) {
						succNr = new TopologicalNumber(graph.getID());
						cg.addContext(succNr);
						visited[graph.getID()].put(succ, succNr);
						wl.add(succ);
					}

					cg.addEdge(new ContextEdge(nextNr, succNr, SDGEdge.Kind.CALL));

				} else if (e.getKind() == SDGEdge.Kind.NO_FLOW) {
					DynamicContext succ = next.copy();
					succ.setNode(e.getTarget());

					TopologicalNumber succNr = visited[graph.getID()].get(succ);
					if (succNr == null) {
						succNr = new TopologicalNumber(graph.getID());
						cg.addContext(succNr);
						visited[graph.getID()].put(succ, succNr);
						wl.add(succ);
					}

					cg.addEdge(new ContextEdge(nextNr, succNr, SDGEdge.Kind.NO_FLOW));
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

					TopologicalNumber predNr = visited[graph.getID()].get(pred);
					TopologicalNumber retNr = visited[graph.getID()].get(retSite);

					cg.addEdge(new ContextEdge(predNr, retNr, SDGEdge.Kind.RETURN));
				}
			}
		}

		return cg;
	}

	private void createMap(ContextGraph cg) {
		// create a map SDGNode -> TopologicalNumbers
		HashMap<SDGNode, List<TopologicalNumber>> iscrTNr = new HashMap<SDGNode, List<TopologicalNumber>>();
		HashMap<SDGNode, LinkedList<TopologicalNumber>> sdgTNr = new HashMap<SDGNode, LinkedList<TopologicalNumber>>();

		// map ISCR nodes to TopologicalNumbers
		for (Map.Entry<DynamicContext, TopologicalNumber> en : visited[graph.getID()].entrySet()) {
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
		}

		cg.setNodeMap(sdgTNr);
	}

	private void enumerateContexts(ContextGraph cg) {
		LinkedList<TopologicalNumber> wl = new LinkedList<TopologicalNumber>();
		TopologicalNumber root = cg.getRoot();
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

		TopologicalNumber root = cg.getRoot();
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
            ContextGraphs cg = ContextGraphs.build(cfg);

			testProcedures(cfg, cg);
			testEdges(cfg, cg);
        }
    }

    private static void testProcedures(CFG icfg, ContextGraphs cg) {
    	// test SDGNodes
    	List<CFG> cfgs = icfg.split();
    	for (CFG g : cfgs) {
    		int size = -1;
    		for (SDGNode n : g.vertexSet()) {
    			Collection<TopologicalNumber> nrs = cg.getTopologicalNumbers(n);
    			if (size == -1) {
    				size = nrs.size();

    			} else if (size != nrs.size()) {
    				System.out.println("***********");
    				for (SDGNode m : g.vertexSet()) {
    	    			System.out.println(cg.getTopologicalNumbers(m));
    				}
    				throw new RuntimeException();
    			}
    		}
    	}
    }

    private static void testEdges(CFG icfg, ContextGraphs cg) {
    	 for (SDGEdge e : icfg.edgeSet()) {
         	Collection<TopologicalNumber> s = cg.getTopologicalNumbers(e.getSource());
         	Collection<TopologicalNumber> t = cg.getTopologicalNumbers(e.getTarget());

         	if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
         		SDGNode source = e.getSource();
         		SDGNode target = e.getTarget();

         		if (source.getKind() == SDGNode.Kind.ACTUAL_IN
         				|| source.getKind() == SDGNode.Kind.FORMAL_IN
         				|| source.getKind() == SDGNode.Kind.ACTUAL_OUT
         				|| source.getKind() == SDGNode.Kind.FORMAL_OUT
         				|| target.getKind() == SDGNode.Kind.ACTUAL_IN
         				|| target.getKind() == SDGNode.Kind.FORMAL_IN
         				|| target.getKind() == SDGNode.Kind.ACTUAL_OUT
         				|| target.getKind() == SDGNode.Kind.FORMAL_OUT) continue;

         		if (source.getKind() == SDGNode.Kind.ENTRY && target.getKind() == SDGNode.Kind.EXIT) continue;

         		for (TopologicalNumber nr : t) {
         			Collection<TopologicalNumber> preds = cg.getPredecessorsPlusNoFlow(nr);
         			boolean ok = false;

         			for (TopologicalNumber mr : s) {
         				if (mr == nr || (mr.getProcID() == nr.getProcID() && preds.contains(mr))) {
         					ok = true;
         				}
         			}

         			if (!ok) {
         				System.out.println("****************");
 	            		System.out.println(e);
 	            		System.out.println("target "+nr);
 	            		System.out.println("preds "+preds);
 	            		System.out.println("sources "+s);
         			}
         		}
         	}
         }
    }
}
