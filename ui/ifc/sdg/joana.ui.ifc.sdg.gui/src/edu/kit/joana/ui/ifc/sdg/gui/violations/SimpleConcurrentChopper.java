/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.violations;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;

import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.core.violations.paths.ConcurrentViolationChop;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;

public class SimpleConcurrentChopper implements IChopper {

	public void initializeGUI(Action action) {
		action.setText("Context-Sensitive Chop");
		action.setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("pdg"));
	}

	public Violation addChop(IProject p, Violation violation, SDG g, IStaticLattice<String> l) {
		Collection<Violation> violations = new LinkedList<Violation>();
		violations.add(violation);
		try {
			return ConcurrentViolationChop.getInstance().addChop(violations, g).get(0);

		} catch (NotInLatticeException e) { }

		return null;
	}

	@Override
	public String getName() {
		return "SimpleConcurrentChop";
	}
}
