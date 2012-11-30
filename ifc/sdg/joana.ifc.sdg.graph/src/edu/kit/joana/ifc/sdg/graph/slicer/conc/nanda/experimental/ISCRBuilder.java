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
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.FoldedCFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.CallGraphBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;


/** Builds ISCR graphs.
 *
 * -- Created on May 16, 2006
 *
 * @author  Dennis Giffhorn
 */
public class ISCRBuilder {
	private static final boolean DEBUG = false;
	private static final boolean VERIFY = false;

    private HashMap<SDGNode, SDGNode> params; // parameter node -> call, return, entry or exit node
    private HashMap<SDGNode, Collection<SDGNode>> cache;
    private HashMap<SDGNode, HashSet<SDGNode>> iscrMap; // ISCR node -> represented nodes
    private LinkedList<SDGNode> returnNodes;

    /** A new ISCR builders. */
    public ISCRBuilder() { }

    /** Constructs an ISCR Graph from a TCFG.
     *
     * @param icfg  The ICFG.
     * @return  An array of ISCR graphs, one for each thread.
     */
    public ISCRGraph[] buildISCRGraphs(CFG tcfg) {
        // an array for the data of the ISCR graphs to build, one for every thread instance
        CFG[] icfgs = splitTCFG(tcfg);
        ISCRGraph[] iscrs = new ISCRGraph[icfgs.length];

        // build the ISCR graphs
        for (int i = 0; i < icfgs.length; i++) {
            iscrs[i] = build(icfgs[i]);
        }

        return iscrs;
    }

    /** Builds an ISCR graph for one thread of an ICFG.
     * @param icfg  The ICFG.
     * @param thread  The thread.
     * @return  An ISCR graph.
     */
    private ISCRGraph build(CFG icfg) {
    	if (DEBUG) System.out.println("Building ISCR graph for "+icfg.getRoot());
    	params = new HashMap<SDGNode, SDGNode>();
        cache = new HashMap<SDGNode, Collection<SDGNode>>();
        iscrMap = new HashMap<SDGNode, HashSet<SDGNode>>();
        returnNodes = new LinkedList<SDGNode>();

        // preprocess
    	if (DEBUG) System.out.println("	preprocessing...");
        CFG newICFG = prepareICFG(icfg, icfg.lastId() + 1);
        if (VERIFY) paramsTest(icfg);

        // 2-pass-folding
    	if (DEBUG) System.out.println("	folding...");
        FoldedCFG folded = GraphFolder.twoPassFolding(newICFG);
        if (VERIFY) twoPassTest(newICFG, folded);

        // inlining of procedures - we need a folded call graph
    	if (DEBUG) System.out.println("	inlining procedures...");
        CFG entryGraph = CallGraphBuilder.buildEntryGraph(icfg);
        removeForkEdges(entryGraph);
        cache = inlining(entryGraph);

        // reduce the ICFG
    	if (DEBUG) System.out.println("	creating reduced ICFG...");
    	CFG reduced = createReducedICFG(folded);
    	if (VERIFY) testReducedICFG(folded, reduced);

        // compute a mapping: node-->ISCR node
    	if (DEBUG) System.out.println("	mapping SDG nodes to ISCR nodes...");
        Map<SDGNode, List<SDGNode>> nodeMap = revertISCRMap(icfg);
        if (VERIFY) testNodeMap(nodeMap, icfg, reduced);

        // create the ISCR graph
    	if (DEBUG) System.out.println("	creating ISCR graph...");
        ISCRGraph iscrGraph = new ISCRGraph(reduced, icfg.getRoot().getThreadNumbers(),
        									reduced.getRoot(), nodeMap, params);
        if (VERIFY) testISCRGraph(nodeMap, iscrGraph);
        if (VERIFY) testISCRGraphNodes(icfg, iscrGraph);

        return iscrGraph;
    }

    private CFG[] splitTCFG(CFG tcfg) {
    	Collection<SDGNode> entries = tcfg.getThreadsInfo().getAllEntries();

    	// the ICFGs of this TSCG
        CFG[] icfgs = new CFG[entries.size()];
        for (int i = 0; i < icfgs.length; i++) {
            icfgs[i] = new CFG();
        }

        // distribute the nodes and edges to the ICFGs
        int i = 0;
        for (SDGNode entry : entries) {
        	LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
        	HashSet<SDGEdge> visited = new HashSet<SDGEdge>();

        	// collect and add the nodes and edges
        	wl.add(entry);
        	icfgs[i].addVertex(entry);

        	while (!wl.isEmpty()) {
        		SDGNode n = wl.poll();

        		for (SDGEdge edge : tcfg.outgoingEdgesOf(n)) {
        			if (edge.getKind() == SDGEdge.Kind.FORK
                            || edge.getKind() == SDGEdge.Kind.JOIN) {

                        continue;
                    }

        			visited.add(edge);

        			if (edge.getKind() != SDGEdge.Kind.RETURN) {
        				if (icfgs[i].addVertex(edge.getTarget())) {
        					wl.add(edge.getTarget());
        				}
        			}
        		}
        	}

        	for (SDGEdge e : visited) {
        		if (icfgs[i].containsVertex(e.getTarget())) {
        			icfgs[i].addEdge(e);
        		}
        	}

        	// increase counter
        	i++;
        }

        return icfgs;
    }

    private void removeForkEdges(CFG entryGraph) {
    	LinkedList<SDGEdge> remove = new LinkedList<SDGEdge>();
    	for (SDGEdge e : entryGraph.edgeSet()) {
    		if (e.getKind() == SDGEdge.Kind.FORK) {
    			remove.add(e);
    		}
    	}
    	entryGraph.removeAllEdges(remove);
    }

    /** Commits a set of preprocessing steps on a ICFG, before the ISCR folding
     * algorithm can start.
     * - it creates a clone, so the original ICFG will not be touched
     * - then it removes  the redurn edges, adds dummy return sites and computes
     *   new return edges (all on the clone)
     * @param icfg  The  ICFG.
     * @param ctr  A counter for enumerating the new dummy return nodes.
     * @return  The modified clone.
     */
    private CFG prepareICFG(CFG icfg, int ctr) {
        // create an icfg clone
        CFG newICFG = icfg.clone();

        // remove parameter nodes
        params = paramNodes(newICFG);

        return newICFG;
    }

    private HashMap<SDGNode, SDGNode> paramNodes(CFG cfg) {
    	HashMap<SDGNode, SDGNode> map = new HashMap<SDGNode, SDGNode>();
    	LinkedList<SDGEdge> remove = new LinkedList<SDGEdge>();
    	LinkedList<SDGEdge> add = new LinkedList<SDGEdge>();

    	// remove act-ins and form-outs
    	for (SDGNode n : cfg.vertexSet()) {
    		if (n.getKind() == SDGNode.Kind.CALL) {
    			LinkedList<SDGNode> ps = new LinkedList<SDGNode>();
    			LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
    			wl.add(n);

    			while (!wl.isEmpty()) {
    				SDGNode next = wl.poll();

    				for (SDGEdge e : cfg.getIncomingEdgesOfKind(next, SDGEdge.Kind.CONTROL_FLOW)) {
    					SDGNode source = e.getSource();

    					if (source.getKind() == SDGNode.Kind.ACTUAL_IN) {
    						wl.add(source);
    						ps.add(source);
    						remove.add(e);

    					} else if (n != next) {
    						add.add(new SDGEdge(source, n, SDGEdge.Kind.CONTROL_FLOW));
    					}
    				}
    			}

    			// store params
    			for (SDGNode p : ps) {
    				map.put(p, n);
    			}

    		} else if (n.getKind() == SDGNode.Kind.EXIT) {
    			LinkedList<SDGNode> ps = new LinkedList<SDGNode>();
    			LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
    			wl.add(n);

    			while (!wl.isEmpty()) {
    				SDGNode next = wl.poll();

    				for (SDGEdge e : cfg.getIncomingEdgesOfKind(next, SDGEdge.Kind.CONTROL_FLOW)) {
    					SDGNode source = e.getSource();

    					if (source.getKind() == SDGNode.Kind.FORMAL_OUT || source.getKind() == SDGNode.Kind.EXIT) {
    						wl.add(source);
    						ps.add(source);
    						remove.add(e);

    					} else if (n != next) {
    						add.add(new SDGEdge(source, n, SDGEdge.Kind.CONTROL_FLOW));
    					}
    				}
    			}

    			// store params
    			for (SDGNode p : ps) {
    				map.put(p, n);
    			}
    		}
    	}

    	cfg.removeAllEdges(remove);
    	for (SDGEdge e : remove) {
    		cfg.removeVertex(e.getSource());
    	}
    	remove.clear();

    	cfg.addAllEdges(add);
    	add.clear();

    	// remove act-outs and form-ins
    	for (SDGNode n : cfg.vertexSet()) {
    		if (n.getKind() == SDGNode.Kind.CALL) {
    			for (SDGEdge ex : cfg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CONTROL_FLOW)) {
    				SDGNode retSite = ex.getTarget();// return site
	    			LinkedList<SDGNode> ps = new LinkedList<SDGNode>();
	    			LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
	    			wl.add(retSite);

	    			while (!wl.isEmpty()) {
	    				SDGNode next = wl.poll();

	    				for (SDGEdge e : cfg.getOutgoingEdgesOfKind(next, SDGEdge.Kind.CONTROL_FLOW)) {
	    					SDGNode target = e.getTarget();

	    					if (target.getKind() == SDGNode.Kind.ACTUAL_OUT) {
	    						wl.add(target);
	    						ps.add(target);
	    						remove.add(e);

	    					} else {
	    						add.add(new SDGEdge(retSite, target, SDGEdge.Kind.CONTROL_FLOW));
	    					}
	    				}
	    			}

	    			// store params
	    			for (SDGNode p : ps) {
	    				map.put(p, retSite);
	    			}
	    			// map the return site (an actual-out node) to itself
	    			// this simplifies the treatment of parameter nodes tremendously
	    			map.put(retSite, retSite);
	    			returnNodes.add(retSite);
    			}

    		} else if (n.getKind() == SDGNode.Kind.ENTRY) {
    			LinkedList<SDGNode> ps = new LinkedList<SDGNode>();
    			LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
    			wl.add(n);

    			while (!wl.isEmpty()) {
    				SDGNode next = wl.poll();

    				for (SDGEdge e : cfg.getOutgoingEdgesOfKind(next, SDGEdge.Kind.CONTROL_FLOW)) {
    					SDGNode target = e.getTarget();

    					if (target.getKind() == SDGNode.Kind.FORMAL_IN) {
    						wl.add(target);
    						ps.add(target);
    						remove.add(e);

    					} else {
    						add.add(new SDGEdge(n, target, SDGEdge.Kind.CONTROL_FLOW));
    					}
    				}
    			}

    			// store params
    			for (SDGNode p : ps) {
    				map.put(p, n);
    			}
    		}
    	}

    	cfg.removeAllEdges(remove);
    	for (SDGEdge e : remove) {
    		cfg.removeVertex(e.getTarget());
    	}

    	cfg.addAllEdges(add);

    	return map;
    }

    /** Computes a TransitiveCache using a call graph.
     * @param foldedCallGraph  A folded call graph.
     * @return  A TransitiveCache.
     */
    private HashMap<SDGNode, Collection<SDGNode>> inlining(CFG entryGraph) {
    	HashMap<SDGNode, Collection<SDGNode>> cache = new HashMap<SDGNode, Collection<SDGNode>>();

    	for (SDGNode entry : entryGraph.vertexSet()) {
    		LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
    		HashSet<SDGNode> reachable = new HashSet<SDGNode>();
    		worklist.add(entry);
    		reachable.add(entry);

    		while (!worklist.isEmpty()) {
    			SDGNode next = worklist.poll();
    			for (SDGEdge e : entryGraph.outgoingEdgesOf(next)) {
    				if (reachable.add(e.getTarget())) {
    					worklist.add(e.getTarget());
    				}
    			}
    		}

    		cache.put(entry, reachable);
    	}

        return cache;
    }

    private CFG createReducedICFG(FoldedCFG graph) {
    	CFG clean = new CFG();
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        HashSet<SDGNode> marked = new HashSet<SDGNode>();
        LinkedList<SDGEdge> returnEdges = new LinkedList<SDGEdge>();

        SDGNode root = graph.getRoot();
        marked.add(root);
        worklist.add(root);
        clean.addVertex(root);

        while (!worklist.isEmpty()) {
            SDGNode next = worklist.poll();

            // treat fold nodes
            if (next.getKind() == SDGNode.Kind.FOLDED) {
            	HashSet<SDGNode> folded = iscrMap.get(next);
            	if (folded == null) {
            		folded = new HashSet<SDGNode>();
            		iscrMap.put(next, folded);
            	}
                folded.addAll(graph.getFoldedNodesOf(next));

                // store the transitively called procedures (if this is a folded procedure call)
                for (SDGEdge call : graph.getOutgoingEdgesOfKind(next, SDGEdge.Kind.CALL)) {
                    SDGNode entry = call.getTarget();

                    if (entry.getKind() == SDGNode.Kind.FOLDED) {
                        List<SDGNode> foldedEntries = graph.getFoldedNodesOf(entry);

                        for (SDGNode fe : foldedEntries) {
                            if (fe.getKind() != SDGNode.Kind.ENTRY) continue;

                            Set<SDGNode> toInsert = allNodes(cache.get(fe), graph);

                            // add the transitively reachable nodes
                            if (toInsert == null) continue;

                            folded.addAll(toInsert);
                        }

                    } else {
                        Set<SDGNode> toInsert = allNodes(cache.get(entry), graph);

                        // add the transitively reachable nodes
                        if (toInsert == null) continue;

                        folded.addAll(toInsert);
                    }

                    // remove the call edge so it won't be traversed later
                    graph.removeEdge(call);
                }

            } else {
            	// the node represents itself
            	HashSet<SDGNode> folded = iscrMap.get(next);
            	if (folded == null) {
            		folded = new HashSet<SDGNode>();
            		iscrMap.put(next, folded);
            	}
                folded.add(next);
            }

            // traverse the outgoing edges
            for (SDGEdge edge : graph.outgoingEdgesOf(next)) {
                // no return edges, their traversal can lead to incomplete procedures
                if (edge.getKind() == SDGEdge.Kind.RETURN) {
                	returnEdges.add(edge);
                    continue;
                }

                SDGNode m = edge.getTarget();
                // update worklist and add the node to the CFG
                if (marked.add(m)) {
                    worklist.add(m);
                    clean.addVertex(m);
                }

                // add the edge to the CFG
                clean.addEdge(new SDGEdge(next, m, edge.getKind()));
            }
        }

        // add the missing return edges
        for (SDGEdge e : returnEdges) {
        	if (clean.containsVertex(e.getSource()) && clean.containsVertex(e.getTarget())
        			&& e.getTarget().getKind() != SDGNode.Kind.FOLDED) {

        		 clean.addEdge(new SDGEdge(e.getSource(), e.getTarget(), e.getKind()));
        	}
        }

        // replace control flow with no flow
        LinkedList<SDGEdge> remove = new LinkedList<SDGEdge>();
        LinkedList<SDGEdge> add = new LinkedList<SDGEdge>();
        for (SDGEdge e : clean.edgeSet()) {
        	if (e.getSource().getKind() == SDGNode.Kind.CALL && e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
        		remove.add(e);
        		add.add(new SDGEdge(e.getSource(), e.getTarget(), SDGEdge.Kind.NO_FLOW));
        	}
        }
        clean.removeAllEdges(remove);
        clean.addAllEdges(add);

        return clean;
    }

    /** Returns all nodes that belong to a set of procedures.
     * @param procedures  The start nodes of the procedures.
     * @param g  An SDG or CFG.
     * @return  All nodes that belong to a set of procedures.
     */
    private Set<SDGNode> allNodes(Collection<SDGNode> procedures, CFG g) {
        HashSet<SDGNode> set = new HashSet<SDGNode>();

        for (SDGNode proc : procedures) {
            set.addAll(g.getNodesOfProcedure(proc));
        }

        return set;
    }

    /** Computes a mapping between nodes and ISCR nodes to speed up
     *  the access to the ISCR nodes belonging to a node.
     *
     * @param iscr  The ISCR graph.
     *
     * @return      The computed map.
     */
    private Map<SDGNode, List<SDGNode>> revertISCRMap(CFG original) {
    	HashMap<SDGNode, List<SDGNode>> foldMap = new HashMap<SDGNode, List<SDGNode>>();

    	for (Map.Entry<SDGNode, HashSet<SDGNode>> en : iscrMap.entrySet()) {
    		for (SDGNode n : en.getValue()) {
    			List<SDGNode> l = foldMap.get(n);
    			if (l == null) {
    				l = new LinkedList<SDGNode>();
    				foldMap.put(n, l);
    			}
    			l.add(en.getKey());
    		}
    	}

    	for (SDGNode orig : original.vertexSet()) {
    		// skip parameter nodes
    		if (orig.getKind() == SDGNode.Kind.ACTUAL_IN
    				|| orig.getKind() == SDGNode.Kind.ACTUAL_OUT
    				|| orig.getKind() == SDGNode.Kind.FORMAL_IN
    				|| orig.getKind() == SDGNode.Kind.FORMAL_OUT) continue;

    		List<SDGNode> mappedTo = foldMap.get(orig);

    		if (mappedTo == null) {
    			// node is missing in the map -> orig represents itself in the ISCR graph
    			mappedTo = new LinkedList<SDGNode>();
    			mappedTo.add(orig);
				foldMap.put(orig, mappedTo);
    		}
    	}

    	// the above loop skipped the return nodes
    	for (SDGNode ret : returnNodes) {
    		List<SDGNode> mappedTo = foldMap.get(ret);

    		if (mappedTo == null) {
    			// node is missing in the map -> orig represents itself in the ISCR graph
    			mappedTo = new LinkedList<SDGNode>();
    			mappedTo.add(ret);
				foldMap.put(ret, mappedTo);
    		}
    	}

    	return foldMap;
    }


    /* VERIFY MODE */

    /**
     * Tests if all parameter nodes of the ICFG are inserted into the params map and mapped to the correct nodes.
     */
    private void paramsTest(CFG icfg) {

    	for (SDGNode n : icfg.vertexSet()) {
        	if (n.getKind() == SDGNode.Kind.ACTUAL_IN) {
        		SDGNode call = n;
        		label:while (true) {
        			for (SDGEdge e : icfg.outgoingEdgesOf(call)) {
        				if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
        					call = e.getTarget();
        					if (call.getKind() == SDGNode.Kind.CALL) {
        						break label;
        					}
        				}
        			}
        		}
        		if (params.get(n) != call) {
        			throw new RuntimeException();
        		}

        	} else if (n.getKind() == SDGNode.Kind.ACTUAL_OUT) {
        		SDGNode ret = n;
        		label:while (true) {
        			for (SDGEdge e : icfg.incomingEdgesOf(ret)) {
        				if (e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
        					if (e.getSource().getKind() == SDGNode.Kind.CALL) {
        						break label;
        					} else {
            					ret = e.getSource();
        					}
        				}
        			}
        		}
        		if (n != ret && params.get(n) != ret) {
        			System.out.println(n);
        			System.out.println(ret);
        			System.out.println(params.get(n));
        			throw new RuntimeException();
        		}

        	} else if (n.getKind() == SDGNode.Kind.FORMAL_OUT) {
        		for (SDGNode exit : icfg.vertexSet()) {
        			if (exit.getKind() == SDGNode.Kind.EXIT && exit.getProc() == n.getProc()) {
        				if (!params.containsKey(n)) {
                			System.out.println(n);
                			System.out.println(exit);
                			System.out.println(params.get(n));
                			throw new RuntimeException();
                		}
        				if (params.get(n) != exit) {
                			System.out.println(n);
                			System.out.println(exit);
                			System.out.println(params.get(exit));
                			throw new RuntimeException();
                		}
        				break;
        			}
        		}

        	} else if (n.getKind() == SDGNode.Kind.FORMAL_IN) {
        		SDGNode entry = icfg.getEntry(n);
				if (!params.containsKey(n)) {
        			System.out.println(n);
        			System.out.println(entry);
        			System.out.println(params.get(n));
        			throw new RuntimeException();
        		}
				if (params.get(n) != entry) {
        			System.out.println(n);
        			System.out.println(entry);
        			System.out.println(params.get(entry));
        			throw new RuntimeException();
        		}
        	}
        }
    }

    /**
     * Tests if the two-pass folding preserves the connections between the nodes.
     */
    private void twoPassTest(CFG icfg, FoldedCFG folded) {
    	for (SDGEdge e : icfg.edgeSet()) {
        	SDGNode foldedSource = folded.getFoldNode(e.getSource()) == null ? e.getSource() : folded.getFoldNode(e.getSource());
        	SDGNode foldedTarget = folded.getFoldNode(e.getTarget()) == null ? e.getTarget() : folded.getFoldNode(e.getTarget());

        	if (foldedSource == foldedTarget) {
        		// ok

        	} else {
        		boolean ok = false;
        		for (SDGEdge f : folded.outgoingEdgesOf(foldedSource)) {
        			if (f.getTarget() == foldedTarget) {
        				ok = true;
        				break;
        			}
        		}
    			if (!ok) {
    				System.out.println("****************");
            		System.out.println(e);
            		System.out.println("foldedSource "+foldedSource);
            		System.out.println("foldedTarget "+foldedTarget);
            		throw new RuntimeException();
    			}
        	}
        }

//    	List<CFG> cfgs = splitFoldedGraph(folded);
//    	for (CFG g : cfgs) {
//    		System.out.println(g);
//    	}
    }

    /**
     * Tests if the procedure inlining preserves the connections between the nodes.
     */
    private void testReducedICFG(FoldedCFG folded, CFG reduced) {
    	for (SDGNode n : reduced.vertexSet()) {
    		if (folded.vertexSet().contains(n)) continue;
    		System.out.println("****************");
    		System.out.println(n);
    		throw new RuntimeException();
    	}

    	// flip the foldedMap
    	HashMap<SDGNode, LinkedList<SDGNode>> foldMap = new HashMap<SDGNode, LinkedList<SDGNode>>();
    	for (Map.Entry<SDGNode, HashSet<SDGNode>> en : iscrMap.entrySet()) {
    		for (SDGNode n : en.getValue()) {
    			LinkedList<SDGNode> l = foldMap.get(n);
    			if (l == null) {
    				l = new LinkedList<SDGNode>();
    				foldMap.put(n, l);
    			}
    			l.add(en.getKey());
    		}
    	}

    	// heyho lets go
    	for (SDGEdge e : reduced.edgeSet()) {
    		// regular edge ?
    		SDGEdge.Kind kind = (e.getKind() == SDGEdge.Kind.NO_FLOW ? SDGEdge.Kind.CONTROL_FLOW : e.getKind());
    		boolean ok = false;
        	for (SDGEdge f : folded.getOutgoingEdgesOfKind(e.getSource(), kind)) {
        		if (f.getTarget() == e.getTarget()) {
        			ok = true;
        			break;
        		}
        	}
        	if (!ok) {
				System.out.println("****************");
        		System.out.println(e);
        		throw new RuntimeException();
			}

        	// no call edges from a fold node!
        	if (e.getKind() == SDGEdge.Kind.CALL && e.getSource().getKind() == SDGNode.Kind.FOLDED) {
        		System.out.println("****************");
        		System.out.println(e);
        		throw new RuntimeException();
        	}
        	// no return edges to a fold node!
        	if (e.getKind() == SDGEdge.Kind.RETURN && e.getTarget().getKind() == SDGNode.Kind.FOLDED) {
        		System.out.println("****************");
        		System.out.println(e);
        		throw new RuntimeException();
        	}
        }
    }

    /**
     * Similar to testReducedICFG, but uses the original ICFG.
     */
    private void testReducedICFG2(CFG icfg, CFG reduced) {
    	// flip the foldedMap
    	HashMap<SDGNode, LinkedList<SDGNode>> foldMap = new HashMap<SDGNode, LinkedList<SDGNode>>();
    	for (Map.Entry<SDGNode, HashSet<SDGNode>> en : iscrMap.entrySet()) {
    		for (SDGNode n : en.getValue()) {
    			LinkedList<SDGNode> l = foldMap.get(n);
    			if (l == null) {
    				l = new LinkedList<SDGNode>();
    				foldMap.put(n, l);
    			}
    			l.add(en.getKey());
    		}
    	}

    	// nodes
    	for (SDGNode n : icfg.vertexSet()) {
    		if (reduced.vertexSet().contains(n)) continue;
    		if (foldMap.containsKey(n)) continue;
    		System.out.println("****************");
    		System.out.println(n);
    		throw new RuntimeException();
    	}

    	// heyho lets go
    	for (SDGEdge e : icfg.edgeSet()) {
    		if (e.getKind() == SDGEdge.Kind.RETURN || e.getKind() == SDGEdge.Kind.CALL) continue;

    		if (reduced.containsVertex(e.getSource()) && reduced.containsVertex(e.getTarget())) {
        		boolean ok = false;
    			for (SDGEdge f : reduced.getOutgoingEdgesOfKind(e.getSource(), e.getKind())) {
            		if (f.getTarget() == e.getTarget()) {
            			ok = true;
            			break;
            		}
            	}
            	if (!ok) {
    				System.out.println("****************");
            		System.out.println(e);
            		throw new RuntimeException();
    			}

    		} else if (reduced.containsVertex(e.getSource()))  {
    			LinkedList<SDGNode> targets = foldMap.get(e.getTarget());
    			boolean ok = false;
				label:for (SDGNode target : targets) {
					if (e.getSource() == target) {
						ok = true;
						break;
					}

					for (SDGEdge f : reduced.getOutgoingEdgesOfKind(e.getSource(), e.getKind())) {
						if (f.getTarget() == target) {
							ok = true;
							break label;
						}
					}
				}
            	if (!ok) {
    				System.out.println("****************");
            		System.out.println(e);
            		System.out.println(e.getSource());
            		System.out.println(targets);
            		throw new RuntimeException();
    			}

    		} else if (reduced.containsVertex(e.getTarget())) {
    			LinkedList<SDGNode> sources = foldMap.get(e.getSource());
    			boolean ok = false;
    			label:for (SDGNode source : sources) {
					if (source == e.getTarget()) {
						ok = true;
						break;
					}

					for (SDGEdge f : reduced.getOutgoingEdgesOfKind(source, e.getKind())) {
						if (f.getTarget() == e.getTarget()) {
							ok = true;
							break label;
						}
					}
    			}
            	if (!ok) {
    				System.out.println("****************");
            		System.out.println(e);
            		throw new RuntimeException();
    			}

    		} else {
    			LinkedList<SDGNode> sources = foldMap.get(e.getSource());
    			LinkedList<SDGNode> targets = foldMap.get(e.getTarget());
    			boolean ok = false;
    			label:for (SDGNode source : sources) {
    				for (SDGNode target : targets) {
    					if (source == target) {
    						ok = true;
    						break label;
    					}

    					for (SDGEdge f : reduced.getOutgoingEdgesOfKind(source, e.getKind())) {
    						if (f.getTarget() == target) {
    							ok = true;
    							break label;
    						}
    					}
    				}
    			}
            	if (!ok) {
    				System.out.println("****************");
            		System.out.println(e);
            		throw new RuntimeException();
    			}
    		}
        }
    }

    /**
     * Tests if all keys and values in the node map appear in the reduced graph.
     */
    private void testNodeMap(Map<SDGNode, List<SDGNode>> nodeMap, CFG original, CFG reduced) {
    	// each node in the original CFG has to be either in the node map or in the reduced CFG
    	for (SDGNode orig : original.vertexSet()) {
    		if (orig.getKind() == SDGNode.Kind.ACTUAL_IN
    				|| orig.getKind() == SDGNode.Kind.ACTUAL_OUT
    				|| orig.getKind() == SDGNode.Kind.FORMAL_IN
    				|| orig.getKind() == SDGNode.Kind.FORMAL_OUT) continue;

    		List<SDGNode> mappedTo = nodeMap.get(orig);

    		if (mappedTo == null) {
    			// node is missing
    			// ALARMA
				System.out.println("****************");
        		System.out.println(orig +" is missing ");
        		throw new RuntimeException();
    		}
    	}

    	// all values in the nodeMap have to be nodes in the reduced graph
    	for (Map.Entry<SDGNode, List<SDGNode>> en : nodeMap.entrySet()) {
    		for (SDGNode n : en.getValue()) {
    			if (!reduced.containsVertex(n)) {
        			// node is missing
        			// ALARMA
    				System.out.println("****************");
            		System.out.println(n +" is missing ");
            		System.out.println(en);
            		throw new RuntimeException();
    			}
    		}
    	}
    }

    /**
     * Tests if all keys and values in the node map appear in the ISCR graph.
     */
    private void testISCRGraph(Map<SDGNode, List<SDGNode>> nodeMap, ISCRGraph ig) {
    	// tests the mapping between nodeMap and the ISCR graph

    	// all values in the nodeMap have to be ISCR nodes
    	for (Map.Entry<SDGNode, List<SDGNode>> en : nodeMap.entrySet()) {
    		for (SDGNode n : en.getValue()) {
    			if (!ig.containsNode(n)) {
        			// node is missing
        			// ALARMA
    				System.out.println("****************");
            		System.out.println(n +" is missing ");
            		System.out.println(en);
            		throw new RuntimeException();
    			}
    		}
    	}

    	for (Map.Entry<SDGNode, List<SDGNode>> en : nodeMap.entrySet()) {
    		for (SDGNode n : en.getValue()) {
    			if (ig.containsNode(n) && !en.getValue().contains(n)) {
        			// node is missing
        			// ALARMA
    				System.out.println("****************");
            		System.out.println(n +" is in the ISCR graph, but not in the map ");
            		System.out.println(en);
            		throw new RuntimeException();
    			}
    		}
    	}
    }

    /**
     * Tests if all nodes in the original ICFG are mapped to nodes in the ISCR graph.
     */
    private void testISCRGraphNodes(CFG orig, ISCRGraph ig) {
    	// nodes
    	for (SDGNode n : orig.vertexSet()) {
    		if (ig.getISCRNodes(n) == null) {
				System.out.println("****************");
        		System.out.println(n+" is not in the ISCR graph");
        		throw new RuntimeException();
    		}
    	}

    	List<CFG> cfgs = orig.split();
    	for (CFG g : cfgs) {
    		// all nodes of the same procedure have to have the same number of ISCR nodes
    		HashMap<SDGNode, List<SDGNode>> proc = new HashMap<SDGNode, List<SDGNode>>();

    		for (SDGNode n : g.vertexSet()) {
    			proc.put(n, ig.getISCRNodes(n));
    		}

    		int size = -1;

    		for (Map.Entry<SDGNode, List<SDGNode>> en : proc.entrySet()) {
    			if (size == -1) {
    				size = en.getValue().size();

    			} else if (en.getValue().size() != size) {
    				System.out.println("****************");
            		System.out.println("procedure has not the same number of representatives");
            		for (Map.Entry<SDGNode, List<SDGNode>> en2 : proc.entrySet()) {
            			System.out.println(en2);
            		}
//            		List<CFG> l = splitISCRGraph(ig);
//            		for (CFG h : l) {
//            			System.out.println(h);
//            		}
//            		for (Map.Entry x : iscrMap.entrySet()) {
//            			System.out.println(x);
//            		}
            		throw new RuntimeException();
    			}
    		}
    	}
    }

    private void testISCRGraphEdges(CFG orig, ISCRGraph ig) {
    	// intra-procedural edges
    	for (SDGEdge e : orig.edgeSet()) {
    		if (e.getKind() == SDGEdge.Kind.RETURN || e.getKind() == SDGEdge.Kind.CALL) continue;

			List<SDGNode> sources = ig.getISCRNodes(e.getSource());
			List<SDGNode> targets = ig.getISCRNodes(e.getTarget());

			// auf jeden topf passt ein deckel
			for (SDGNode source : sources) {
				boolean ok = false;
				for (SDGNode target : targets) {
					if (source == target) {
						ok = true;
						break;
					}

					for (SDGEdge f : ig.getOutgoingEdgesOfKind(source, e.getKind())) {
						if (f.getTarget() == target) {
							ok = true;
							break;
						}
					}
				}
	        	if (!ok) {
					System.out.println("****************");
	        		System.out.println(e);
	        		System.out.println(sources);
	        		System.out.println(targets);
	        		throw new RuntimeException();
				}
			}

			for (SDGNode target : targets) {
				boolean ok = false;
				for (SDGNode source : sources) {
					if (source == target) {
						ok = true;
						break;
					}

					for (SDGEdge f : ig.getIncomingEdgesOfKind(target, e.getKind())) {
						if (f.getSource() == source) {
							ok = true;
							break;
						}
					}
				}
	        	if (!ok) {
					System.out.println("****************");
	        		System.out.println(e);
	        		System.out.println(sources);
	        		System.out.println(targets);
	        		throw new RuntimeException();
				}
			}
        }

    	// interprocedural edges
    	LinkedList<SDGEdge> calls = new LinkedList<SDGEdge>();
    	LinkedList<SDGEdge> returns = new LinkedList<SDGEdge>();

    	for (SDGEdge e : ig.edgeSet()) {
    		if (e.getKind() == SDGEdge.Kind.CALL) {
    			calls.add(e);
    		}

    		if (e.getKind() == SDGEdge.Kind.RETURN) {
    			returns.add(e);
    		}
    	}

    	if (calls.size() != returns.size()) {
			System.out.println("****************");
    		System.out.println("more or less returns than calls");
    		System.out.println(calls.size()+" calls");
    		System.out.println(returns.size()+" returns");
    		throw new RuntimeException();
    	}

    	//
    	while (!calls.isEmpty()) {
    		SDGEdge call = calls.poll();
    		SDGEdge ret = null;

    		label:for (SDGEdge e : returns) {
    			if (e.getTarget() == call.getSource() && e.getTarget().getId() < 0) {
    				// folded procedure call
    				ret = e;
    				break label;

    			} else {
    				List<SDGEdge> cf = ig.getOutgoingEdgesOfKind(call.getSource(), SDGEdge.Kind.CONTROL_FLOW);
    				List<SDGEdge> nf = ig.getOutgoingEdgesOfKind(call.getSource(), SDGEdge.Kind.NO_FLOW);

    				for (SDGEdge f : cf) {
    					if (f.getTarget() == e.getTarget()) {
    						ret = e;
    						break label;
    					}
    				}

    				for (SDGEdge f : nf) {
    					if (f.getTarget() == e.getTarget()) {
    						ret = e;
    						break label;
    					}
    				}
    			}
    		}

    		if (ret == null) {
				System.out.println("****************");
        		System.out.println("call edge not matched: ");
        		System.out.println(call);
        		throw new RuntimeException();
			}

    		returns.remove(ret);
    	}
    }

    private List<CFG> splitISCRGraph(ISCRGraph ig) {
    	List<CFG> cfgs = new LinkedList<CFG>();
    	HashSet<SDGNode> entries = new HashSet<SDGNode>();

    	for (SDGEdge e : ig.edgeSet()) {
    		if (e.getKind() == SDGEdge.Kind.CALL) {
    			entries.add(e.getTarget());
    		}
    	}

    	for (SDGNode entry : entries) {
    		LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
    		CFG g = new CFG();
    		g.addVertex(entry);
    		g.setRoot(entry);
    		wl.add(entry);

    		while (!wl.isEmpty()) {
    			SDGNode next = wl.poll();
    			for (SDGEdge e : ig.outgoingEdgesOf(next)) {
    				if (e.getKind().isIntraproceduralEdge()) {
    					if (g.addVertex(e.getTarget())) {
    						wl.add(e.getTarget());
    					}
    					g.addEdge(e);
    				}
    			}
    		}

    		cfgs.add(g);
    	}

    	return cfgs;
    }

    private List<CFG> splitFoldedGraph(CFG ig) {
    	List<CFG> cfgs = new LinkedList<CFG>();
    	HashSet<SDGNode> entries = new HashSet<SDGNode>();

    	for (SDGEdge e : ig.edgeSet()) {
    		if (e.getKind() == SDGEdge.Kind.CALL) {
    			entries.add(e.getTarget());
    		}
    	}

    	for (SDGNode entry : entries) {
    		LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
    		CFG g = new CFG();
    		g.addVertex(entry);
    		g.setRoot(entry);
    		wl.add(entry);

    		while (!wl.isEmpty()) {
    			SDGNode next = wl.poll();
    			for (SDGEdge e : ig.outgoingEdgesOf(next)) {
    				if (e.getKind().isIntraproceduralEdge()) {
    					if (g.addVertex(e.getTarget())) {
    						wl.add(e.getTarget());
    					}
    					g.addEdge(e);
    				}
    			}
    		}

    		cfgs.add(g);
    	}

    	return cfgs;
    }


    public static void main(String[] args) throws Exception {
//    	String file = PDGs.pdgs[2];
//        SDG g = SDG.readFrom(file);
//        CFG icfg = ICFGBuilder.extractICFG(g);
//        ISCRBuilder b = new ISCRBuilder();
//        b.buildISCRGraphs(icfg);

        for (String file : PDGs.pdgs) {
        	System.out.println(file);
	        SDG g = SDG.readFrom(file);
	        CFG icfg = ICFGBuilder.extractICFG(g);
	        ISCRBuilder b = new ISCRBuilder();
	        b.buildISCRGraphs(icfg);
        }
	}
}
