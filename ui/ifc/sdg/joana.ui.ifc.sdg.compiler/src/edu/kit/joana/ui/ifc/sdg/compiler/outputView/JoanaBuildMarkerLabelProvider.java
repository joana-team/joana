/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.outputView;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * A JFace <code>ITableLabelProvider</code> that operates on
 * <code>IMarker</code> objects. Displays the name of the resource the marker
 * is attached to in the first column, and the full path to the resource in the
 * second.
 *
 */
public class JoanaBuildMarkerLabelProvider implements ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		IMarker marker = (IMarker) element;
		switch (columnIndex) {
		case 0:
			return marker.getResource().getName();
		case 1:
			return marker.getResource().getFullPath().toString();
		}
		return null;
	}

	public void addListener(ILabelProviderListener listener) {

	}

	public void dispose() {

	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {

	}

}
