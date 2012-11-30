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
package edu.kit.joana.ui.ifc.sdg.compiler.builder;

import java.util.Collection;

import org.eclipse.core.resources.IFile;

/**
 * Defines the interface for classes implementing a build input for the
 * <code>JoanaCompiler</code>.
 *
 */
interface IBuildInput {

	/**
	 * Files to remove the already generated build output for.
	 *
	 * @return a <code>Collection</code> of <code>IFile</code>s to remove
	 *         the already generated build output for.
	 */
	public Collection<IFile> getFilesToRemove();

	/**
	 * Files to be built during the build process.
	 *
	 * @return a <code>Collection</code> of <code>IFile</code>s to be built
	 *         during the build process.
	 */
	public Collection<IFile> getFilesToBuild();
}
