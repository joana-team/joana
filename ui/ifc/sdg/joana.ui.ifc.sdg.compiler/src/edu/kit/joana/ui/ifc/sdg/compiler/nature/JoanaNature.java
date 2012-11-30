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
package edu.kit.joana.ui.ifc.sdg.compiler.nature;

import java.io.File;
import java.io.IOException;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;

import edu.kit.joana.ui.ifc.sdg.compiler.builder.IJoanaBuildProblemMarker;
import edu.kit.joana.ui.ifc.sdg.compiler.builder.JoanaBuilder;
import edu.kit.joana.ui.ifc.sdg.compiler.util.Activator;
import edu.kit.joana.ui.ifc.sdg.compiler.util.JoanaNatureUtil;
import edu.kit.joana.ui.ifc.sdg.compiler.util.PluginUtil;

/**
 * A eclipse core IProjectNature for the JOANA nature. It handles
 * attachment/detachment of the JOANA builder for a project and the
 * creation/deletion of the plugins working directories.
 *
 */
public class JoanaNature implements IProjectNature {

	private IProject project;

	/**
	 * The qualified ID of the JOANA nature
	 */
	public static final String ID = Activator.PLUGIN_ID + ".joanaNature";

	/**
	 * Constructor
	 */
	public JoanaNature() {
	}

	public void configure() throws CoreException {
//		System.out.println("Configure");
		try {
			// Create the nature's required directories
			setupNatureDirectories();

			// Add the builder to the project
			PluginUtil.addBuilder(project, JoanaBuilder.ID);

			// Notify all interested parties
			Activator.getDefault().fireJoanaNatureChanged(JavaCore.create(project));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setupNatureDirectories() throws IOException {
		File projectWorkFolder = JoanaNatureUtil.getProjectWorkFolder(project);
		if (!projectWorkFolder.exists())
			projectWorkFolder.mkdir();

		File projectBuildFolder = JoanaNatureUtil.getProjectBuildFolder(project);
		if (!projectBuildFolder.exists())
			projectBuildFolder.mkdir();
	}

	public void deconfigure() throws CoreException {
//		System.out.println("Deconfigure");
		try {
			// Remove joana builder from project
			PluginUtil.removeBuilder(project, JoanaBuilder.ID);

			// Cleanup joana builder related markers
			project.deleteMarkers(IJoanaBuildProblemMarker.ID, true, IResource.DEPTH_INFINITE);

			// Delete build cache etc.
			cleanupNatureDirectories();

			// Notify all interested parties
			Activator.getDefault().fireJoanaNatureChanged(JavaCore.create(project));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void cleanupNatureDirectories() {
		File projectBuildFolder = JoanaNatureUtil.getProjectWorkFolder(project);
		if (projectBuildFolder.exists())
			projectBuildFolder.delete();
	}

	public IProject getProject() {
		return project;
	}

	public void setProject(IProject project) {
		this.project = project;
	}

}
