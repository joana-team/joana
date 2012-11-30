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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;

import edu.kit.joana.ifc.sdg.core.conc.ProbabilisticNISlicer;
import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;

public class ConflictsCollector implements IChopper {

	public void initializeGUI(Action action) {
		action.setText("Show Conflicts causing a Probabilistic Information Leak");
		action.setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("pdg"));
	}

	public Violation addChop(IProject p, Violation violations, SDG g, IStaticLattice<String> l) {
		try {
			return (Violation) ProbabilisticNISlicer.detailedCheck(g, l).check().toArray()[0];

		} catch (NotInLatticeException e) { }

		return null;
	}

	@Override
	public String getName() {
		return "ConflictsCollector";
	}
}
