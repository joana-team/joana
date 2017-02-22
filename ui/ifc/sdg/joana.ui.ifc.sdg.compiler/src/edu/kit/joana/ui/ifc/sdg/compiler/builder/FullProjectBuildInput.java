/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.builder;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import edu.kit.joana.ui.ifc.sdg.compiler.util.JoanaNatureUtil;

/**
 * A <code>IBuildInput</code> containing all java files in a projects build
 * path.
 *
 */
class FullProjectBuildInput implements IBuildInput {
	private final Collection<IFile> filesToBuild = new ArrayList<IFile>();

	private final Collection<IFile> filesToRemove = new ArrayList<IFile>();

	public Collection<IFile> getFilesToBuild() {
		return filesToBuild;
	}

	public Collection<IFile> getFilesToRemove() {
		return filesToRemove;
	}

	/**
	 * Constructor
	 *
	 * @param project
	 *            the <code>IProject</code> to collect the java files in its
	 *            build path for.
	 * @throws CoreException
	 *             if the files to build could not be determined.
	 */
	public FullProjectBuildInput(IProject project) throws CoreException {
		walk(project);
	}

	private void walk(IResource res) throws CoreException {
		if (JoanaNatureUtil.isBuildResource(res))
			filesToBuild.add((IFile) res);
		else if (res instanceof IContainer)
			for (IResource child : ((IContainer) res).members())
				walk(child);
	}
}
