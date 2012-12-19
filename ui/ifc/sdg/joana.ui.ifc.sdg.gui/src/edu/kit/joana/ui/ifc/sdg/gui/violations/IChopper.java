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
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;

public interface IChopper {

	/**
	 * Thats where all the Chopping should happen
	 * Generate List<Violation>, where Violations do contain Chop
	 * @param p
	 * @param violations
	 * @param vcl
	 * @return
	 */
	public Violation addChop(IProject p, Violation violation, SDG g, IStaticLattice<String> l);

	/**
	 * use action.setText() to set caption for contextmenu-entry
	 * use action.setimageDescriptor() to set Icon for contextmenu-entry, and so on..
	 * @param action
	 */
	public void initializeGUI(Action action);

	/**
	 * Gets the Name of the Chop
	 * @return name
	 */
	public String getName();
}
