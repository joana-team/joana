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
package edu.kit.joana.ui.ifc.sdg.gui.contextmenu;


import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import edu.kit.joana.ui.ifc.sdg.gui.actions.RemoveRangedAnnotationAction;


public class RemoveAnnotationActionDelegate extends MarkerActionDelegate {

	public RemoveAnnotationActionDelegate() {
		concreteAction = new RemoveRangedAnnotationAction();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		concreteAction.selectionChanged(selection);
	}
}
