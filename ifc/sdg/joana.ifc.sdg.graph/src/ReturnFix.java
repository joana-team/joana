/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;



public class ReturnFix {
	public static void main(String[] args) throws IOException {
        SDG sdg = SDG.readFrom("/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/old/j2me.Barcode.pdg");

        LinkedList<SDGEdge> es = new LinkedList<SDGEdge>();
        for (SDGEdge e : sdg.edgeSet()) {
        	if (e.getKind() == SDGEdge.Kind.RETURN) {
        		es.add(e);
//            	System.out.println(e.getSource()+", "+e.getSource().getKind()+" -> "+e.getTarget()+", "+e.getTarget().getKind());
        	}
        }
        sdg.removeAllEdges(es);
        System.out.println("***************");

        // search for call vertices
        for (SDGNode n : sdg.vertexSet()) {
            if (n.getKind() == SDGNode.Kind.CALL) {
                SDGNode exit = null;

                // compute exit vertex of called procedure
                for (SDGEdge e : sdg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CALL)) {
                    for (SDGEdge x : sdg.getOutgoingEdgesOfKind(e.getTarget(), SDGEdge.Kind.CONTROL_FLOW)){
                        if (x.getTarget().getKind() == SDGNode.Kind.EXIT) {
                            exit = x.getTarget();

                            // compute return nodes and add return edges (with 'exit' as source)
                            for (SDGEdge edge : sdg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CONTROL_FLOW)) {
                                if (edge.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
                                    SDGEdge ret =  new SDGEdge(exit, edge.getTarget(), SDGEdge.Kind.RETURN);
                                    sdg.addEdge(ret);
                                }
                            }
                        }
                    }
                }
            }
        }

        System.out.println("***************");
        es = new LinkedList<SDGEdge>();
        for (SDGEdge e : sdg.edgeSet()) {
        	if (e.getSource() == e.getTarget()
        			&& e.getKind() != SDGEdge.Kind.INTERFERENCE_WRITE
        			&& e.getKind() != SDGEdge.Kind.INTERFERENCE_WRITE) {
        		es.add(e);
        	}
        }
        sdg.removeAllEdges(es);

        String content = SDGSerializer.toPDGFormat(sdg);

        FileWriter w = new FileWriter("/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/old/j2me.Barcode.pdg_new");
        w.write(content);
        w.flush();
        w.close();
    }
}
