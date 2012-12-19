/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.threadviewer.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;

import edu.kit.joana.ui.ifc.sdg.threadviewer.controller.Controller;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.Activator;


public class ViewContextMenu_FollowThreadAction extends Action {
	public ViewContextMenu_FollowThreadAction() {
		super();
		setText("Follow Thread");
		setToolTipText("Follows the selected thread in code.");


		ImageRegistry ir = Activator.getDefault().getImageRegistry();
		ImageDescriptor desc = ir.getDescriptor(Activator.IMAGE_VIEW_CONTEXTMENU_FOLLOW_THREAD);
		setImageDescriptor(desc);
	}

	@Override
	public void run() {
		Controller.getInstance().runViewFollowThread();
	}
}
