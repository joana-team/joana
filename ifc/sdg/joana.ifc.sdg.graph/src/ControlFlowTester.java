/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;



public class ControlFlowTester {
	public static void main(String[] args) throws IOException {
//		int i = 8;
        SDG sdg = SDG.readFrom("/ben/giffhorn/Desktop/Test.pdg");
//        GraphModifier.inlineParameterVertices(sdg);

//        System.out.println(PDGs.pdgs[i]);
        System.out.println("***** INC EDGES *****");
        for (SDGNode n : sdg.vertexSet()) {
        	List<SDGEdge> l = sdg.getIncomingEdgesOfKind(n, SDGEdge.Kind.CONTROL_FLOW);
        	l.addAll(sdg.getIncomingEdgesOfKind(n, SDGEdge.Kind.CALL));

        	if (l.isEmpty()) {// && n.getKind() != SDGNode.Kind.ENTRY) {
        		System.out.println(n+" "+n.getKind());
        	}
        }
        System.out.println("***** OUT EDGES *****");
        for (SDGNode n : sdg.vertexSet()) {
        	List<SDGEdge> l = sdg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CONTROL_FLOW);
        	l.addAll(sdg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.RETURN));

        	if (l.isEmpty()) {// && n.getKind() != SDGNode.Kind.EXIT) {
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
        	if (n.getKind() == SDGNode.Kind.ENTRY) {
    			SDGNode exit = null;

        		for (SDGEdge e : sdg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CONTROL_FLOW)) {
	        		if (e.getTarget().getKind() == SDGNode.Kind.EXIT) {
	        			exit = e.getTarget();
	        			break;
	        		}
        		}
        		
        		if (exit != sdg.getExit(n)) {
        			System.out.println("Exit node is not an unique CONTROL_FLOW successof of entry ");
        		}

        		if (exit == null) {
        			System.out.println(n+" has no exit");

        		} else if (!sdg.getOutgoingEdgesOfKind(exit, SDGEdge.Kind.CONTROL_FLOW).isEmpty()) {
        			System.out.println(exit+" is not an exit");
        			fs.addAll(sdg.getOutgoingEdgesOfKind(exit, SDGEdge.Kind.CONTROL_FLOW));
        		}
        	}
        }


//        sdg.removeAllEdges(es);
//        sdg.removeAllEdges(fs);
//        String content = SDGSerializer.toPDGFormat(sdg);
//
//        FileWriter w = new FileWriter(pdgs[i]+"_new");
//        w.write(content);
//        w.flush();
//        w.close();
    }
}
