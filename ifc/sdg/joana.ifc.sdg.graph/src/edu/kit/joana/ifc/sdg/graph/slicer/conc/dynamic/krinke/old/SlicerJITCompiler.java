/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.old;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;


public class SlicerJITCompiler extends Slicer implements edu.kit.joana.ifc.sdg.graph.slicer.Slicer {
    /**
     * Creates a new instance of this slicer.
     *
     * @param graph A threaded interprocedural program dependencies graph that shall be sliced.
     *              It has to contain control flow edges.
     */
    public SlicerJITCompiler(SDG graph) {
        super(graph);
    }

    @Override
    protected boolean reaches(Context start, Context target) {
    	DynamicContext mappedTarget = map(target);
    	DynamicContext mappedStart = map(start);
    	return reachable.reaches(mappedStart, mappedTarget) || !reachesInSDG(target, start);
    }

    private boolean reachesInSDG(Context from, Context to) {
    	// guided traversal
    	LinkedList<Context> worklist_1 = new LinkedList<Context>();
    	LinkedList<Context> worklist_2 = new LinkedList<Context>();
    	HashSet<Context> visited = new HashSet<Context>();
    	worklist_1.add(to);
    	visited.add(to);

    	// 1. compute a chop (from -> to), to which we will restrict the traversal
    	Collection<SDGNode> chop = truncated.chop(from.getNode(), to.getNode());

    	if (chop.isEmpty()) return false; // shortcut

    	// 2. ascend procedures until reaching suffixes of context `from'
    	while(!worklist_1.isEmpty()){
            // next element, put it in the slice
    		Context next = worklist_1.poll();

            if (next.equals(from)) {System.out.println("TRUE");return true;}

            // handle all incoming edges of 'next'
            for(SDGEdge e : sdg.incomingEdgesOf(next.getNode())){
                SDGNode pre = e.getTarget();

                // restrict the traversal to the chop
                if (!chop.contains(pre)) continue;

                // traverse intra-procedurally and towards calling procedures
                if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.PARAMETER_IN) {
                	if (pre.getKind() == SDGNode.Kind.FORMAL_OUT) {
                		// handle class initializer
                		Collection<Context> newContexts = conMan.getContextsOf(pre, 0);

                        // compute new thread states
                        for (Context con : newContexts) {
                        	if (con.isCallStringPrefixOf(from)) {
                        		// add to the second worklist
                        		if (visited.add(con)) {
                                	worklist_2.add(con);
                                }

                        	} else {
                        		if (visited.add(con)) {
                                	worklist_1.add(con);
                                }
                        	}
                        }

                    } else if (pre.isInThread(next.getThread())) {
                    	// go to the calling procedure
                    	SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    	Context[] newContexts = conMan.ascend(pre, callSite, next);

                        for (Context con : newContexts) {
                            if (con != null) {
                            	if (con.isCallStringPrefixOf(from)) {
                            		// add to the second worklist
                            		if (visited.add(con)) {
                                    	worklist_2.add(con);
                                    }

                            	} else {
                            		if (visited.add(con)) {
                                    	worklist_1.add(con);
                                    }
                            	}
                            }
                        }
                    }

                } else if (e.getKind().isSDGEdge() && e.getKind().isIntraproceduralEdge()){
                    // intra-procedural traversal
                	Context newContext = conMan.level(pre, next);
                    if (visited.add(newContext)) {
                    	worklist_1.add(newContext);
                    }
                }
            }
        }

    	// 3. descend from there towards target context
    	while(!worklist_2.isEmpty()){
            Context next = worklist_2.poll();

            if (next.equals(from)) {System.out.println("TRUE");return true;}

            // handle all incoming edges of 'next'
            for(SDGEdge e : sdg.incomingEdgesOf(next.getNode())){
                SDGNode pre = e.getSource();

                // restrict the traversal to the chop
                if (!chop.contains(pre)) continue;

                // traverse intra-procedurally and towards called procedures
                if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT ) {
                    // go to the called procedure
                    SDGNodeTuple callSite = sdg.getCallEntryFor(e);
                    Context newContext = conMan.descend(pre, callSite, next);

                    // only traverse towards 1from'
                    if (newContext.isCallStringPrefixOf(from) && (visited.add(newContext))) {
                    	worklist_2.add(newContext);
                	}

                } else if (e.getKind().isSDGEdge() && e.getKind().isIntraproceduralEdge()){
                    // intraprocedural traversal
                    Context newContext = conMan.level(pre, next);
                    if (visited.add(newContext)) {
                    	worklist_1.add(newContext);
                    }
                }
            }
        }

    	// 4. we haven't reached `from'
    	return false;
    }
}
