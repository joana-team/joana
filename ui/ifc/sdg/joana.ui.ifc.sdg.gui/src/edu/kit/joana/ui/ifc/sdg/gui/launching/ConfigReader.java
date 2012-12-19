/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.launching;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;

public class ConfigReader {

	public ILaunchConfiguration configuration;

	public ConfigReader(ILaunchConfiguration configuration) {
		this.configuration = configuration;
	}

	public String getProjectName() throws CoreException {
		if (configuration == null) return null;
		return configuration.getAttribute(ConfigurationAttributes.PROJECT_NAME, "");
	}

	public String getMainClassName() throws CoreException {
		if (configuration == null) return null;
		return configuration.getAttribute(ConfigurationAttributes.MAIN_CLASS_NAME, "");
	}

	public String getLatticeLocation() throws CoreException {
		if (configuration == null) return null;
		return configuration.getAttribute(ConfigurationAttributes.LATTICE_LOCATION, "");
	}

	public String getSDGLocation() throws CoreException {
		if (configuration == null) return null;
		return configuration.getAttribute(ConfigurationAttributes.SDG_LOCATION, "");
	}

	public boolean getUseJoanaCompiler() throws CoreException {
		if (configuration == null)  return true;
		return configuration.getAttribute(ConfigurationAttributes.USE_JOANA_COMPILER, true);
	}

	public boolean getGenerateSimpleChop() throws CoreException {
		if (configuration == null) return false;
		return configuration.getAttribute(ConfigurationAttributes.GEN_SIMPLE_CHOP, false);
	}

	public boolean isStandardConfiguration() throws CoreException {
		if (configuration == null) return false;
		return configuration.getAttribute(ConfigurationAttributes.IS_PROJECT_STANDARD, false);
	}

	public IProject getProject() throws CoreException {
		if (configuration == null) return null;
		String projectName = getProjectName();
		if (projectName.equals("")) return null;
		IJavaModel ijm = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		IProject project = ijm.getJavaProject(projectName).getProject();
		return project;
	}

	public String getMemoryXMX(String defaultValue) throws CoreException {
        if (configuration == null) return defaultValue;
        return configuration.getAttribute(ConfigurationAttributes.MEMORY_XMX, defaultValue);
	}

    public String getJavaHome(String defaultValue) throws CoreException {
        if (configuration == null) return defaultValue;
        return configuration.getAttribute(ConfigurationAttributes.JAVA_HOME, defaultValue);
    }

    public String getStubs(String defaultValue) throws CoreException {
        if (configuration == null) return defaultValue;
        return configuration.getAttribute(ConfigurationAttributes.STUBS, defaultValue);
    }

    public String getSDGLib(String defaultValue) throws CoreException {
        if (configuration == null) return defaultValue;
        return configuration.getAttribute(ConfigurationAttributes.SDG_LIB, defaultValue);
    }

    public boolean getWholeFlag() throws CoreException {
        if (configuration == null) return false;
        return configuration.getAttribute(ConfigurationAttributes.WHOLE, false);
    }

    public boolean getConcurrentSDG() throws CoreException {
        if (configuration == null) return false;
        return configuration.getAttribute(ConfigurationAttributes.CONC, false);
    }

    public boolean getSummaryFlag() throws CoreException {
        if (configuration == null) return true;
        return configuration.getAttribute(ConfigurationAttributes.SUMMARY, true);
    }

    public boolean getFineGrainedFlag() throws CoreException {
        if (configuration == null) return false;
        return configuration.getAttribute(ConfigurationAttributes.FINE_GRAINED, false);
    }

    public boolean getInterferenceFlag() throws CoreException {
        if (configuration == null) return true;
        return configuration.getAttribute(ConfigurationAttributes.INTERFERENCE, true);
    }

    public boolean getCFGFlag() throws CoreException {
        if (configuration == null) return false;
        return configuration.getAttribute(ConfigurationAttributes.CFG, false);
    }

    public String getLibraryFilter(String defaultValue) throws CoreException {
        if (configuration == null) return defaultValue;
        return configuration.getAttribute(ConfigurationAttributes.FILTER, defaultValue);
    }

    public boolean getClassicNI() throws CoreException {
        if (configuration == null) return true;
        return configuration.getAttribute(ConfigurationAttributes.CLASS_NI, true);
    }

    public boolean getClassicNIWithTermination() throws CoreException {
        if (configuration == null) return false;
        return configuration.getAttribute(ConfigurationAttributes.CLASS_NI_TS, false);
    }

    public boolean getKrinkeNI() throws CoreException {
        if (configuration == null) return true;
        return configuration.getAttribute(ConfigurationAttributes.KRINKE_NI, false);
    }

    public boolean getProbabilisticNI() throws CoreException {
        if (configuration == null) return false;
        return configuration.getAttribute(ConfigurationAttributes.PROB_NI, false);
    }

    public boolean getProbabilisticNIWithTermination() throws CoreException {
        if (configuration == null) return false;
        return configuration.getAttribute(ConfigurationAttributes.PROB_NI_TS, false);
    }

    public boolean getPossibilisticNI() throws CoreException {
        if (configuration == null) return false;
        return configuration.getAttribute(ConfigurationAttributes.POSS_NI, false);
    }

    public boolean getPossibilisticNIWithTermination() throws CoreException {
        if (configuration == null) return false;
        return configuration.getAttribute(ConfigurationAttributes.POSS_NI_TS, false);
    }


    /* More complex functions */

    public IProject getIProject() throws CoreException {
        String projectName = getProjectName();
        IJavaModel ijm = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
        IProject project = ijm.getJavaProject(projectName).getProject();
        return project;
    }

    public IJavaProject getIJavaProject() throws CoreException {
        String projectName = getProjectName();
        IJavaModel ijm = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
        return ijm.getJavaProject(projectName);
    }

    public IJavaElement getMainClass() throws CoreException {
        IJavaProject jp = getIJavaProject();
        String mainClass = getMainClassName();
        IPath mainPath = new Path(mainClass);
        IJavaElement mainFile = jp.findElement(mainPath);

        if (mainFile == null) {
            IStatus status= new Status(IStatus.ERROR, NJSecPlugin.singleton().getSymbolicName(),
                    "Invalid Name or Path for Main File in edu.kit.joana.ifc.sdg.gui.launching.ConfigReader.getMainClass()");
            throw new CoreException(status);
        }

        return mainFile;
    }
}
