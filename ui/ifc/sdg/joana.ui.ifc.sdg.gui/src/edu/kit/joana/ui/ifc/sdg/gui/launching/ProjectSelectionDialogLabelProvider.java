/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.launching;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.LabelProvider;

public class ProjectSelectionDialogLabelProvider extends LabelProvider {

	public String getText(Object element) {
		if (element instanceof IProject) {
			IProject ip = (IProject) element;
			return ip.getName();

		} else {
			return element.toString();
		}
	}
}
