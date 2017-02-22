/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.util;

import java.io.File;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Various general purpose plugin utilities.
 *
 */
public final class PluginUtil {

	private PluginUtil() {}

	/**
	 * Adds a nature to a project
	 *
	 * @param project
	 *            the project to add the nature to
	 * @param natureID
	 *            the ID of the nature add to the project
	 * @throws CoreException
	 *             if the nature could not be added
	 */
	public static void addNature(IProject project, String natureID) throws CoreException {
		if (hasNature(project, natureID))
			return;
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = natureID;
		description.setNatureIds(newNatures);
		project.setDescription(description, null);
	}

	private static IProgressMonitor defaultProgress;

	public static IProgressMonitor getDefaultProgressMonitor() {
		if (defaultProgress == null) {
			defaultProgress = new NullProgressMonitor();
		}

		return defaultProgress;
	}

	/**
	 * Converts a workspace relative path to a absolute path in the filesystem
	 *
	 * @param workspaceRoot
	 *            the root of the workspace
	 * @param path
	 *            the relative path
	 * @return the absolute path in the filesystem
	 */
	public static String workspaceRelativePath2Filesystem(IWorkspaceRoot workspaceRoot, IPath path) {
		return workspaceRoot.getLocation().removeTrailingSeparator().toOSString() + path.toOSString();
	}

	/**
	 * Indicates if a project has a nature
	 *
	 * @param project
	 *            the project to check
	 * @param id
	 *            the nature to look for
	 * @return <code>true</code> if <code>project</code> has the specified
	 *         nature, else <code>false</code>
	 * @throws CoreException
	 *             if an error occured during the process.
	 */
	public static boolean hasNature(IProject project, String id) throws CoreException {
		for (String natureID : project.getDescription().getNatureIds())
			if (natureID.equals(id))
				return true;
		return false;
	}

	/**
	 * Removes a nature from a project
	 *
	 * @param project
	 *            the project to remove the nature from
	 * @param id
	 *            the ID of the nature to remove
	 * @throws CoreException
	 *             if the nature could not be removed.
	 */
	public static void removeNature(IProject project, String id) throws CoreException {
		if (!hasNature(project, id))
			return;

		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();
		String[] newNatures = new String[natures.length - 1];
		int j = 0;
		for (int i = 0; i < natures.length; i++)
			if (!natures[i].equals(id))
				newNatures[j++] = natures[i];
		description.setNatureIds(newNatures);
		project.setDescription(description, null);
	}

	/**
	 * Toggles a nature assignment state on a project
	 *
	 * @param project
	 *            the project to toggle the nature assignment state on
	 * @param natureID
	 *            the id of the nature to toggle
	 * @throws CoreException
	 *             if the nature assignment state could not be toggled.
	 */
	public static void toggleNature(IProject project, String natureID) throws CoreException {
		if (hasNature(project, natureID))
			removeNature(project, natureID);
		else
			addNature(project, natureID);
	}

	/**
	 * Indicates if a project has a builder assigned to its build specification
	 *
	 * @param project
	 *            the project to check the builder assignment for
	 * @param id
	 *            the id of the builder to check
	 * @return <code>true</code> if the builder is in the project's build
	 *         spec, else <code>false</code>
	 * @throws CoreException
	 *             if the builder assignment could not be determined.
	 */
	public static boolean hasBuilder(IProject project, String id) throws CoreException {
		for (ICommand builder : project.getDescription().getBuildSpec())
			if (builder.getBuilderName().equals(id))
				return true;
		return false;
	}

	/**
	 * Adds a builder to the build specification of a project
	 *
	 * @param project
	 *            the project to add the builder to
	 * @param id
	 *            the ID of the builder to add to the project
	 * @throws CoreException
	 *             if the builder could not be added to the project's build
	 *             specification
	 */
	public static void addBuilder(IProject project, String id) throws CoreException {
		if (hasBuilder(project, id))
			return;
		IProjectDescription desc = project.getDescription();
		ICommand[] builders = desc.getBuildSpec();
		ICommand[] newBuilders = new ICommand[builders.length + 1];
		System.arraycopy(builders, 0, newBuilders, 1, builders.length);
		ICommand cmd = newBuilders[0] = desc.newCommand();
		cmd.setBuilderName(id);
		desc.setBuildSpec(newBuilders);
		project.setDescription(desc, null);
	}

	/**
	 * Removes a builder from a project's build specification
	 *
	 * @param project
	 *            the project to remove the builder form
	 * @param id
	 *            the ID of the builder to remove
	 * @throws CoreException
	 *             if the builder could not be removed from the project's build
	 *             specification
	 */
	public static void removeBuilder(IProject project, String id) throws CoreException {
		if (!hasBuilder(project, id))
			return;

		IProjectDescription description = project.getDescription();
		ICommand[] builders = description.getBuildSpec();
		ICommand[] newBuilders = new ICommand[builders.length - 1];
		int j = 0;
		for (int i = 0; i < builders.length; i++)
			if (!builders[i].getBuilderName().equals(id))
				newBuilders[j++] = builders[i];
		description.setBuildSpec(newBuilders);
		project.setDescription(description, null);
	}

	/**
	 * Ensures that a certain folder exists, along with all its parents
	 *
	 * @param folder
	 *            the folder whose existance to ensure
	 */
	public static void ensureFolderExists(File folder) {
		if (!folder.exists())
			ensureFolderExists(folder.getParentFile());
		folder.mkdir();
	}

	/**
	 * Ensures that a certain folder exists, along with all its parents
	 *
	 * @param folder
	 *            the folder whose existance to ensure
	 * @throws CoreException
	 *             if the folder could not be created.
	 */
	public static void ensureFolderExists(IFolder folder) throws CoreException {
		if (folder.exists())
			return;
		if (folder.getParent() instanceof IFolder)
			ensureFolderExists((IFolder) folder.getParent());
		folder.create(true, true, null);
	}

	/**
	 * Returns the filesystem location of a resource
	 *
	 * @param res
	 *            the resource to get the filesystem location for
	 * @return the filesystem location of the resource
	 */
	public static String getFilesystemPathString(IResource res) {
		// TODO: getLocationURI() can return null
		return res.getLocationURI().getPath();
	}

}
