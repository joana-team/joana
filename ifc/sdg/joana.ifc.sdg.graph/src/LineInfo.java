/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import java.io.IOException;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.PDGs.PDGIterator;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import gnu.trove.map.hash.TIntObjectHashMap;



public class LineInfo {
	private static SDG sdg;
	private static TIntObjectHashMap<LinkedList<SDGNode>> procs;

	public static void main(String[] args)
	throws IOException {
		PDGIterator iter = PDGs.getPDGs();
		while (iter.hasNext()) {
			sdg = iter.next();
			procs = new TIntObjectHashMap<LinkedList<SDGNode>>();
			System.out.println("*******************\n"+sdg.getName());
			lineInfo();
		}
//		sdg = PDGs.get(PDGs.LAPLACE_GRID);
//		procs = new TIntObjectHashMap<LinkedList<SDGNode>>();
//
//		lineInfo();
	}

	private static void lineInfo() {
		sort();
		filter();
	}

	private static void sort() {
		for (SDGNode n : sdg.vertexSet()) {
			LinkedList<SDGNode> l = procs.get(n.getProc());

			if (l == null) {
				l = new LinkedList<SDGNode>();
				procs.put(n.getProc(), l);
			}

			l.add(n);
		}
	}

	private static void filter() {
		for (LinkedList<SDGNode> l : procs.valueCollection()) {
			for (int i = 0; i < l.size(); i++) {
				SDGNode n = l.get(i);

				if (n.getSr() == 0) {
					System.out.println(n+" "+n.getKind()+" "+n.getLabel());
				}
			}
		}
	}
}
