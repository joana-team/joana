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
package edu.kit.joana.ui.ifc.sdg.compiler.outputView;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * A JFace <code>IStructuredContentProvider</code> returning a list of all
 * markers of a specific type in the workspace. A <code>IWorkspace</code>
 * object is expected as input.
 *
 */
public class MarkerContentProvider implements IStructuredContentProvider {

	private boolean includeSubtypes;

	private String markerType;

	public Object[] getElements(Object inputElement) {
		try {
			return ((IWorkspace) inputElement).getRoot().findMarkers(markerType, includeSubtypes, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void dispose() {

	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

	}

	/**
	 * Constructor
	 *
	 * @param markerType
	 *            the ID of the marker to look for.
	 * @param includeSubtypes
	 *            indicates if subtypes of the marker type identified by
	 *            <code>markerType</code> are to be included as well.
	 */
	public MarkerContentProvider(String markerType, boolean includeSubtypes) {
		super();
		this.markerType = markerType;
		this.includeSubtypes = includeSubtypes;
	}

}
