/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.builder;

import java.util.ArrayList;
import java.util.List;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import edu.kit.joana.ui.ifc.sdg.compiler.util.PluginUtil;

/**
 * A eclipse core resources IResourceChangeListener watching for file
 * modifications to schedule a rebuild of affected projects with the JOANA
 * nature.
 *
 */
public class FileChangeListener implements IResourceChangeListener {

	private IWorkspace workspace = ResourcesPlugin.getWorkspace();

	public void resourceChanged(IResourceChangeEvent event) {
		List<IProject> projects = walk(event.getDelta());
		for (IProject project : projects)
			JoanaBuilder.scheduleBuild(project, true);
	}

	private List<IProject> walk(IResourceDelta delta) {
		List<IProject> ret = new ArrayList<IProject>();

		if (delta.getResource() instanceof IFile) {
			// Ignore files that were not modified
			if (delta.getKind() != IResourceDelta.CHANGED || (delta.getFlags() & IResourceDelta.CONTENT) == 0)
				return ret;

			IFile file = (IFile) delta.getResource();
			if (!file.getProjectRelativePath().getFileExtension().equals("java"))
				return ret;

			for (IProject p : getAffectedJoanaProjects(file))
				if (!ret.contains(p))
					ret.add(p);
		}

		for (IResourceDelta c : delta.getAffectedChildren())
			for (IProject p : walk(c))
				if (!ret.contains(p))
					ret.add(p);

		return ret;
	}

	private List<IProject> getAffectedJoanaProjects(IFile file) {
		List<IProject> ret = new ArrayList<IProject>();

		for (IProject proj : workspace.getRoot().getProjects()) {
			try {
			    if (!proj.isAccessible()) continue;

				if (!PluginUtil.hasBuilder(proj, JoanaBuilder.ID))
					continue;
				IJavaProject javaProject = JavaCore.create(proj);
				if (javaProject.isOnClasspath(file))
					ret.add(proj);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}
}
