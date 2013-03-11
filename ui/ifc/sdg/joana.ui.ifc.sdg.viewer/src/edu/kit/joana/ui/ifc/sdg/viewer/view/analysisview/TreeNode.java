/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview;

import org.eclipse.core.resources.IProject;

abstract class TreeNode implements Selection {

	/** Returns the parent element
	 */
	@SuppressWarnings("rawtypes")
	public abstract Parent getParent();

	/** Returns the name of the element.
	 */
	public abstract String getName();

	public abstract IProject getProject();

	/**
	 * Double-click action.
	 */
	public abstract void doubleClicked();

	/** Returns the name of the element.
	 */
	public String toString() {
		return getName();
	}
}
