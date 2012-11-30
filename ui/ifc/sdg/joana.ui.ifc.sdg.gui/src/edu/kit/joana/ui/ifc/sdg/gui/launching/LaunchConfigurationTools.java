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
package edu.kit.joana.ui.ifc.sdg.gui.launching;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

public class LaunchConfigurationTools {

	public static void setAllNJSecLaunchersToNonStandard(IProject p) throws CoreException {
		ILaunchConfiguration[] allConfigsForProject = getAllNJSecLaunchConfiguration(p);
		if (allConfigsForProject == null) return;
		for (ILaunchConfiguration ilc : allConfigsForProject ) {
			if (ilc.getAttribute(ConfigurationAttributes.PROJECT_NAME, "").equals(p.getName())) {
				ILaunchConfigurationWorkingCopy ilcwc = ilc.getWorkingCopy();
				ilcwc.setAttribute(ConfigurationAttributes.IS_PROJECT_STANDARD, false);
				ilcwc.doSave();
			}
		}
	}

	/**
	 * Returns standard configuration for given project. returns null if no standard configuration found
	 * @param p
	 * @return
	 * @throws CoreException
	 */
	public static ILaunchConfiguration getStandardLaunchConfiguration(IProject p) throws CoreException {
		if (p == null) return null;

		ILaunchConfigurationType type =
			DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType("edu.kit.joana.ifc.sdg.gui.launching.NJSecLaunch");
		ILaunchConfiguration[] ilcs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(type);
		for (ILaunchConfiguration ilc : ilcs ) {
			if (ilc.getAttribute(ConfigurationAttributes.PROJECT_NAME, "").equals(p.getName())) {
				if (ilc.getAttribute(ConfigurationAttributes.IS_PROJECT_STANDARD, false)) {
					return ilc;
				}
			}
		}

		return null;
	}

	/**
	 * Returns standard configuration for given project. returns null if no standard configuration found
	 * @param p
	 * @return
	 * @throws CoreException
	 */
	public static ILaunchConfiguration[] getAllNJSecLaunchConfiguration(IProject p) throws CoreException {
		if (p == null) return new ILaunchConfiguration[0];

		ILaunchConfigurationType type =
			DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType("edu.kit.joana.ifc.sdg.gui.launching.NJSecLaunch");
		ILaunchConfiguration[] ilcs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(type);

		ArrayList<ILaunchConfiguration> ret = new ArrayList<ILaunchConfiguration>();

		for (ILaunchConfiguration ilc : ilcs ) {
			if (ilc.getAttribute(ConfigurationAttributes.PROJECT_NAME, "").equals(p.getName())) {
				ret.add(ilc);
			}
		}

		return ret.toArray(new ILaunchConfiguration[0]);
	}
}
