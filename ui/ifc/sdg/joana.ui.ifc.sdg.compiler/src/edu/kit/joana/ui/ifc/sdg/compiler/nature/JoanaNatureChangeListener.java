/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.nature;

import org.eclipse.jdt.core.IJavaProject;

public interface JoanaNatureChangeListener {
	public void notifyJoanaNatureChanged(IJavaProject project);
}
