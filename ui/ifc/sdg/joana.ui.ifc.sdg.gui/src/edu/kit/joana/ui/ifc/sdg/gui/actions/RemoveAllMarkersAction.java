/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.actions;


import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import edu.kit.joana.ui.ifc.sdg.gui.marker.MarkerManager;


public class RemoveAllMarkersAction extends MarkerAction {

	protected boolean preconditions() {
		return true;
	}

	protected boolean concreteAction() throws CoreException {
		return true;
	}

    protected IWorkspaceRunnable createWorkspaceRunnable() {
        return new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
            	MarkerManager.singleton().removeAllMarkers(project);
            }
        };
    }

	protected String getErrorMessage() {
		return "Couldn't delete all NJSecMarkers";
	}
}
