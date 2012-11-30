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
package edu.kit.joana.ui.ifc.sdg.gui.actions;


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorInput;

import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;

public abstract class MarkerAction extends Action {
    protected IProject project;
	protected IResource resource;

	protected abstract boolean preconditions();

	protected abstract boolean concreteAction() throws CoreException;

	protected abstract IWorkspaceRunnable createWorkspaceRunnable();

	protected abstract String getErrorMessage();

    /**
     * @see IActionDelegate#run(IAction)
     */
    public void run() {
    	// boiler plate code
        IEditorInput iei = NJSecPlugin.singleton().getActivePage().getActiveEditor().getEditorInput();
        resource = (IResource) ((IAdaptable) iei).getAdapter(IResource.class);
        project = resource.getProject();

    	// test customizable preconditions
    	if (!preconditions()) return;

        try {
	        // commit the concrete action
	        if (!concreteAction()) return;

	        // update the workspace if the action demands so
            IWorkspaceRunnable body = createWorkspaceRunnable();
            resource.getWorkspace().run(body, null);

        } catch (CoreException e) {
            NJSecPlugin.singleton().showError(getErrorMessage(), null, e);
        }
    }

    /** This method is a stub doing nothing.
     *
	 * @param selection
	 */
    public void selectionChanged(ISelection selection) { }

    public IMarker getMarker() {
    	return null;
    }
}

