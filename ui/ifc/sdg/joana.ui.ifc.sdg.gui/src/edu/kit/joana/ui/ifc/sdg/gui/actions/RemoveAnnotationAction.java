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

import java.util.Collection;


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import edu.kit.joana.ui.ifc.sdg.gui.marker.MarkerManager;
import edu.kit.joana.ui.ifc.sdg.gui.views.AnnotationView;


public class RemoveAnnotationAction extends MarkerAction {
	private AnnotationView view;
	private Collection<IMarker> remaining;
	private Collection<IMarker> delete;

	public RemoveAnnotationAction(AnnotationView view) {
		this.view = view;
	}

	protected boolean preconditions() {
		return true;
	}

	protected boolean concreteAction() throws CoreException {
		if (view.getSelectedMarkers().size() == 0) {
			return false;

		} else {
			delete = view.getSelectedMarkers();
			remaining = view.getInversedSelection();
			return true;
		}
	}

	protected IWorkspaceRunnable createWorkspaceRunnable() {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
            	MarkerManager.singleton().removeMarkers(project, delete, remaining);
			}
		};
	}

	@Override
	protected String getErrorMessage() {
		return "Problem while deleting NJSec Annotation Marker";
	}
}
