/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;



public class DataSliceTest {

	public static void main(String[] args) {
		SDG sdg;
		try {
			sdg = SDG.readFrom("/afs/info.uni-karlsruhe.de/user/mmohr/git/mojo/joana.ifcconsole/edu.kit.pp.joana.ifcconsole.toy.conc.InterproceduralThreadTest.main.pdg");
		} catch (IOException e) {
			System.out.println("I/O error while reading sdg from file.");
			e.printStackTrace();
			return;
		}

		Set<SDGEdge.Kind> nonDataEdges = new HashSet<SDGEdge.Kind>();
		for (SDGEdge.Kind kind : SDGEdge.Kind.values()) {
			nonDataEdges.add(kind);
		}

		nonDataEdges.remove(SDGEdge.Kind.DATA_DEP);
		nonDataEdges.remove(SDGEdge.Kind.DATA_HEAP);
		nonDataEdges.remove(SDGEdge.Kind.DATA_ALIAS);
		nonDataEdges.remove(SDGEdge.Kind.PARAMETER_IN);
		nonDataEdges.remove(SDGEdge.Kind.PARAMETER_OUT);
		nonDataEdges.remove(SDGEdge.Kind.SUMMARY_DATA);
		nonDataEdges.remove(SDGEdge.Kind.SUMMARY_NO_ALIAS);
		nonDataEdges.remove(SDGEdge.Kind.SUMMARY);

		SummarySlicerBackward s = new SummarySlicerBackward(sdg, nonDataEdges);

		SDGNode thiS = sdg.getNode(41);

		System.out.println(s.slice(thiS));
	}
}
