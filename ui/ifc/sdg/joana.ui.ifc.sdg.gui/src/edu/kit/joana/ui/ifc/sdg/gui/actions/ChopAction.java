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
package edu.kit.joana.ui.ifc.sdg.gui.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;

import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.core.violations.Violation.Chop;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.WrongLatticeDefinitionException;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;
import edu.kit.joana.ui.ifc.sdg.gui.launching.ConfigReader;
import edu.kit.joana.ui.ifc.sdg.gui.launching.LaunchConfigurationTools;
import edu.kit.joana.ui.ifc.sdg.gui.views.ViolationView;
import edu.kit.joana.ui.ifc.sdg.gui.violations.IChopper;

public class ChopAction extends Action {
	IChopper chopper;
	ViolationView av;

	public ChopAction(IChopper chopper, ViolationView av) {
		super();
		this.chopper=chopper;
		this.av = av;
	}

	public void run() {
		IProject p = NJSecPlugin.singleton().getActiveProject();
		Collection<Violation> vios = av.getSelected();
		SDG g = NJSecPlugin.singleton().getSDGFactory().getCachedSDG(p);

		try {
			ConfigReader cr = new ConfigReader(LaunchConfigurationTools.getStandardLaunchConfiguration(p));
			if (cr.configuration == null) {
	            IStatus status = new Status(IStatus.ERROR, NJSecPlugin.singleton().getSymbolicName(),
	            		"No NJSec-Standard-Launch-Configuration available");
	            NJSecPlugin.singleton().showError("No NJSec-Standard-Launch-Configuration available", status, null);
			}
			String latticeLocation = cr.getLatticeLocation();
			File latticeFile = new File(latticeLocation);
			IStaticLattice<String> l = LatticeUtil.compileBitsetLattice(new FileInputStream(latticeFile));

			for (Violation v : vios) {
				if (v.getChop(chopper.getName()) == null) {
					Violation withChop = chopper.addChop(p, v, g, l);

					if (withChop != null) {
						Chop c = new Chop(chopper.getName());
						c.setViolationPathes(withChop.getViolationPathes());
						v.addChop(c);
						av.violationsChanged(p);
						//av.setContent(withChop.toArray(new Violation[0]));
					} else {
						//av.setContent(null);
					}
				}
			}




		} catch (CoreException e) {
            NJSecPlugin.singleton().showError(e.getMessage(), null, e);

		} catch (WrongLatticeDefinitionException e) {
            NJSecPlugin.singleton().showError(e.getMessage(), null, e);

		} catch (IOException e) {
            NJSecPlugin.singleton().showError(e.getMessage(), null, e);
		}
	}
}
