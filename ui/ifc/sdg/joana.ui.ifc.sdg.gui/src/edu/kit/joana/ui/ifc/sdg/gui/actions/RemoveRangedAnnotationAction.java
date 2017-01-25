/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.actions;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;

import edu.kit.joana.ui.ifc.sdg.gui.marker.MarkerManager;
import edu.kit.joana.ui.ifc.sdg.gui.marker.NJSecMarkerConstants;


public class RemoveRangedAnnotationAction extends MarkerAction {
	private int offset = 0;
	private int length = 0;

	private IMarker[] allMarkers;
	private List<IMarker> remaining;
	private List<IMarker> delete;

	protected boolean preconditions() {
		return true;
	}

	protected boolean concreteAction() throws CoreException {
		allMarkers = resource.findMarkers(NJSecMarkerConstants.MARKER_TYPE_NJSEC, true, IResource.DEPTH_INFINITE);
		remaining = new LinkedList<IMarker>();
		delete = new LinkedList<IMarker>();

		for (int i = 0; i < allMarkers.length; i++) {
    		if ((allMarkers[i].getAttribute(IMarker.CHAR_START, -1) >= offset
    				&& allMarkers[i].getAttribute(IMarker.CHAR_END, -1) <= offset + length)
    				|| (allMarkers[i].getAttribute(IMarker.CHAR_START, -1) <= offset
    						&& allMarkers[i].getAttribute(IMarker.CHAR_END, -1) >= offset + length)) {

    			delete.add(allMarkers[i]);

    		} else {
    			remaining.add(allMarkers[i]);
    		}
    	}

		return delete.size() > 0;
	}

    protected IWorkspaceRunnable createWorkspaceRunnable() {
        return new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
            	MarkerManager.singleton().removeMarkers(project, delete, remaining);
            }
        };
    }

	protected String getErrorMessage() {
		return "Problem while finding NJSec Annotation Markers";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(ISelection selection) {
		//Decide which kind of selection it is
		if (selection != null && selection instanceof TextSelection) {
			TextSelection ims = (TextSelection) selection;
			offset = ims.getOffset();
			length = ims.getLength();
		}
	}
}
