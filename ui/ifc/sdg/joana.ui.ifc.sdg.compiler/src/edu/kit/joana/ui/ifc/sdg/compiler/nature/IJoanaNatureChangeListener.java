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

import org.eclipse.jdt.core.IJavaProject;

/**
 * Interface for receiving notification about JOANA nature assignment state
 * changes of projects in the workspace.
 *
 */
public interface IJoanaNatureChangeListener {
	/**
	 * Called when the JOANA nature state is toggled on a PDE project.
	 *
	 * @param project
	 *            the <code>IJavaProject</code> the JOANA nature was toggled
	 *            on.
	 */
	public void notifyJoanaNatureChanged(IJavaProject project);
}
