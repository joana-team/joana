/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.latticeeditor.ui;


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import edu.kit.joana.ifc.sdg.lattice.LatticeProblemDescription;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.LatticeEditorPlugin;

public class LatticeErrorMarker {
	public static final String ID = LatticeEditorPlugin.PLUGIN_ID + ".joanaLatticeValidationError";

	public static <ElementType> void mark(IResource resource, LatticeProblemDescription<ElementType> problem) throws CoreException {
		IMarker marker = resource.createMarker(ID);
		marker.setAttribute(IMarker.MESSAGE, problem.message);
		if (problem.involvedNodes != null)
			marker.setAttribute(IMarker.LOCATION, problem.involvedNodes.toString());
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
	}

	public static void unmark(IResource resource) throws CoreException {
		resource.deleteMarkers(ID, true, IResource.DEPTH_ZERO);
	}

}
