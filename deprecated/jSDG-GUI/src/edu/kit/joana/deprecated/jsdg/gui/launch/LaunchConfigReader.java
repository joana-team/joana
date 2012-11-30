/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.gui.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import edu.kit.joana.deprecated.jsdg.SDGFactory;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config.PointsToType;
import edu.kit.joana.deprecated.jsdg.util.Log.LogLevel;

public class LaunchConfigReader {

	private final ILaunchConfiguration conf;
	private SDGFactory.Config sdgConf = null;

	public LaunchConfigReader(ILaunchConfiguration conf) {
		this.conf = conf;
	}

	public SDGFactory.Config getSDGConfig() throws CoreException {
		if (sdgConf == null) {
			populateSDGConf();
		}

		return sdgConf;
	}

	private void populateSDGConf() throws CoreException {
		if (conf == null) {
			throw new IllegalStateException("Launch configuration is null.");
		}
		sdgConf = new SDGFactory.Config();
		sdgConf.mainClass = getMainClassBytecodeName();
		sdgConf.exclusions = getExclusions();
		IJavaProject project = getIJavaProject();
		IPath binDir = project.getOutputLocation();
		IFile binDirFile = ResourcesPlugin.getWorkspace().getRoot().getFile(binDir);
		IPath path = binDirFile.getLocation();
		File fileBinDir = path.toFile();

		assert (fileBinDir.exists()) : "Directory with binary files does not exist: " + fileBinDir;
		assert (fileBinDir.isDirectory()) : "This is not a directory: " + fileBinDir;

		sdgConf.classpath = fileBinDir.getAbsolutePath();

		IPath pathToProject = project.getProject().getLocation();
		File fileProjectPath = pathToProject.toFile();
		sdgConf.outputDir = fileProjectPath.getAbsolutePath();
		sdgConf.outputSDGfile = getSDGFileName();

		String tmp = getLogfile();//"/afs/info.uni-karlsruhe.de/user/grafj/Desktop/jsdg_output/edu.kit.joana.deprecated.jsdg.gui.log";
		if (tmp.trim().equals("")) {
			sdgConf.logFile = null;
		} else {
			sdgConf.logFile = tmp;
		}
		sdgConf.invertExclusion = isInvertExclusion();
		sdgConf.ignoreExceptions = isIgnoreExceptions();
		sdgConf.optimizeExceptions = isOptimizeExceptions();
		sdgConf.simpleDataDependency = isSimpleDataDependency();
		sdgConf.nonTermination = isNonTermination();
		sdgConf.addControlFlow = isAddControlFlow();
        sdgConf.computeInterference = isComputeInterference();
        //sdgConf.interferenceOptimizeThisAccess = isInterferenceOptimize();
		sdgConf.pointsTo = getPointsToType();
		sdgConf.logLevel = getLogLevel();
		sdgConf.scopeData =  getScopeFileData();
	}

	public static void setInvertExclusion(ILaunchConfigurationWorkingCopy conf) throws CoreException {
		setInvertExclusion(conf, LaunchConfig.EXCLUSIONS_INVERT_DEFAULT);
	}

	public static void setInvertExclusion(ILaunchConfigurationWorkingCopy conf, Boolean val) throws CoreException {
		if (conf == null) {
			throw new CoreException(new Status(IStatus.WARNING, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, "No configuration object provided."));
		}

		conf.setAttribute(LaunchConfig.EXCLUSIONS_INVERT, val);
	}

	public Boolean isInvertExclusion() throws CoreException {
		if (conf == null) {
			return null;
		}

		return conf.getAttribute(LaunchConfig.EXCLUSIONS_INVERT, LaunchConfig.EXCLUSIONS_INVERT_DEFAULT);
	}

	public static void setNonTermination(ILaunchConfigurationWorkingCopy conf) throws CoreException {
		setNonTermination(conf, LaunchConfig.NON_TERMINATION_DEFAULT);
	}

	public static void setNonTermination(ILaunchConfigurationWorkingCopy conf, Boolean val) throws CoreException {
		if (conf == null) {
			throw new CoreException(new Status(IStatus.WARNING, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, "No configuration object provided."));
		}

		conf.setAttribute(LaunchConfig.NON_TERMINATION, val);
	}

	public Boolean isNonTermination() throws CoreException {
		if (conf == null) {
			return null;
		}

		return conf.getAttribute(LaunchConfig.NON_TERMINATION, LaunchConfig.NON_TERMINATION_DEFAULT);
	}

	public static void setIgnoreExceptions(ILaunchConfigurationWorkingCopy conf) throws CoreException {
		setInvertExclusion(conf, LaunchConfig.IGNORE_EXCEPTIONS_DEFAULT);
	}

	public static void setIgnoreExceptions(ILaunchConfigurationWorkingCopy conf, Boolean val) throws CoreException {
		if (conf == null) {
			throw new CoreException(new Status(IStatus.WARNING, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, "No configuration object provided."));
		}

		conf.setAttribute(LaunchConfig.IGNORE_EXCEPTIONS, val);
	}

	public Boolean isIgnoreExceptions() throws CoreException {
		if (conf == null) {
			return null;
		}

		return conf.getAttribute(LaunchConfig.IGNORE_EXCEPTIONS, LaunchConfig.IGNORE_EXCEPTIONS_DEFAULT);
	}

	public static void setOptimizeExceptions(ILaunchConfigurationWorkingCopy conf, Boolean val) throws CoreException {
		if (conf == null) {
			throw new CoreException(new Status(IStatus.WARNING, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, "No configuration object provided."));
		}

		conf.setAttribute(LaunchConfig.OPTIMIZE_EXCEPTIONS, val);
	}

	public Boolean isOptimizeExceptions() throws CoreException {
		if (conf == null) {
			return null;
		}

		return conf.getAttribute(LaunchConfig.OPTIMIZE_EXCEPTIONS, LaunchConfig.OPTIMIZE_EXCEPTIONS_DEFAULT);
	}

	public static void setSimpleDataDependency(ILaunchConfigurationWorkingCopy conf) throws CoreException {
		setInvertExclusion(conf, LaunchConfig.SIMPLE_DATA_DEP_DEFAULT);
	}

	public static void setSimpleDataDependency(ILaunchConfigurationWorkingCopy conf, Boolean val) throws CoreException {
		if (conf == null) {
			throw new CoreException(new Status(IStatus.WARNING, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, "No configuration object provided."));
		}

		conf.setAttribute(LaunchConfig.SIMPLE_DATA_DEP, val);
	}

	public Boolean isSimpleDataDependency() throws CoreException {
		if (conf == null) {
			return null;
		}

		return conf.getAttribute(LaunchConfig.SIMPLE_DATA_DEP, LaunchConfig.SIMPLE_DATA_DEP_DEFAULT);
	}

	public static void setAddControlFlow(ILaunchConfigurationWorkingCopy conf) throws CoreException {
		setInvertExclusion(conf, LaunchConfig.ADD_CONTROL_FLOW_DEFAULT);
	}

	public static void setAddControlFlow(ILaunchConfigurationWorkingCopy conf, Boolean val) throws CoreException {
		if (conf == null) {
			throw new CoreException(new Status(IStatus.WARNING, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, "No configuration object provided."));
		}

		conf.setAttribute(LaunchConfig.ADD_CONTROL_FLOW, val);
	}

	public Boolean isAddControlFlow() throws CoreException {
		if (conf == null) {
			return null;
		}

		return conf.getAttribute(LaunchConfig.ADD_CONTROL_FLOW, LaunchConfig.ADD_CONTROL_FLOW_DEFAULT);
	}

	public static void setComputeInterference(ILaunchConfigurationWorkingCopy conf) throws CoreException {
		setInvertExclusion(conf, LaunchConfig.COMPUTE_INTERFERENCE_DEFAULT);
	}

	public static void setComputeInterference(ILaunchConfigurationWorkingCopy conf, Boolean val) throws CoreException {
		if (conf == null) {
			throw new CoreException(new Status(IStatus.WARNING, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, "No configuration object provided."));
		}

		conf.setAttribute(LaunchConfig.COMPUTE_INTERFERENCE, val);
	}

	public Boolean isComputeInterference() throws CoreException {
		if (conf == null) {
			return null;
		}

		return conf.getAttribute(LaunchConfig.COMPUTE_INTERFERENCE, LaunchConfig.COMPUTE_INTERFERENCE_DEFAULT);
	}

	public static void setProjectName(ILaunchConfigurationWorkingCopy conf, String txt) throws CoreException {
		if (conf == null) {
			throw new CoreException(new Status(IStatus.WARNING, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, "No configuration object provided."));
		}

		conf.setAttribute(LaunchConfig.PROJECT_NAME, txt);
	}

	public static void setProjectName(ILaunchConfigurationWorkingCopy conf) throws CoreException {
		setProjectName(conf, LaunchConfig.PROJECT_NAME_DEFAULT);
	}

	public String getProjectName() throws CoreException {
		if (conf == null) {
			return null;
		}

		return conf.getAttribute(LaunchConfig.PROJECT_NAME, LaunchConfig.PROJECT_NAME_DEFAULT);
	}

	public static void setLogfile(ILaunchConfigurationWorkingCopy conf, String txt) throws CoreException {
		if (conf == null) {
			throw new CoreException(new Status(IStatus.WARNING, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, "No configuration object provided."));
		}

		conf.setAttribute(LaunchConfig.LOGFILE, txt);
	}

	public static void setLogfile(ILaunchConfigurationWorkingCopy conf) throws CoreException {
		setLogfile(conf, LaunchConfig.LOGFILE_DEFAULT);
	}

	public String getLogfile() throws CoreException {
		if (conf == null) {
			return null;
		}

		return conf.getAttribute(LaunchConfig.LOGFILE, LaunchConfig.LOGFILE_DEFAULT);
	}

	public static void setMainClassName(ILaunchConfigurationWorkingCopy conf, String txt) throws CoreException {
		if (conf == null) {
			throw new CoreException(new Status(IStatus.WARNING, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, "No configuration object provided."));
		}

		conf.setAttribute(LaunchConfig.MAIN_CLASS_NAME, txt);
	}

	public static void setMainClassName(ILaunchConfigurationWorkingCopy conf) throws CoreException {
		setMainClassName(conf, LaunchConfig.MAIN_CLASS_NAME_DEFAULT);
	}

	public String getMainClassBytecodeName() throws CoreException {
		if (conf == null) {
			return null;
		}

		/*
		 * convert "pkg1.pkg2/Class.java" to "Lpkg1/pkg2/Class"
		 */
		String main = getMainClassName();

		main = "L" + main.substring(0, main.lastIndexOf('.'));
		main = main.replace('.', '/');

		return main;
	}

	public String getSDGFileName() throws CoreException {
		if (conf == null) {
			return null;
		}

		String main = getMainClassName();
		main = main.replace('/', '.');
		main = main.substring(0, main.lastIndexOf('.'));
		main += ".pdg";

		return main;
	}

	public String getMainClassName() throws CoreException {
		if (conf == null) {
			return null;
		}

		return conf.getAttribute(LaunchConfig.MAIN_CLASS_NAME, LaunchConfig.MAIN_CLASS_NAME_DEFAULT);
	}

	public String getExclusionsTxt() throws CoreException {
		if (conf == null) {
			return null;
		}

		return conf.getAttribute(LaunchConfig.EXCLUSIONS, LaunchConfig.EXCLUSIONS_DEFAULT);

	}

	public static void setExclusionsTxt(ILaunchConfigurationWorkingCopy conf, String txt) throws CoreException {
		if (conf == null) {
			throw new CoreException(new Status(IStatus.WARNING, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, "No configuration object provided."));
		}

		conf.setAttribute(LaunchConfig.EXCLUSIONS, txt);
	}

	public static void setExclusionsTxt(ILaunchConfigurationWorkingCopy conf) throws CoreException {
		setExclusionsTxt(conf, LaunchConfig.EXCLUSIONS_DEFAULT);
	}

	public List<String> getExclusions() throws CoreException {
		if (conf == null) {
			return null;
		}

		String excl = getExclusionsTxt();

		List<String> result = new ArrayList<String>();

		StringTokenizer strTok = new StringTokenizer(excl, LaunchConfig.NEW_LINE + "\n ;,\r\n");
		while (strTok.hasMoreTokens()) {
			result.add(strTok.nextToken());
		}

		return result;
	}

	public static void setPointsToType(ILaunchConfigurationWorkingCopy conf, String txt) throws CoreException {
		if (conf == null) {
			throw new CoreException(new Status(IStatus.WARNING, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, "No configuration object provided."));
		}

		conf.setAttribute(LaunchConfig.POINTS_TO_TYPE, txt);
	}

	public static void setPointsToType(ILaunchConfigurationWorkingCopy conf) throws CoreException {
		setPointsToType(conf, LaunchConfig.POINTS_TO_TYPE_DEFAULT);
	}

	public PointsToType getPointsToType() throws CoreException {
		if (conf == null) {
			return null;
		}

		String ptsStr = conf.getAttribute(LaunchConfig.POINTS_TO_TYPE, LaunchConfig.POINTS_TO_TYPE_DEFAULT);

		for (PointsToType type : PointsToType.values()) {
			if (type.toString().equals(ptsStr)) {
				return type;
			}
		}

		return null;
	}

	public static void setLogLevel(ILaunchConfigurationWorkingCopy conf, String txt) throws CoreException {
		if (conf == null) {
			throw new CoreException(new Status(IStatus.WARNING, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, "No configuration object provided."));
		}

		conf.setAttribute(LaunchConfig.LOG_LEVEL, txt);
	}

	public static void setLogLevel(ILaunchConfigurationWorkingCopy conf) throws CoreException {
		setLogLevel(conf, LaunchConfig.LOG_LEVEL_DEFAULT);
	}

	public LogLevel getLogLevel() throws CoreException {
		if (conf == null) {
			return null;
		}

		String loglvlStr = conf.getAttribute(LaunchConfig.LOG_LEVEL, LaunchConfig.LOG_LEVEL_DEFAULT);

		for (LogLevel lvl : LogLevel.values()) {
			if (lvl.toString().equals(loglvlStr)) {
				return lvl;
			}
		}

		return null;
	}

	public List<String> getScopeFileData() throws CoreException {
		if (conf == null) {
			return null;
		}

		String excl = getScopeFileDataTxt();

		List<String> result = new ArrayList<String>();

		StringTokenizer strTok = new StringTokenizer(excl, LaunchConfig.NEW_LINE);
		while (strTok.hasMoreTokens()) {
			result.add(strTok.nextToken());
		}

		return result;
	}

	public String getScopeFileDataTxt() throws CoreException {
		if (conf == null) {
			return null;
		}

		return conf.getAttribute(LaunchConfig.SCOPE_FILE_DATA, LaunchConfig.SCOPE_FILE_DATA_DEFAULT);

	}

	public static void setScopeFileDataTxt(ILaunchConfigurationWorkingCopy conf, String txt) throws CoreException {
		if (conf == null) {
			throw new CoreException(new Status(IStatus.WARNING, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, "No configuration object provided."));
		}

		conf.setAttribute(LaunchConfig.SCOPE_FILE_DATA, txt);
	}

	public static void setScopeFileDataTxt(ILaunchConfigurationWorkingCopy conf) throws CoreException {
		setScopeFileDataTxt(conf, LaunchConfig.SCOPE_FILE_DATA_DEFAULT);
	}

    public IJavaProject getIJavaProject() throws CoreException {
        String projectName = getProjectName();
        IJavaModel ijm = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
        return ijm.getJavaProject(projectName);
    }


}
