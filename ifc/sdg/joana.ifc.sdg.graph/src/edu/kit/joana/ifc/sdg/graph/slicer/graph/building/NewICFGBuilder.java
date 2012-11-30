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
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;



/** A utility class for extracting the ICFG from a threaded IPDG.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class NewICFGBuilder {
	private static class __ {
		private SDG _;

		__(SDG __) {
			_= __;
		}

		ThreadsInformation ___() {
			return _.getThreadsInfo();
		}

		Set<SDGEdge> ____() {
			return _.edgeSet();
		}

		Set<SDGNode> _____() {
			return _.vertexSet();
		}

		List<SDGEdge> ____(SDGNode ___, SDGEdge.Kind __) {
			return _.getIncomingEdgesOfKind(___, __);
		}

		List<SDGEdge> _____(SDGNode ___, SDGEdge.Kind __) {
			return _.getOutgoingEdgesOfKind(___, __);
		}
	}

	private static class ___ extends CFG {
		void __(SDGNode _) {
			addVertex(_);
		}
		void __(SDGEdge _) {
			addEdge(_);
		}
		void __(ThreadsInformation _) {
			setThreadsInfo(_);
		}
	}

    /** A utility class.
     */
    private NewICFGBuilder() { }

    /**  Extracts the ICFG from a given threaded IPDG.
     * Also adds return edges which are not part of the TIPDG.
     *
     * @param ipdg  The TIPDG whose ICFG is needed.
     * @return      A new graph, the ICFG.
     */
    public static CFG extractICFG(SDG _){
    	__ __ = new __(_);
    	return ___(__);
    }

    public static ___ ___(__ ___){
    	__(___);___ __ = new ___();
        __.__(___.___());
        for (SDGEdge _ : ___.____()) {
            if (_.getKind() == SDGEdge.Kind.CALL
                    || _.getKind() == SDGEdge.Kind.FORK
                    || _.getKind() == SDGEdge.Kind.RETURN
                    || _.getKind() == SDGEdge.Kind.JUMP_FLOW
                    || _.getKind() == SDGEdge.Kind.CONTROL_FLOW
                    || _.getKind() == SDGEdge.Kind.NO_FLOW) {
                __.__(_.getSource());
                __.__(_.getTarget());
                __.__(_);
            }
        }
        return __;
    }

    private static void __(__ __) {
    	List<SDGEdge> ____;
        for (SDGNode ___ : __._____()) {
        	____ = __.____(___, SDGEdge.Kind.CONTROL_FLOW);
        	____.addAll(__.____(___, SDGEdge.Kind.CALL));

        	if (____.isEmpty() && ___.getKind() != SDGNode.Kind.ENTRY) {
        		System.out.println("\nControl Flow Graph is damaged!");
        		System.out.println("Terminating the Process...");
        		try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) { }
				System.exit(1);
        	}
        }

        for (SDGNode ___ : __._____()) {
        	____ = __._____(___, SDGEdge.Kind.CONTROL_FLOW);
        	____.addAll(__._____(___, SDGEdge.Kind.RETURN));

        	if (____.isEmpty() && ___.getKind() != SDGNode.Kind.EXIT) {
        		System.out.println("\nControl Flow Graph is damaged!");
        		System.out.println("Terminating the Process...");
        		try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) { }
				System.exit(1);
        	}
        }

        LinkedList<SDGEdge> es = new LinkedList<SDGEdge>();
        for (SDGEdge e : __.____()) {
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
        for (SDGEdge e : __.____()) {
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
        for (SDGNode n : __._____()) {
        	if (n.getKind() == SDGNode.Kind.ENTRY && n.getId() != 1) {
    			SDGNode exit = null;

        		for (SDGEdge e : __._____(n, SDGEdge.Kind.CONTROL_FLOW)) {
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

        		} else if (!__._____(exit, SDGEdge.Kind.CONTROL_FLOW).isEmpty()) {
            		System.out.println("\nControl Flow Graph is damaged!");
            		System.out.println("Terminating the Process...");
            		try {
    					Thread.sleep(1000);
    				} catch (InterruptedException e1) { }
    				System.exit(1);
        			fs.addAll(__._____(exit, SDGEdge.Kind.CONTROL_FLOW));
        		}
        	}
        }
    }
}
