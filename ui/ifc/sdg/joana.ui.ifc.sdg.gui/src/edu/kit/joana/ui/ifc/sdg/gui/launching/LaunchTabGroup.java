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

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;

public class LaunchTabGroup implements ILaunchConfigurationTabGroup {
	private ArrayList<ILaunchConfigurationTab> tabs = new ArrayList<ILaunchConfigurationTab>();

	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
	    tabs.add(new ConfigurationMainTab());
        tabs.add(new ConfigurationIFCTab());
        tabs.add(new CommonTab());
	}

	public ILaunchConfigurationTab[] getTabs() {
		return tabs.toArray(new ILaunchConfigurationTab[0]);
	}

	public void dispose() { }

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	    for (ILaunchConfigurationTab tab : tabs) {
	        tab.setDefaults(configuration);
	    }
	}

	public void initializeFrom(ILaunchConfiguration configuration) {
        for (ILaunchConfigurationTab tab : tabs) {
            tab.initializeFrom(configuration);
        }

	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        for (ILaunchConfigurationTab tab : tabs) {
            tab.performApply(configuration);
        }
	}

	public void launched(ILaunch launch) { }
}
