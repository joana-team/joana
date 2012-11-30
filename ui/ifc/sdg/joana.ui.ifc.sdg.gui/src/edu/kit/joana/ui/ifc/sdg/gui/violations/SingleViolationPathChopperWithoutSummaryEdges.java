/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.violations;
//package edu.kit.joana.ifc.sdg.gui.violations;
//
//import java.util.Collection;
//import java.util.List;
//
//import edu.kit.joana.ifc.sdg.core.violations.Violation;
//import edu.kit.joana.ifc.sdg.core.violations.paths.IncrementalViolationPathGen;
//import edu.kit.joana.ifc.sdg.gui.NJSecPlugin;
//import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
//import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
//import edu.kit.joana.ifc.sdg.graph.SDG;
//
//import org.eclipse.core.resources.IProject;
//import org.eclipse.jface.action.Action;
//
//public class SingleViolationPathChopperWithoutSummaryEdges implements IChopper {
//
//	public void initializeGUI(Action action) {
//		action.setText("Find Up To 100 Violation Pathes(max length: 100) dont use NJSec Summary Edges");
//		action.setImageDescriptor(NJSecPlugin.getDefault().getImageRegistry().getDescriptor("pdg"));
//	}
//
//	public List<Violation> addChop(IProject p, Collection<Violation> violations, SDG g, IStaticLattice<String> l) {
//		try {
//			/**
//			 * search up to 100 Pathes per Violation
//			 * search for pathes up to maximum length of 100 Nodes
//			 */
//			return new IncrementalViolationPathGen().addChop(violations, g, l, 100, 1000, false);
//		} catch (NotInLatticeException e) { }
//
//		return null;
//	}
//}
