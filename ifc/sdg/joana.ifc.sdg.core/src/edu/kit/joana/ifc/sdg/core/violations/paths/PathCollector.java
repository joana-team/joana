/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.violations.paths;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;


/** Prototypische neue Pfadberechnung, angelehnt an die Implicit Flow Metrik:
 * Berechnet alle kontext-sensitiven Pfade, deren Anzahl von Kontrollabhaengigkeiten
 * dem minimalen Wert der Metrik entsprechen.
 * Scheint recht gut zu funktionieren -> weiter evaluieren.
 *
 * @author  Dennis Giffhorn
 */
public class PathCollector {
	public static final boolean DEBUG = true;

	private SDG sdg;
	private DynamicContextManager man;
	private Set<SDGEdge.Kind> threadEdges = SDGEdge.Kind.threadEdges();

    /**
     * Instantiates a new PathCollector.
     *
     * @param The SDG to slice.
     * @param The call graph of the program.
     */
    public PathCollector(SDG g) {
        sdg = g;
        man = new DynamicContextManager(sdg);
    }

    public Collection<Path> collect(SDGNode from, SDGNode to, Collection<SDGNode> subGraph, int steps) {
    	Context criterion = new DynamicContext(to);
    	criteria.add(new Path(criterion));

    	Collection<Path> result = collectIteratively(from, subGraph, steps);

    	while (result.isEmpty()) {
    		steps++;
    		result = collectIteratively(from, subGraph, steps);
    	}

    	return result;
    }

    LinkedList<Path> criteria= new LinkedList<Path>();

    /**
     *
     */
    private Collection<Path> collectIteratively(SDGNode from, Collection<SDGNode> subGraph, int steps) {
    	LinkedList<Path> paths = new LinkedList<Path>();
    	LinkedList<Path> worklist = new LinkedList<Path>();

        // init worklist
        worklist.addAll(criteria);
        criteria.clear();

        while(!worklist.isEmpty()){
            // next element, put it in the slice
            Path path = worklist.poll();
            if (DEBUG) System.out.println(path+" "+path.getStep()+" <= "+steps);

            if (path.getStep() > (steps)) {
            	criteria.add(path);
            	continue;
            }

            if (path.getCurrentNode() == from) {
            	// found a path
            	paths.add(path);
            	continue;
            }

            // handle all incoming edges of 'next'
            // only slice along the nodes given in 'border'
            for(SDGEdge e : sdg.incomingEdgesOf(path.getCurrentNode())){
            	if (!e.getKind().isSDGEdge()) continue;

                SDGNode pre = e.getSource();

                if (!subGraph.contains(pre)) continue;

                // distinguish between call sites, summary edges and return edges,
                // param-out edges, interference edges, or intra-procedural data dependence edges
                if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN) {

                    // The class initializer method is a special case due to the structure of the given SDG graphs.
                    // It can be recognised by having the only formal-out vertex with an outgoing param-in edge
                    // which is also the only 'entry point' during an intra-thread backward slice.
                    if (pre.getKind() == SDGNode.Kind.FORMAL_OUT) {
                        Collection<Context> cons = man.getAllContextsOf(pre);

                        // compute new thread states
                        for (Context con : cons) {
                        	if (!path.contains(con)) {
                            	Path extendedPath = path.prepend(con);
                            	worklist.add(extendedPath);
                            }
                        }

                    } else {
                        boolean insideThread = false;
                        for (int st : pre.getThreadNumbers()) {
                            if (st == path.getThread()) insideThread = true;
                        }

                        if (insideThread) {
                            // a common call or parameter-in edge
                            // go to the calling procedure
                        	SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                            Context[] cons = man.ascend(pre, callSite, path.getCurrent());

                            for (Context con : cons) {
                            	if (con != null && !path.contains(con)) {
                                	Path extendedPath = path.prepend(con);

                                	if (e.getKind() == SDGEdge.Kind.CALL) {
                                		extendedPath.incStep();
                                	}

                                	worklist.add(extendedPath);
                                }
                            }
                        }
                    }

                } else if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                    // go to the called procedure
                	SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    Context con = man.descend(pre, callSite, path.getCurrent());

                    if (!path.contains(con)) {
                    	Path extendedPath = path.prepend(con);
                    	worklist.add(extendedPath);
                    }

                } else if (threadEdges.contains(e.getKind())) {
                	Context con = new DynamicContext(pre);

                	if (!path.contains(con)) {
                    	Path extendedPath = path.prepend(con);

                    	if (e.getKind() == SDGEdge.Kind.FORK) {
                    		extendedPath.incStep();
                    	}

                    	worklist.add(extendedPath);
                    }

                } else if (e.getKind() == SDGEdge.Kind.SUMMARY || e.getKind() == SDGEdge.Kind.SUMMARY_DATA
                		|| e.getKind() == SDGEdge.Kind.SUMMARY_NO_ALIAS) {
                	// skip

                } else {
                    // intraprocedural traversion
                    Context con = man.level(pre, path.getCurrent());

                    if (!path.contains(con)) {
                    	Path extendedPath = path.prepend(con);

//                    	if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_UNCOND
//                    			|| e.getKind() == SDGEdge.Kind.CONTROL_DEP_COND
//                    			|| e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR
//                    			|| e.getKind() == SDGEdge.Kind.CONTROL_DEP_CALL
//                    			|| e.getKind() == SDGEdge.Kind.JUMP_DEP) {
                    	if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_COND
                    			|| e.getKind() == SDGEdge.Kind.JUMP_DEP) {

                    		extendedPath.incStep();
                    	}

                    	worklist.add(extendedPath);
                    }
                }
            }
        }

        // return all found vertices with outgoing interference edges
        return paths;
    }
}

