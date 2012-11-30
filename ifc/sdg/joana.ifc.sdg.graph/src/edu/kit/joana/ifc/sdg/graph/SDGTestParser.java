/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;
/*
 * Created on Feb 25, 2004
 */
import java.io.IOException;
import java.util.Iterator;

/**
 * @author hammer
 *
 */
class SDGTestParser {
	public static void main(String[] args) throws IOException {
		SDG sdg = SDG.readFrom("/home/st/hammer/Projects/workspace/sdglib/tests.RetThis.pdg");
		//System.out.println(sdg);
		for (Iterator<SDGNode> i = sdg.vertexSet().iterator(); i.hasNext(); ) {
			SDGNode node = i.next();
			for (Iterator<SDGEdge> it = sdg.outgoingEdgesOf(node).iterator();
					it.hasNext() ;) {
				System.out.print(node.getId() +" -> ");
				SDGNode to = it.next().getTarget();
				System.out.println(to.getId());
			}
		}
/*		Frame w = new Frame();
		w.add(new JGraph(new JGraphModelAdapter(sdg)));
		w.pack();
		w.show();
*/	}
}
