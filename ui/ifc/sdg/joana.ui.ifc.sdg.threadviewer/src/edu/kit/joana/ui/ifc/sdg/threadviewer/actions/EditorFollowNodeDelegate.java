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
package edu.kit.joana.ui.ifc.sdg.threadviewer.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.*;

import edu.kit.joana.ui.ifc.sdg.threadviewer.controller.Controller;

public class EditorFollowNodeDelegate implements IEditorActionDelegate {

	private IEditorPart targetPart;

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		this.targetPart = targetEditor;
	}

	@Override
	public void run(IAction action) {
		Controller.getInstance().runEditorFollowNode(targetPart);
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) { }
}
