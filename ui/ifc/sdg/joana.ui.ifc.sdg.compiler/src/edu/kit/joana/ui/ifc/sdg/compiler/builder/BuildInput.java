/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.builder;

import java.util.Collection;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;

import edu.kit.joana.ui.ifc.sdg.compiler.nature.JoanaNature;
import edu.kit.joana.ui.ifc.sdg.compiler.util.PluginUtil;

abstract class BuildInput {

	public BuildInput() {
	}

	public abstract Collection<IFile> getFilesToRemove();

	public abstract Collection<IFile> getFilesToBuild();

	protected boolean isBuildResource(IResource res) {
		if (!(res instanceof IFile)) {
			return false;
		}

		if (res.getProjectRelativePath().getFileExtension() == null
		        || !res.getProjectRelativePath().getFileExtension().equals("java")) {

			return false;
		}

		try {
			if (!PluginUtil.hasNature(res.getProject(), JoanaNature.ID))
				return false;
		} catch (CoreException e) {
			return false;
		}

		if (!JavaCore.create(res.getProject()).isOnClasspath(res)) {
			return false;
		}

		return true;
	}
}
