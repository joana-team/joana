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
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
//import edu.kit.joana.ifc.sdg.core.violations.Violation;
//import edu.kit.joana.ifc.sdg.gui.NJSecPlugin;
//import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
//import edu.kit.joana.ifc.sdg.graph.SDG;
//
//import org.eclipse.core.resources.IProject;
//import org.eclipse.jface.action.Action;
//
//public class NullChopper implements IChopper {
//
//	public List<Violation> addChop(IProject p, Collection<Violation> violations, SDG g, IStaticLattice<String> l) {
//		ArrayList<Violation> ret = new ArrayList<Violation>();
//		for (Violation v : violations) {
//			Violation newv = Violation.createViolation(v.getSink(), v.getSource(), v.getSink().getRequired());
//			ret.add(newv);
//		}
//		return ret;
//	}
//
//	public void initializeGUI(Action action) {
//		action.setText("Remove Chops and Paths");
//		action.setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("clear"));
//	}
//}
