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
package edu.kit.joana.ui.ifc.sdg.compiler.util;

import java.io.File;
import java.io.IOException;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;

import edu.kit.joana.ui.ifc.sdg.compiler.nature.JoanaNature;

/**
 * Utilities for working with the JOANA nature.
 *
 */
public class JoanaNatureUtil {
	/**
	 * Returns a project's build folder.
	 *
	 * @param project
	 *            the project to get the build folder for.
	 *
	 * @return the project's build folder, or <code>null</code> if the folder
	 *         does not exist.
	 *
	 * @throws IOException
	 *             if an I/O error was thrown
	 */
	public static File getProjectBuildFolder(IProject project) throws IOException {
		File workFolder = getProjectWorkFolder(project);
		if (!workFolder.exists() || !workFolder.isDirectory())
			return null;
		return new File(workFolder.getCanonicalPath() + File.separator + "build" + File.separator);
	}

	/**
	 * Returns a project's work folder as provided by the
	 * org.eclipse.core.resources plugin.
	 *
	 * @param project
	 *            the project to get the work folder for.
	 * @return the project's work folder.
	 */
	public static File getProjectWorkFolder(IProject project) {
		return project.getWorkingLocation(Activator.PLUGIN_ID).toFile();
	}

	/**
	 * Returns a handle to the destination file location of a source file. This
	 * handle matches the path of the compilation result of the provided source
	 * file.
	 *
	 * @param srcFile
	 *            the Java file to get the destination path for.
	 * @return the path of the compilation result for the provided Java source
	 *         file.
	 * @throws IOException
	 *             if the destination file could not be determinded
	 * @throws CoreException
	 *             if the destination file could not be determined
	 */
	public static File getDestinationFile(IFile srcFile) throws IOException, CoreException {
		String pkg = PDEUtil.getPackage(srcFile).replace(".", File.separator);
		IPath dstFile = srcFile.getProjectRelativePath().removeFileExtension().addFileExtension("class");
		String fname = dstFile.segment(dstFile.segmentCount() - 1);
		String relPath = pkg + File.separator + fname;
		return new File(getProjectBuildFolder(srcFile.getProject()) + File.separator + relPath).getCanonicalFile();
	}

	/**
	 * Indicates if a resource if part of a JOANA build process.
	 *
	 * @param res
	 *            the resource to check
	 * @return <code>true</code> if the resource is supposed to be built by
	 *         the JOANA build process, else <code>false</code>
	 */
	public static boolean isBuildResource(IResource res) {
		if (!(res instanceof IFile))
			return false;
		if (res.getProjectRelativePath().getFileExtension() == null)
			return false;
		if (!res.getProjectRelativePath().getFileExtension().equals("java"))
			return false;
		try {
			if (!PluginUtil.hasNature(res.getProject(), JoanaNature.ID))
				return false;
		} catch (CoreException e) {
			return false;
		}
		if (!JavaCore.create(res.getProject()).isOnClasspath(res))
			return false;
		return true;
	}
}
