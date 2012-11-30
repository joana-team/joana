/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.pathslicing;

import java.io.IOException;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;


public class TestDominator {
	public static void main(String[] args) throws IOException {
		SDG g = SDG.readFrom("D:\\Martin\\JOB\\runtime-EclipseApplication\\Tests\\jSDG\\conc.sq.SharedQueue.pdg");

		testDominator(g);
	}

	private static void testDominator(SDG g) {
		CFG icfg = ICFGBuilder.extractICFG(g); // interprozeduraler CFG
		List<CFG> cfgs = icfg.split(); // teilt den ICFG in die CFGs der einzelnen Methoden auf

		for (CFG cfg : cfgs) {
			CFG revPO = Dominator.dominator(cfg);
			System.out.println(revPO);
//			revPO = Dominator.postDominator(cfg);
			System.out.println(revPO);
		}
	}
}
