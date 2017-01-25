/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.builder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.JavaModelException;

import com.sun.tools.javac.main.Main;

import edu.kit.joana.ui.ifc.sdg.compiler.configUI.ConfigConstants;
import edu.kit.joana.ui.ifc.sdg.compiler.util.Activator;
import edu.kit.joana.ui.ifc.sdg.compiler.util.JoanaNatureUtil;
import edu.kit.joana.ui.ifc.sdg.compiler.util.PDEUtil;
import edu.kit.joana.ui.ifc.sdg.compiler.util.PluginUtil;
/**
 * This class wraps the JOANA java compiler for convenient usage in the build
 * process.
 *
 */
public class JoanaCompiler {

	private static void markFile(IFile srcFile, String message, String cmdLine) {
		// Clear joana build markers
		try {
			srcFile.deleteMarkers(IJoanaBuildProblemMarker.ID, true, IResource.DEPTH_INFINITE);

			// Re-add if message is available
			if (message != null) {
				IMarker marker = srcFile.createMarker(IJoanaBuildProblemMarker.ID);
				marker.setAttribute(IMarker.MESSAGE, IJoanaBuildProblemMarker.PROBLEM_MESSAGE);
				marker.setAttribute(IMarker.SEVERITY, IJoanaBuildProblemMarker.PROBLEM_SEVERITY);
				marker.setAttribute(IJoanaBuildProblemMarker.COMPILER_OUTPUT, message);
				marker.setAttribute(IJoanaBuildProblemMarker.COMMAND_LINE, cmdLine);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private static String[] getCompilerCmdLineArgs(String srcPathStr, String dstPathStr, IProject project) {
		List<String> paramList = new ArrayList<String>();

		// Src and dst file
		paramList.add(srcPathStr);
		paramList.add("-d");
		paramList.add(dstPathStr);

		// enable detailed source info generation
		paramList.add("-Xsourceinfo");

		// adhere preference page settings
		boolean allowPrivate = ConfigConstants.getUsedPreferenceStore(project).getBoolean(ConfigConstants.COMPILER_PRIVATE_ACCESS);
		if (allowPrivate)
			paramList.add("-Xallowprivaccess");
		boolean allowUncaught = ConfigConstants.getUsedPreferenceStore(project).getBoolean(ConfigConstants.COMPILER_ALLOW_UNCAUGHT);
		if (allowUncaught)
			paramList.add("-Xallowuncaught");

		// check compliance level
		String qualifier = "org.eclipse.jdt.core";
		IEclipsePreferences prefStore = new ProjectScope(project).getNode(qualifier);
		String compLvl = "";
		try{
		    compLvl = prefStore.get("org.eclipse.jdt.core.compiler.compliance", null);
		} catch(Exception e) {
		    e.printStackTrace();
		}
		if (compLvl != null) {
		    paramList.add("-source");
		    paramList.add(compLvl);
		}

		   /*IProgressMonitor myProgressMonitor;
		   project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, myProgressMonitor);
		   org.eclipse.jdt.internal.compiler.batch.Main*/

		try {
			paramList.add("-cp");
			paramList.add(PDEUtil.buildClasspathString(project));
		} catch (JavaModelException e) {
			e.printStackTrace();
			// TODO clean handling of e
		}

		String[] params = paramList.toArray(new String[paramList.size()]);
		return params;
	}

	private static boolean build(IFile srcFile) {
		IProject project = srcFile.getProject();
		try {

			// Get folder that will contain the result .class file
			File dstFolder = JoanaNatureUtil.getProjectBuildFolder(project);
			PluginUtil.ensureFolderExists(dstFolder);

			// Get path of result .class file and delete if exists
			File dstFile = JoanaNatureUtil.getDestinationFile(srcFile);
			if (dstFile.exists())
				dstFile.delete();

			// Make filesystem paths for src file and dst folder
			String srcPathStr = PluginUtil.getFilesystemPathString(srcFile);
			String dstPathStr = dstFolder.getCanonicalPath();

			// Prepare cmd-line params for javac based on compiler configuration
			String[] params = getCompilerCmdLineArgs(srcPathStr, dstPathStr, project);
			String cmdLine = "javac ";
			for (int i = 0; i < params.length; i++)
				cmdLine += params[i] + " ";

			// Compile
			StringWriter stringWriter = new StringWriter();
			Main main = new Main("Something", new PrintWriter(stringWriter));
			int result = main.compile(params);

			if (result != 0) {
			    String resultStr = "";
			    switch(result) {
			    case 1: resultStr = "EXIT_ERROR"; break;
                case 2: resultStr = "EXIT_CMDERR"; break;
                case 3: resultStr = "EXIT_SYSERR"; break;
                case 4: resultStr = "EXIT_ABNORMAL"; break;
			    }
                System.out.println(">> Compilation error: " + srcFile + " returned with " + resultStr);
			}

			// Check if target file exists
			if (result == 0 && !dstFile.exists()) {
			    System.out.println("Not found: " + dstFile);
				markFile(srcFile, "Destination file could not be found", cmdLine);
				throw new FileNotFoundException();
			}

			// Set/unset compilation result depending on result
			markFile(srcFile, result == 0 ? null : stringWriter.toString(), cmdLine);
			return result == 0;
		} catch (Throwable t) {
		    t.printStackTrace();
			StringWriter stringWriter = new StringWriter();
			t.printStackTrace(new PrintWriter(stringWriter));
			markFile(srcFile, stringWriter.toString(), null);
			return false;
		}
	}

	private static void removeFile(IFile file) throws IOException, CoreException {
		File dstFile = JoanaNatureUtil.getDestinationFile(file);
		if (dstFile.exists())
			dstFile.delete();
	}

	/**
	 * Performs a full build of a provided <code>IBuildInput</code>. Files
	 * that could not be built will be annotated with JOANA build problem
	 * marker, previousely existing markers will be removed.
	 *
	 * @param input
	 *            the <code>IBuildInput</code> providing the list of files to
	 *            be removed and built.
	 * @param monitor
	 *            a <code>IProgressMonitor</code> for tracking the progress of
	 *            the build process.
	 * @param owningJob
	 *            the <code>BuildJob</code> issuing the build used to check if
	 *            the build process should be aborted.
	 * @return <code>true</code> if all build steps was successful, else
	 *         <code>false</code>
	 * @throws CoreException
	 */
	public static boolean build(IBuildInput input, IProgressMonitor monitor, BuildJob owningJob) throws CoreException {

		if (monitor == null)
			monitor = new NullProgressMonitor();

		Collection<IFile> build = input.getFilesToBuild();
		Collection<IFile> remove = input.getFilesToRemove();
		monitor.beginTask("Update Joana build cache", build.size() + remove.size());

		boolean succeeded = true;

		for (IFile file : build) {
			if (!build(file))
				succeeded = false;
			monitor.worked(1);
			if (owningJob.cancelled()) {
				monitor.done();
				return false;
			}
		}
		for (IFile file : remove) {
			try {
				removeFile(file);
			} catch (IOException e) {
				succeeded = false;
				monitor.done();
				throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, IStatus.OK, "Error removing file " + file, e));
			}
			monitor.worked(1);
			if (owningJob.cancelled()) {
				monitor.done();
				return false;
			}
		}
		monitor.done();
		return succeeded;
	}

}
