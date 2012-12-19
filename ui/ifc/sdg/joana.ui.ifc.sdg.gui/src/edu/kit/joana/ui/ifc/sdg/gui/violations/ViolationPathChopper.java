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

import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.core.violations.Violation.Classification;
import edu.kit.joana.ifc.sdg.core.violations.paths.PathGenerator;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPathes;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;

public class ViolationPathChopper implements IChopper {

	public void initializeGUI(Action action) {
		action.setText("Find Most Dangerous Paths");
		action.setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("pdg"));
	}

	public Violation addChop(IProject p, Violation violation, SDG g, IStaticLattice<String> l) {
		Violation v = Violation.createViolation(violation.getSink(), violation.getSource(), violation.getViolationPathes(), violation.getAttackerLevel());
		for (Classification c : violation.getClassifications()) {
			v.addClassification(c.getName(), c.getDescription(), c.getSeverity(), c.getRating());
		}
		v.setViolationPathes(new ViolationPathes());
		try {
			PathGenerator pg = new PathGenerator(g);
			return pg.computeAllPaths(v);

		} catch (NotInLatticeException e) { }

		return null;
	}

	@Override
	public String getName() {
		return "Most Dangerous Paths";
	}
}
