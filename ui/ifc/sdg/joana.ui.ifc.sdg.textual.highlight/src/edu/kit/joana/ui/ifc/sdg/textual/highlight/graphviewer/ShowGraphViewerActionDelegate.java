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
package edu.kit.joana.ui.ifc.sdg.textual.highlight.graphviewer;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.internal.PluginAction;

public class ShowGraphViewerActionDelegate extends ActionDelegate implements IObjectActionDelegate {

	@Override
	public void run(IAction action) {
		ShowGraphViewer.showGraphViewer(((IResource)((TreeSelection)((PluginAction) action).getSelection()).getFirstElement()).getLocation().toString());
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}


}
