/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.building;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;


/** A utility class for extracting the ICFG from a threaded IPDG.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class ICFGBuilder {

    /** A utility class.
     */
    private ICFGBuilder() { }

    /**  Extracts the ICFG from a given threaded IPDG.
     * Also adds return edges which are not part of the TIPDG.
     *
     * @param ipdg  The TIPDG whose ICFG is needed.
     * @return      A new graph, the ICFG.
     */
    public static CFG extractICFG(SDG sdg){
//    	debuggingControlFlowInspector(sdg);

        // create new graph and copy all ICFG-related edges and associated
        // vertices into it
        CFG icfg = new CFG();
        icfg.setThreadsInfo(sdg.getThreadsInfo());
        Set<SDGEdge> edges = sdg.edgeSet();

        for (SDGEdge e : edges) {
            if (e.getKind() == SDGEdge.Kind.CALL
                    || e.getKind() == SDGEdge.Kind.FORK
                    || e.getKind() == SDGEdge.Kind.RETURN
                    || e.getKind() == SDGEdge.Kind.JUMP_FLOW
                    || e.getKind() == SDGEdge.Kind.CONTROL_FLOW
                    || e.getKind() == SDGEdge.Kind.NO_FLOW) {

                icfg.addVertex(e.getSource());
                icfg.addVertex(e.getTarget());
                icfg.addEdge(e);
            }
        }

        return icfg;
    }
    
    /**  
     * Variant of {@link #extractICFG(SDG} in which also join edges are included.
     *
     * @param ipdg  The TIPDG whose ICFG is needed.
     * @return      A new graph, the ICFG (including join edges).
     */
    public static CFG extractICFGIncludingJoins(SDG sdg){
//    	debuggingControlFlowInspector(sdg);

        // create new graph and copy all ICFG-related edges and associated
        // vertices into it
        CFG icfg = new CFG();
        icfg.setThreadsInfo(sdg.getThreadsInfo());
        Set<SDGEdge> edges = sdg.edgeSet();

        for (SDGEdge e : edges) {
            if (e.getKind() == SDGEdge.Kind.CALL
                    || e.getKind() == SDGEdge.Kind.FORK
                    || e.getKind() == SDGEdge.Kind.RETURN
                    || e.getKind() == SDGEdge.Kind.JUMP_FLOW
                    || e.getKind() == SDGEdge.Kind.CONTROL_FLOW
                    || e.getKind() == SDGEdge.Kind.NO_FLOW
                    || e.getKind() == SDGEdge.Kind.JOIN) {

                icfg.addVertex(e.getSource());
                icfg.addVertex(e.getTarget());
                icfg.addEdge(e);
            }
        }

        return icfg;
    }

    @SuppressWarnings("unused")
	private static void debuggingControlFlowInspector(SDG sdg) {
        System.out.println("***** INC EDGES *****");
        for (SDGNode n : sdg.vertexSet()) {
        	List<SDGEdge> l = sdg.getIncomingEdgesOfKind(n, SDGEdge.Kind.CONTROL_FLOW);
        	l.addAll(sdg.getIncomingEdgesOfKind(n, SDGEdge.Kind.CALL));

        	if (l.isEmpty() && n.getKind() != SDGNode.Kind.ENTRY) {
        		System.out.println(n+" "+n.getKind());
        	}
        }
    	System.out.println("***** OUT EDGES *****");
        for (SDGNode n : sdg.vertexSet()) {
        	List<SDGEdge> l = sdg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CONTROL_FLOW);
        	l.addAll(sdg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.RETURN));

        	if (l.isEmpty() && n.getKind() != SDGNode.Kind.EXIT) {
        		System.out.println(n+" "+n.getKind());
        	}
        }

        System.out.println("***** LOOPS *****");
        LinkedList<SDGEdge> es = new LinkedList<SDGEdge>();
        for (SDGEdge e : sdg.edgeSet()) {
        	if (e.getSource() == e.getTarget()
        			&& e.getKind() != SDGEdge.Kind.INTERFERENCE_WRITE
        			&& e.getKind() != SDGEdge.Kind.INTERFERENCE_WRITE) {
        		System.out.println("loop: "+e);
        		es.add(e);
        	}
        }

        System.out.println("***** UNIQUE RETURN *****");
        HashMap<Integer, SDGEdge> ret = new HashMap<Integer, SDGEdge>();
        for (SDGEdge e : sdg.edgeSet()) {
        	if (e.getKind() == SDGEdge.Kind.RETURN) {
        		if (ret.get(e.getSource().getProc()) != null
        				&& ret.get(e.getSource().getProc()).getSource() != e.getSource()) {
        			System.out.println(e+"  "+ret.get(e.getSource().getProc()));
        		} else {
        			ret.put(e.getSource().getProc(), e);
        		}
        	}
        }

        System.out.println("***** UNIQUE EXIT *****");
        LinkedList<SDGEdge> fs = new LinkedList<SDGEdge>();
        for (SDGNode n : sdg.vertexSet()) {
        	if (n.getKind() == SDGNode.Kind.ENTRY && n.getId() != 1) {
    			SDGNode exit = null;

        		for (SDGEdge e : sdg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CONTROL_FLOW)) {
	        		if (e.getTarget().getKind() == SDGNode.Kind.EXIT) {
	        			exit = e.getTarget();
	        			break;
	        		}
        		}

        		if (exit == null) {
        			System.out.println(n+" has no exit");

        		} else if (!sdg.getOutgoingEdgesOfKind(exit, SDGEdge.Kind.CONTROL_FLOW).isEmpty()) {
        			System.out.println(exit+" is not an exit");
        			fs.addAll(sdg.getOutgoingEdgesOfKind(exit, SDGEdge.Kind.CONTROL_FLOW));
        		}
        	}
        }
    }

    @SuppressWarnings("unused")
	private static void draconicControlFlowInspector(SDG sdg) {
        for (SDGNode n : sdg.vertexSet()) {
        	List<SDGEdge> l = sdg.getIncomingEdgesOfKind(n, SDGEdge.Kind.CONTROL_FLOW);
        	l.addAll(sdg.getIncomingEdgesOfKind(n, SDGEdge.Kind.CALL));

        	if (l.isEmpty() && n.getKind() != SDGNode.Kind.ENTRY) {
        		System.out.println("\nControl Flow Graph is damaged!");
        		System.out.println("Terminating the Process...");
        		try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) { }
				System.exit(1);
        	}
        }

        for (SDGNode n : sdg.vertexSet()) {
        	List<SDGEdge> l = sdg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CONTROL_FLOW);
        	l.addAll(sdg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.RETURN));

        	if (l.isEmpty() && n.getKind() != SDGNode.Kind.EXIT) {
        		System.out.println("\nControl Flow Graph is damaged!");
        		System.out.println("Terminating the Process...");
        		try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) { }
				System.exit(1);
        	}
        }

        for (SDGEdge e : sdg.edgeSet()) {
        	if (e.getSource() == e.getTarget()
        			&& e.getKind() != SDGEdge.Kind.INTERFERENCE_WRITE
        			&& e.getKind() != SDGEdge.Kind.INTERFERENCE_WRITE) {
        		System.out.println("\nCircular Dependences!");
        		System.out.println("Terminating the Process...");
        		try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) { }
				System.exit(1);
        	}
        }

        HashMap<Integer, SDGEdge> ret = new HashMap<Integer, SDGEdge>();
        for (SDGEdge e : sdg.edgeSet()) {
        	if (e.getKind() == SDGEdge.Kind.RETURN) {
        		if (ret.get(e.getSource().getProc()) != null
        				&& ret.get(e.getSource().getProc()).getSource() != e.getSource()) {
            		System.out.println("\nControl Flow Graph is damaged!");
            		System.out.println("Terminating the Process...");
            		try {
    					Thread.sleep(1000);
    				} catch (InterruptedException e1) { }
    				System.exit(1);
        		} else {
        			ret.put(e.getSource().getProc(), e);
        		}
        	}
        }

        LinkedList<SDGEdge> fs = new LinkedList<SDGEdge>();
        for (SDGNode n : sdg.vertexSet()) {
        	if (n.getKind() == SDGNode.Kind.ENTRY && n.getId() != 1) {
    			SDGNode exit = null;

        		for (SDGEdge e : sdg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CONTROL_FLOW)) {
	        		if (e.getTarget().getKind() == SDGNode.Kind.EXIT) {
	        			exit = e.getTarget();
	        			break;
	        		}
        		}

        		if (exit == null) {
            		System.out.println("\nControl Flow Graph is damaged!");
            		System.out.println("Terminating the Process...");
            		try {
    					Thread.sleep(1000);
    				} catch (InterruptedException e1) { }
    				System.exit(1);

        		} else if (!sdg.getOutgoingEdgesOfKind(exit, SDGEdge.Kind.CONTROL_FLOW).isEmpty()) {
            		System.out.println("\nControl Flow Graph is damaged!");
            		System.out.println("Terminating the Process...");
            		try {
    					Thread.sleep(1000);
    				} catch (InterruptedException e1) { }
    				System.exit(1);
        			fs.addAll(sdg.getOutgoingEdgesOfKind(exit, SDGEdge.Kind.CONTROL_FLOW));
        		}
        	}
        }
    }
}
